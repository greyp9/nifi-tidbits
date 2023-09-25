package io.github.greyp9.nifi.tls.test;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.security.util.KeystoreType;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.security.util.StandardTlsConfiguration;
import org.apache.nifi.security.util.TemporaryKeyStoreBuilder;
import org.apache.nifi.security.util.TlsConfiguration;
import org.apache.nifi.security.util.TlsException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TlsTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * If server uses normal keystore / truststore, but client uses a temporary keystore, we expect failure to
     * establish trust relationship.
     */
    @Test
    void testConnectFailKeyMismatch() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                "nifi1.pkcs12", "1234", KeystoreType.PKCS12,
                "trust.pkcs12", "123456", KeystoreType.PKCS12);
        final ServerRunnable serverRunnable = new ServerRunnable(tlsConfigurationServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new TemporaryKeyStoreBuilder().trustStoreType("JKS").build();

        final ClientRunnable clientRunnable = new ClientRunnable(tlsConfigurationClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        assertTrue(clientRunnable.getException() instanceof SSLHandshakeException);
        logger.trace(clientRunnable.getException().getMessage());
        assertTrue(clientRunnable.getException().getMessage().contains("PKIX path building failed"));
    }

    /**
     * If server uses normal keystore / truststore, but client has truststore only, we expect SSL mutual authentication
     * to fail.
     */
    @Test
    void testConnectFailNeedClientAuth() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                "nifi1.pkcs12", "1234", KeystoreType.PKCS12,
                "trust.pkcs12", "123456", KeystoreType.PKCS12);
        final ServerRunnable serverRunnable = new ServerRunnable(tlsConfigurationServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new StandardTlsConfiguration(
                null, null, null,
                "nifi1.pkcs12", "1234", KeystoreType.PKCS12);
        final ClientRunnable clientRunnable = new ClientRunnable(tlsConfigurationClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        assertTrue(clientRunnable.getException() instanceof SSLException);
        logger.trace(clientRunnable.getException().getMessage());
        assertTrue(clientRunnable.getException().getMessage().contains("readHandshakeRecord"));
    }

    /**
     * SSL mutual authentication test case, where truststore is of type PKCS12 (created using keytool).
     */
    @Test
    void testConnectSuccessPKCSTrust() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                "nifi1.pkcs12", "1234", KeystoreType.PKCS12,
                "trust.pkcs12", "123456", KeystoreType.PKCS12);
        final ServerRunnable serverRunnable = new ServerRunnable(tlsConfigurationServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new StandardTlsConfiguration(
                "nifi2.pkcs12", "1234", KeystoreType.PKCS12,
                "trust.pkcs12", "123456", KeystoreType.PKCS12);
        final ClientRunnable clientRunnable = new ClientRunnable(tlsConfigurationClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(300000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        assertNull(serverRunnable.getException());
        assertNull(clientRunnable.getException());
    }

    /**
     * SSL mutual authentication test case, where truststore is of type JKS (created using keytool).
     */
    @Test
    void testConnectSuccessJKSTrust() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                "nifi1.pkcs12", "1234", KeystoreType.PKCS12,
                "trust.jks", "123456", KeystoreType.JKS);
        final ServerRunnable serverRunnable = new ServerRunnable(tlsConfigurationServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new StandardTlsConfiguration(
                "nifi2.pkcs12", "1234", KeystoreType.PKCS12,
                "trust.jks", "123456", KeystoreType.JKS);
        final ClientRunnable clientRunnable = new ClientRunnable(tlsConfigurationClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        assertNull(serverRunnable.getException());
        assertNull(clientRunnable.getException());
    }

    private static class ServerRunnable implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final TlsConfiguration tlsConfiguration;
        private int port;
        private Exception exception;

        public ServerRunnable(final TlsConfiguration tlsConfiguration) {
            this.tlsConfiguration = tlsConfiguration;
        }

        public int getPort() {
            return port;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public void run() {
            try {
                final SSLContext sslContext = SslContextFactory.createSslContext(tlsConfiguration);
                try (final ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(0, 0)) {
                    port = serverSocket.getLocalPort();
                    logger.trace("SERVER RUNNING ON PORT {}", port);
                    try (final Socket socket = serverSocket.accept()) {
                        logger.trace("GOT A CLIENT CONNECTION: {}", socket);

                        if (socket instanceof SSLSocket) {
                            final SSLSocket sslSocket = (SSLSocket) socket;
                            sslSocket.setNeedClientAuth(true);
                        }

                        final InputStream is = new BufferedInputStream(socket.getInputStream());
                        final OutputStream os = new BufferedOutputStream(socket.getOutputStream());

                        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        // loop until we get some data
                        while (bos.size() == 0) {
                            final int i = is.read();
                            if (i > 0) {
                                bos.write(i);
                            }
                            while (is.available() > 0) {
                                bos.write(IOUtils.toByteArray(is, is.available()));
                            }
                            Thread.sleep(200L);
                        }
                        logger.info(new String(bos.toByteArray(), StandardCharsets.UTF_8));

                        os.write("hello from server".getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                }
            } catch (TlsException | IOException | InterruptedException e) {
                exception = e;
                logger.error("{} - {}", e.getClass(), e.getMessage());
            }
        }
    }

    private static class ClientRunnable implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final TlsConfiguration tlsConfiguration;
        private final int port;
        private Exception exception;

        public ClientRunnable(final TlsConfiguration tlsConfiguration, final int port) {
            this.tlsConfiguration = tlsConfiguration;
            this.port = port;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public void run() {
            try {
                logger.trace("CLIENT RUNNING");
                final SSLContext sslContext = SslContextFactory.createSslContext(tlsConfiguration);
                try (final Socket socket = sslContext.getSocketFactory().createSocket("localhost", port)) {
                    logger.trace("GOT A SERVER CONNECTION: {}", socket);

                    final InputStream is = new BufferedInputStream(socket.getInputStream());
                    final OutputStream os = new BufferedOutputStream(socket.getOutputStream());

                    final byte[] payloadSend = "hello from client".getBytes(StandardCharsets.UTF_8);
                    os.write(payloadSend, 0, payloadSend.length);
                    os.flush();

                    final byte[] payloadReceive = IOUtils.toByteArray(is);
                    logger.info(new String(payloadReceive, StandardCharsets.UTF_8));
                }
            } catch (TlsException | IOException e) {
                exception = e;
                logger.error("{} - {}", e.getClass(), e.getMessage());
            }
        }
    }
}
