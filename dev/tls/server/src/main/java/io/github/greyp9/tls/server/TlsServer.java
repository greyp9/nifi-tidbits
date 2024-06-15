package io.github.greyp9.tls.server;

import io.github.greyp9.arwo.core.date.DurationU;
import io.github.greyp9.arwo.core.input.runnable.InputStreamRunnable;
import io.github.greyp9.arwo.core.value.Value;
import io.github.greyp9.arwo.core.vm.thread.ThreadU;
import org.apache.nifi.security.util.KeystoreType;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.security.util.StandardTlsConfiguration;
import org.apache.nifi.security.util.TlsConfiguration;
import org.apache.nifi.security.util.TlsException;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;

public class TlsServer {
    private final File tlsDir;

    public TlsServer(final File tlsDir) {
        this.tlsDir = tlsDir;
    }

    public void run() throws TlsException {
        final String keystore = new File(tlsDir, "ks.p12").getAbsolutePath();
        final String truststore = new File(tlsDir, "ts.jks").getAbsolutePath();
        final String password = "123456";
        final TlsConfiguration tlsConfiguration = new StandardTlsConfiguration(
                keystore, password, KeystoreType.PKCS12,
                truststore, password, KeystoreType.JKS);
        final SSLContext sslContextServer = SslContextFactory.createSslContext(tlsConfiguration);
        final long idleInterval = DurationU.Const.ONE_SECOND_MILLIS / 2;
        final AtomicReference<String> signal = new AtomicReference<>();
        final ServerRunnable serverRunnable = new ServerRunnable(sslContextServer, idleInterval, signal);
        ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());
        executorService.execute(new InputStreamRunnable(System.in, signal, idleInterval));
        executorService.execute(serverRunnable);
        final Runnable runnableTerminal = () -> signal.set("INTERRUPTED");
        while (signal.get() == null) {
            Value.doIf(ThreadU.sleepMillis(idleInterval), runnableTerminal);
        }
        executorService.shutdown();
    }

    public static void main(final String[] args) {
        final String tlsDir = System.getProperty("tls.dir");
        Value.require(Value.isData(tlsDir), () -> new IllegalArgumentException("specify '-Dtls.dir'"));
        final File folder = new File(System.getProperty("tls.dir"));
        Value.require(folder.exists(), () -> new IllegalArgumentException("tlsDir must refer to an existing folder"));

        try {
            new TlsServer(folder).run();
        } catch (TlsException e) {
            throw new RuntimeException(e);
        }
    }
}
