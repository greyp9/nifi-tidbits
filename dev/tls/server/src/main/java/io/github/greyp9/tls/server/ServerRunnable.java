package io.github.greyp9.tls.server;

import io.github.greyp9.arwo.core.charset.UTF8Codec;
import io.github.greyp9.arwo.core.vm.thread.ThreadU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class ServerRunnable implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SSLContext sslContext;
    private final long soTimeout;
    private final AtomicReference<String> reference;

    private int port;
    private Exception exception;

    public ServerRunnable(final SSLContext sslContext, final long soTimeout, final AtomicReference<String> reference) {
        this.sslContext = sslContext;
        this.soTimeout = soTimeout;
        this.reference = reference;
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
                serverSocket.setSoTimeout((int) soTimeout);
                port = serverSocket.getLocalPort();
                logger.info("SERVER RUNNING ON PORT {}", port);
                while (reference.get() == null) {
                    accept(serverSocket);
                }
            }

        } catch (IOException e) {
            exception = e;
            logger.error(e.getMessage(), e);
        }
    }

    private void accept(final ServerSocket serverSocket) throws IOException {
        try (final Socket socket = serverSocket.accept()) {
            logger.info("GOT A CLIENT CONNECTION: {}", socket);

            if (socket instanceof SSLSocket) {
                final SSLSocket sslSocket = (SSLSocket) socket;
                sslSocket.setNeedClientAuth(true);
            }

            final InputStream is = new BufferedInputStream(socket.getInputStream());
            final OutputStream os = new BufferedOutputStream(socket.getOutputStream());

            // receive message
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
            final String payloadIn = UTF8Codec.toString(bos.toByteArray());
            logger.info(String.format("SERVER RECEIVED: %s", payloadIn));
            // send service response
            final String payloadOut = String.format("server ack - %s", payloadIn);
            final byte[] payloadOutBytes = UTF8Codec.toBytes(payloadOut);
            os.write(payloadOutBytes);
            os.flush();
            logger.info(String.format("SERVER SENT: %s", payloadOut));
        } catch (SocketTimeoutException e) {
            logger.trace(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
