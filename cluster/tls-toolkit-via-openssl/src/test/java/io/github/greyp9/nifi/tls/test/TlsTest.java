package io.github.greyp9.nifi.tls.test;

import org.apache.nifi.security.util.KeystoreType;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.security.util.StandardTlsConfiguration;
import org.apache.nifi.security.util.TemporaryKeyStoreBuilder;
import org.apache.nifi.security.util.TlsConfiguration;
import org.apache.nifi.security.util.TlsException;
import org.junit.jupiter.api.BeforeEach;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test interaction of NiFi keystore classes with OpenSSL-generated keystores.
 * <p>
 * Preconditions:
 * - setup trust.p12, trust.jks, nifi1.p12, nifi2.p12 in project root (as described in README.md)
 */
public class TlsTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final File FOLDER_ROOT = new File(".");
    private static final String TRUSTSTORE_JKS = new File(FOLDER_ROOT, "trust.jks").getAbsolutePath();
    private static final String TRUSTSTORE_PKCS12 = new File(FOLDER_ROOT, "trust.p12").getAbsolutePath();
    private static final String KEYSTORE_NIFI1 = new File(FOLDER_ROOT, "nifi1.p12").getAbsolutePath();
    private static final String KEYSTORE_NIFI2 = new File(FOLDER_ROOT, "nifi2.p12").getAbsolutePath();
    private static final String PASSWORD = "123456123456";

    @BeforeEach
    void setUp() {
        logger.trace(new File(".").getAbsolutePath());
    }

    /**
     * If server uses normal keystore / truststore, but client uses a temporary keystore, we expect failure to
     * establish trust relationship.
     */
    @Test
    void testConnectFailKeyMismatch() throws InterruptedException, TlsException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                KEYSTORE_NIFI1, PASSWORD, KeystoreType.PKCS12,
                TRUSTSTORE_PKCS12, PASSWORD, KeystoreType.PKCS12);
        final SSLContext sslContextServer = SslContextFactory.createSslContext(tlsConfigurationServer);
        final ServerRunnable serverRunnable = new ServerRunnable(sslContextServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new TemporaryKeyStoreBuilder().trustStoreType("JKS").build();
        final SSLContext sslContextClient = SslContextFactory.createSslContext(tlsConfigurationClient);
        final ClientRunnable clientRunnable = new ClientRunnable(sslContextClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        assertInstanceOf(SSLHandshakeException.class, clientRunnable.getException());
        logger.trace(clientRunnable.getException().getMessage());
        assertTrue(clientRunnable.getException().getMessage().contains("PKIX path building failed"));
    }

    /**
     * If server uses normal keystore / truststore, but client has truststore only, we expect SSL mutual authentication
     * to fail.
     */
    @Test
    void testConnectFailNeedClientAuth() throws InterruptedException, TlsException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                KEYSTORE_NIFI1, PASSWORD, KeystoreType.PKCS12,
                TRUSTSTORE_PKCS12, PASSWORD, KeystoreType.PKCS12);
        final SSLContext sslContextServer = SslContextFactory.createSslContext(tlsConfigurationServer);
        final ServerRunnable serverRunnable = new ServerRunnable(sslContextServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new StandardTlsConfiguration(
                null, null, null,
                KEYSTORE_NIFI1, PASSWORD, KeystoreType.PKCS12);
        final SSLContext sslContextClient = SslContextFactory.createSslContext(tlsConfigurationClient);
        final ClientRunnable clientRunnable = new ClientRunnable(sslContextClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        assertInstanceOf(SSLException.class, clientRunnable.getException());
        logger.trace(clientRunnable.getException().getMessage());
        assertTrue(clientRunnable.getException().getMessage().contains("readHandshakeRecord"));
    }

    /**
     * SSL mutual authentication test case, where truststore is of type PKCS12 (created using keytool).
     */
    @Test
    void testConnectSuccessPKCSTrust() throws InterruptedException, TlsException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                KEYSTORE_NIFI1, PASSWORD, KeystoreType.PKCS12,
                TRUSTSTORE_PKCS12, PASSWORD, KeystoreType.PKCS12);
        final SSLContext sslContextServer = SslContextFactory.createSslContext(tlsConfigurationServer);
        final ServerRunnable serverRunnable = new ServerRunnable(sslContextServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new StandardTlsConfiguration(
                KEYSTORE_NIFI2, PASSWORD, KeystoreType.PKCS12,
                TRUSTSTORE_PKCS12, PASSWORD, KeystoreType.PKCS12);
        final SSLContext sslContextClient = SslContextFactory.createSslContext(tlsConfigurationClient);
        final ClientRunnable clientRunnable = new ClientRunnable(sslContextClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        if (serverRunnable.getException() != null) {
            serverRunnable.getException().printStackTrace(System.err);
            logger.error("SERVER RUNNABLE", serverRunnable.getException());
        }

        assertNull(serverRunnable.getException());
        assertNull(clientRunnable.getException());
    }

    /**
     * SSL mutual authentication test case, where truststore is of type JKS (created using keytool).
     */
    @Test
    void testConnectSuccessJKSTrust() throws InterruptedException, TlsException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, Executors.defaultThreadFactory());

        final TlsConfiguration tlsConfigurationServer = new StandardTlsConfiguration(
                KEYSTORE_NIFI1, PASSWORD, KeystoreType.PKCS12,
                TRUSTSTORE_JKS, PASSWORD, KeystoreType.JKS);
        final SSLContext sslContextServer = SslContextFactory.createSslContext(tlsConfigurationServer);
        final ServerRunnable serverRunnable = new ServerRunnable(sslContextServer);
        executorService.execute(serverRunnable);

        Thread.sleep(1000L);  // allow server to start
        final int port = serverRunnable.getPort();

        final TlsConfiguration tlsConfigurationClient = new StandardTlsConfiguration(
                KEYSTORE_NIFI2, PASSWORD, KeystoreType.PKCS12,
                TRUSTSTORE_JKS, PASSWORD, KeystoreType.JKS);
        final SSLContext sslContextClient = SslContextFactory.createSslContext(tlsConfigurationClient);
        final ClientRunnable clientRunnable = new ClientRunnable(sslContextClient, port);
        executorService.execute(clientRunnable);

        executorService.shutdown();
        final boolean terminated = executorService.awaitTermination(3000L, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
        if (serverRunnable.getException() != null) {
            serverRunnable.getException().printStackTrace(System.err);
            logger.error("SERVER RUNNABLE", serverRunnable.getException());
        }
        assertNull(serverRunnable.getException());
        assertNull(clientRunnable.getException());
    }

    private static class ServerRunnable implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final SSLContext sslContext;
        private int port;
        private Exception exception;

        public ServerRunnable(final SSLContext sslContext) {
            this.sslContext = sslContext;
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
                                bos.write(is.read());
                            }
                            Thread.sleep(200L);
                        }
                        logger.info(bos.toString(StandardCharsets.UTF_8.name()));

                        os.write("hello from server".getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                }
            } catch (IOException | InterruptedException e) {
                exception = e;
                logger.error("{} - {}", e.getClass(), e.getMessage());
            }
        }
    }

    private static class ClientRunnable implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final SSLContext sslContext;
        private final int port;
        private Exception exception;

        public ClientRunnable(final SSLContext sslContext, final int port) {
            this.sslContext = sslContext;
            this.port = port;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public void run() {
            try {
                logger.trace("CLIENT RUNNING");
                try (final Socket socket = sslContext.getSocketFactory().createSocket("localhost", port)) {
                    logger.trace("GOT A SERVER CONNECTION: {}", socket);

                    final InputStream is = new BufferedInputStream(socket.getInputStream());
                    final OutputStream os = new BufferedOutputStream(socket.getOutputStream());

                    final byte[] payloadSend = "hello from client".getBytes(StandardCharsets.UTF_8);
                    os.write(payloadSend, 0, payloadSend.length);
                    os.flush();

                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while (is.available() > 0) {
                        bos.write(is.read());
                    }
                    logger.info(bos.toString(StandardCharsets.UTF_8.name()));
                }
            } catch (IOException e) {
                exception = e;
                logger.error("{} - {}", e.getClass(), e.getMessage());
            }
        }
    }
}
