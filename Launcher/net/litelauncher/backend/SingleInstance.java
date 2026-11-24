package net.litelauncher.backend;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/** The open channel owns the instance; the loopback socket only requests activation. */
public final class SingleInstance implements AutoCloseable {
    private static final int ACTIVATE = 0x4c4c0101;
    private final FileChannel channel;
    private final ServerSocket server;

    private SingleInstance(FileChannel channel, Runnable activate) throws IOException {
        this.channel = channel;
        server = new ServerSocket();
        try {
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            channel.truncate(0);
            channel.position(1); // Byte zero is locked; Windows readers must not touch that region.
            ByteBuffer port = StandardCharsets.US_ASCII.encode(Integer.toString(server.getLocalPort()));
            while (port.hasRemaining()) channel.write(port);
        } catch (IOException exception) {
            server.close();
            throw exception;
        }
        Thread.ofVirtual().name("launcher-activation").start(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(500);
                    if (new DataInputStream(socket.getInputStream()).readInt() != ACTIVATE) continue;
                    activate.run();
                    new DataOutputStream(socket.getOutputStream()).writeInt(ACTIVATE);
                } catch (IOException ignored) {
                }
            }
        });
    }

    /** Returns null after notifying the existing instance; never initializes a second launcher. */
    public static SingleInstance open(Path directory, Runnable activate) throws IOException {
        Files.createDirectories(directory);
        FileChannel channel = FileChannel.open(directory.resolve("launcher.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            do {
                try {
                    if (channel.tryLock(0, 1, false) != null) return new SingleInstance(channel, activate);
                } catch (OverlappingFileLockException ignored) {
                }
                if (signal(channel)) {
                    channel.close();
                    return null;
                }
                Thread.sleep(25);
            } while (System.nanoTime() < deadline);
            throw new IOException("The running LiteLauncher is not responding.");
        } catch (Exception exception) {
            channel.close();
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IOException("Unable to open LiteLauncher.", exception);
        }
    }

    private static boolean signal(FileChannel channel) {
        try {
            ByteBuffer data = ByteBuffer.allocate(16);
            if (channel.read(data, 1) <= 0) return false;
            data.flip();
            int port = Integer.parseInt(StandardCharsets.US_ASCII.decode(data).toString());
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 150);
                socket.setSoTimeout(150);
                new DataOutputStream(socket.getOutputStream()).writeInt(ACTIVATE);
                return new DataInputStream(socket.getInputStream()).readInt() == ACTIVATE;
            }
        } catch (IOException | IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public void close() {
        try { server.close(); } catch (IOException ignored) { }
        try { channel.close(); } catch (IOException ignored) { }
    }
}
