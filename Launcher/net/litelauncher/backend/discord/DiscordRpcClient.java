package net.litelauncher.backend.discord;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.platform.BrowserLinks;
import net.litelauncher.backend.platform.OSUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class DiscordRpcClient implements Closeable {

    private static final String CLIENT_ID = "1523941265786277978";
    private static final String LARGE_IMAGE = "litelauncher";

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int PIPE_COUNT = 10;

    private Transport transport;

    void setActivity(long pid, String details) {
        JsonObject activity = new JsonObject();
        activity.put("details", DiscordRpcService.safe(details));
        activity.put("assets", assets());
        send(pid, activity);
    }

    void clearActivity(long pid) {
        send(pid, null);
    }

    private void send(long pid, JsonObject activity) {
        try {
            if (!connect()) return;

            JsonObject args = new JsonObject();
            args.put("pid", pid);
            args.put("activity", activity);

            JsonObject payload = new JsonObject();
            payload.put("cmd", "SET_ACTIVITY");
            payload.put("args", args);
            payload.put("nonce", UUID.randomUUID().toString());

            transport.write(frame(OP_FRAME, payload));
        } catch (Exception _) {
            closeQuietly();
        }
    }

    private boolean connect() {
        if (transport != null) return true;

        for (String endpoint : endpoints()) {
            try {
                Transport opened = open(endpoint);
                opened.write(frame(OP_HANDSHAKE, handshake()));
                transport = opened;
                return true;
            } catch (IOException _) {
            }
        }
        return false;
    }

    private List<String> endpoints() {
        List<String> endpoints = new ArrayList<>();
        for (int index = 0; index < PIPE_COUNT; index++) {
            String name = "discord-ipc-" + index;
            if (OSUtils.os().windows()) {
                endpoints.add("\\\\?\\pipe\\" + name);
                endpoints.add("\\\\.\\pipe\\" + name);
            } else {
                for (Path directory : unixDirectories()) {
                    Path socket = directory.resolve(name);
                    if (Files.exists(socket)) endpoints.add(socket.toString());
                }
            }
        }
        return endpoints;
    }

    private List<Path> unixDirectories() {
        Set<Path> directories = new LinkedHashSet<>();
        addDirectory(directories, System.getenv("XDG_RUNTIME_DIR"));
        addDirectory(directories, System.getenv("TMPDIR"));
        addDirectory(directories, System.getenv("TMP"));
        addDirectory(directories, System.getenv("TEMP"));
        directories.add(Path.of("/tmp"));
        return List.copyOf(directories);
    }

    private void addDirectory(Set<Path> directories, String value) {
        if (value != null && !value.isBlank()) directories.add(Path.of(value));
    }

    private Transport open(String endpoint) throws IOException {
        if (OSUtils.os().windows()) return new WindowsPipe(new RandomAccessFile(endpoint, "rw"));

        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(UnixDomainSocketAddress.of(Path.of(endpoint)));
        return new UnixSocket(channel);
    }

    private JsonObject handshake() {
        JsonObject payload = new JsonObject();
        payload.put("v", 1);
        payload.put("client_id", CLIENT_ID);
        return payload;
    }

    private JsonObject assets() {
        JsonObject assets = new JsonObject();
        assets.put("large_image", LARGE_IMAGE);
        assets.put("large_text", DiscordRpcService.LAUNCHER_ACTIVITY);
        assets.put("large_url", BrowserLinks.WEBSITE);
        return assets;
    }

    private byte[] frame(int opcode, JsonObject payload) {
        byte[] json = JsonWriter.string().value(payload).done().getBytes(StandardCharsets.UTF_8);
        ByteBuffer frame = ByteBuffer.allocate(8 + json.length).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(opcode);
        frame.putInt(json.length);
        frame.put(json);
        return frame.array();
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        Transport current = transport;
        transport = null;
        if (current == null) return;
        try {
            current.close();
        } catch (IOException _) {
        }
    }

    private interface Transport extends Closeable {
        void write(byte[] bytes) throws IOException;
    }

    private record WindowsPipe(RandomAccessFile pipe) implements Transport {
        @Override
        public void write(byte[] bytes) throws IOException {
            pipe.write(bytes);
        }

        @Override
        public void close() throws IOException {
            pipe.close();
        }
    }

    private record UnixSocket(SocketChannel channel) implements Transport {
        @Override
        public void write(byte[] bytes) throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}
