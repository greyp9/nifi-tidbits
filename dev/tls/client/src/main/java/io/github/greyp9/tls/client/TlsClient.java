package io.github.greyp9.tls.client;

import io.github.greyp9.arwo.core.charset.UTF8Codec;
import io.github.greyp9.arwo.core.date.DurationU;
import io.github.greyp9.arwo.core.input.runnable.InputStreamRunnable;
import io.github.greyp9.arwo.core.lang.NumberU;
import io.github.greyp9.arwo.core.tls.context.TLSContext;
import io.github.greyp9.arwo.core.tls.context.TLSContextFactory;
import io.github.greyp9.arwo.core.tls.manage.TLSKeyManager;
import io.github.greyp9.arwo.core.tls.manage.TLSTrustManager;
import io.github.greyp9.arwo.core.value.Value;
import io.github.greyp9.arwo.core.vm.thread.ThreadU;
import org.apache.nifi.security.util.KeystoreType;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.security.util.StandardTlsConfiguration;
import org.apache.nifi.security.util.TlsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;

public class TlsClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final File tlsDir;
    private final int port;

    public TlsClient(final File tlsDir, final int port) {
        this.tlsDir = tlsDir;
        this.port = port;
    }

    public void run() throws GeneralSecurityException, IOException {
        final String keystore = new File(tlsDir, "ks.p12").getAbsolutePath();
        final String truststore = new File(tlsDir, "ts.jks").getAbsolutePath();
        final String password = "123456";
        //final SSLContext sslContextClient = getSSLContextNiFi(keystore, truststore);
        final SSLContext sslContextClient = getSSLContextArwo(keystore, truststore, password);
        logger.info(sslContextClient.toString());

        final long idleInterval = (DurationU.Const.ONE_SECOND_MILLIS / 2);
        final AtomicReference<String> signal = new AtomicReference<>();
        final AtomicReference<String> referenceText = new AtomicReference<>();
        ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());
        executorService.execute(new InputStreamRunnable(System.in, referenceText, idleInterval));
        final Runnable runnableTerminal = () -> signal.set("INTERRUPTED");

        while (Value.isEmpty(signal.get())) {
            Value.doIf(ThreadU.sleepMillis(idleInterval), runnableTerminal);
            final String text = referenceText.getAndSet(null);
            if (Value.isData(text)) {
                if (text.contains("q")) {
                    runnableTerminal.run();
                } else {
                    logger.info(text);
                    sendMessage(sslContextClient, text);
                    executorService.execute(new InputStreamRunnable(System.in, referenceText, idleInterval));
                }
            }
        }
        logger.info("SIGNAL=[{}]", signal);
        executorService.shutdown();
    }

    private SSLContext getSSLContextNiFi(final String keystore, final String truststore, final String password)
            throws GeneralSecurityException {
        final TlsConfiguration tlsConfiguration = new StandardTlsConfiguration(
                keystore, password, KeystoreType.PKCS12,
                truststore, password, KeystoreType.JKS);
        return SslContextFactory.createSslContext(tlsConfiguration);
    }

    private SSLContext getSSLContextArwo(final String keystore, final String truststore, final String password)
            throws GeneralSecurityException, IOException {
        final TLSContextFactory tlsContextFactory = new TLSContextFactory();
        final TLSKeyManager keyManager = tlsContextFactory.getKeyManager("PKCS12", keystore, password.toCharArray());
        final TLSTrustManager trustManager = tlsContextFactory.getTrustManager("JKS", truststore, password.toCharArray());
        final TLSContext tlsContext = new TLSContext(keyManager, trustManager, "TLSv1.2");
        //final TLSContext tlsContext = new TLSContext(null, trustManager, "TLSv1.2");
        return tlsContext.getContext();
    }

    private void sendMessage(final SSLContext sslContext, final String message) {
        try (final Socket socket = sslContext.getSocketFactory().createSocket("localhost", port)) {
            logger.info("GOT A SERVER CONNECTION: {}", socket);
            // send message
            final String payload = String.format("hello from client - message=[%s]", message);
            final byte[] payloadBytes = UTF8Codec.toBytes(payload);
            final InputStream is = new BufferedInputStream(socket.getInputStream());
            final OutputStream os = new BufferedOutputStream(socket.getOutputStream());
            os.write(payloadBytes, 0, payloadBytes.length);
            os.flush();
            logger.info(String.format("CLIENT SENT: %s", payload));
            // receive service response
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bos.size() == 0) {
                final int i = is.read();
                if (i > 0) {
                    bos.write(i);
                }
                while (is.available() > 0) {
                    bos.write(is.read());
                }
                ThreadU.sleepMillis(20L);
            }
            logger.info(String.format("CLIENT RECEIVED: %s", UTF8Codec.toString(bos.toByteArray())));
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main(final String[] args) {
        final String tlsDir = System.getProperty("tls.dir");
        Value.require(Value.isData(tlsDir), () -> new IllegalArgumentException("specify '-Dtls.dir'"));
        final File folder = new File(System.getProperty("tls.dir"));
        Value.require(folder.exists(), () -> new IllegalArgumentException("tlsDir must refer to an existing folder"));

        Value.require((args.length >= 1), () -> new IllegalArgumentException("usage: TlsClient [port]"));
        final int port = NumberU.toInt(args[0], 0);
        Value.require((port > 0), () -> new IllegalArgumentException("port should be number"));

        try {
            new TlsClient(folder, port).run();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
