package net.litelauncher.backend.modules.launch;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.modules.download.DownloadService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class ClientJarPatcher {

    private static final long RELEASE_BORDER = Instant.parse("2011-11-17T22:00:00Z").toEpochMilli();
    private static final int METADATA_FORMAT = 1;
    private static final int PATCHER_VERSION = 3;
    private static final String PATCHED_SUFFIX = "-litelauncher-patched";
    private static final String OLD_JOIN = "http://www.minecraft.net/game/joinserver.jsp";
    private static final String NEW_JOIN = "http://session.minecraft.net/game/joinserver.jsp";

    boolean needsPatch(ResolvedVersion version) {
        if (version == null) return false;
        if (version.clientReleaseTime() > 0) return version.clientReleaseTime() < RELEASE_BORDER;
        return "old_alpha".equals(version.clientType()) || "old_beta".equals(version.clientType());
    }

    Path ensurePatched(Path originalJar, String expectedOriginalSha1) throws GameLaunchException {
        if (originalJar == null || !Files.isRegularFile(originalJar)) return originalJar;

        Path patchedJar = patchedJar(originalJar);
        Path metadata = patchedMetadata(originalJar);
        Path tempJar = patchedJar.resolveSibling(patchedJar.getFileName() + ".tmp");
        try {
            if (isValidCached(originalJar, patchedJar, metadata, expectedOriginalSha1)) {
                LauncherLog.info("Using cached patched client jar: " + patchedJar);
                return patchedJar;
            }

            Files.createDirectories(patchedJar.getParent());
            Files.deleteIfExists(tempJar);
            PatchSummary summary = patchJar(originalJar, tempJar);
            if (summary.replacements() <= 0) {
                deleteQuietly(tempJar);
                deleteQuietly(patchedJar);
                deleteQuietly(metadata);
                LauncherLog.info("Client jar does not contain legacy auth URL, using original: " + originalJar);
                return originalJar;
            }

            String originalSha1 = originalSha1(originalJar, expectedOriginalSha1);
            long originalSize = Files.size(originalJar);
            String patchedSha1 = DownloadService.sha1(tempJar);
            long patchedSize = Files.size(tempJar);

            Files.move(tempJar, patchedJar, StandardCopyOption.REPLACE_EXISTING);
            writeMetadata(metadata, metadata(originalJar, originalSha1, originalSize, patchedJar, patchedSha1, patchedSize, summary.replacements()));

            LauncherLog.info("Client jar patched: source=" + originalJar + ", patched=" + patchedJar + ", replacements=" + summary.replacements() + ", sha1=" + patchedSha1);
            return patchedJar;
        } catch (Exception exception) {
            deleteQuietly(tempJar);
            throw exception instanceof GameLaunchException launchException
                    ? launchException
                    : new GameLaunchException("Unable to patch old client jar.", exception);
        }
    }

    Path patchedJar(Path originalJar) {
        String fileName = originalJar.getFileName().toString();
        String baseName = fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return originalJar.resolveSibling(baseName + PATCHED_SUFFIX + ".jar");
    }

    Path patchedMetadata(Path originalJar) {
        String fileName = originalJar.getFileName().toString();
        String baseName = fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return originalJar.resolveSibling(baseName + PATCHED_SUFFIX + ".json");
    }

    private boolean isValidCached(Path originalJar, Path patchedJar, Path metadata, String expectedOriginalSha1) {
        try {
            if (!Files.isRegularFile(patchedJar) || !Files.isRegularFile(metadata)) return false;

            JsonObject json = readMetadata(metadata);
            if (json == null) return false;
            if (json.getInt("format", 0) != METADATA_FORMAT) return false;
            if (json.getInt("patcherVersion", 0) != PATCHER_VERSION) return false;
            if (!originalJar.getFileName().toString().equals(json.getString("originalJar", ""))) return false;
            if (!patchedJar.getFileName().toString().equals(json.getString("patchedJar", ""))) return false;
            if (json.getLong("originalSize", -1L) != Files.size(originalJar)) return false;
            if (json.getLong("patchedSize", -1L) != Files.size(patchedJar)) return false;
            if (!OLD_JOIN.equals(json.getString("oldJoinUrl", ""))) return false;
            if (!NEW_JOIN.equals(json.getString("newJoinUrl", ""))) return false;

            String originalSha1 = originalSha1(originalJar, expectedOriginalSha1);
            String cachedOriginalSha1 = normalizeSha1(json.getString("originalSha1", ""));
            if (cachedOriginalSha1.isBlank() || !cachedOriginalSha1.equals(originalSha1)) return false;

            String cachedPatchedSha1 = normalizeSha1(json.getString("patchedSha1", ""));
            if (cachedPatchedSha1.isBlank()) return false;
            return cachedPatchedSha1.equals(DownloadService.sha1(patchedJar));
        } catch (Exception exception) {
            LauncherLog.error("Unable to validate cached patched client jar metadata.", exception);
            return false;
        }
    }

    private PatchSummary patchJar(Path source, Path target) throws Exception {
        int replacements = 0;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(source));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (skipSignature(entry.getName())) continue;

                byte[] data = in.readAllBytes();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    PatchResult result = patchClass(data);
                    data = result.data();
                    replacements += result.replacements();
                }

                ZipEntry copy = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0) copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                if (!entry.isDirectory()) out.write(data);
                out.closeEntry();
            }
        }
        return new PatchSummary(replacements);
    }

    private PatchResult patchClass(byte[] data) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(data.length + 64);
        DataOutputStream out = new DataOutputStream(bytes);
        int replacements = 0;

        out.writeInt(in.readInt());
        out.writeShort(in.readUnsignedShort());
        out.writeShort(in.readUnsignedShort());

        int count = in.readUnsignedShort();
        out.writeShort(count);
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            out.writeByte(tag);
            switch (tag) {
                case 1 -> {
                    byte[] value = in.readNBytes(in.readUnsignedShort());
                    String text = new String(value, StandardCharsets.UTF_8);
                    String patched = patchText(text);
                    byte[] patchedBytes = patched.getBytes(StandardCharsets.UTF_8);
                    out.writeShort(patchedBytes.length);
                    out.write(patchedBytes);
                    replacements += replacements(text);
                }
                case 3, 4 -> out.write(in.readNBytes(4));
                case 5, 6 -> {
                    out.write(in.readNBytes(8));
                    i++;
                }
                case 7, 8, 16, 19, 20 -> out.write(in.readNBytes(2));
                case 9, 10, 11, 12, 17, 18 -> out.write(in.readNBytes(4));
                case 15 -> out.write(in.readNBytes(3));
                default -> throw new GameLaunchException("Unknown class constant pool tag: " + tag);
            }
        }
        out.write(in.readAllBytes());
        return replacements == 0 ? new PatchResult(data, 0) : new PatchResult(bytes.toByteArray(), replacements);
    }

    private JsonObject metadata(Path originalJar, String originalSha1, long originalSize, Path patchedJar, String patchedSha1, long patchedSize, int replacements) {
        JsonObject json = new JsonObject();
        json.put("format", METADATA_FORMAT);
        json.put("patcherVersion", PATCHER_VERSION);
        json.put("originalJar", originalJar.getFileName().toString());
        json.put("originalSha1", normalizeSha1(originalSha1));
        json.put("originalSize", originalSize);
        json.put("patchedJar", patchedJar.getFileName().toString());
        json.put("patchedSha1", normalizeSha1(patchedSha1));
        json.put("patchedSize", patchedSize);
        json.put("replacements", replacements);
        json.put("oldJoinUrl", OLD_JOIN);
        json.put("newJoinUrl", NEW_JOIN);
        return json;
    }

    private JsonObject readMetadata(Path metadata) {
        try (Reader reader = Files.newBufferedReader(metadata, StandardCharsets.UTF_8)) {
            return JsonParser.object().from(reader);
        } catch (Exception exception) {
            LauncherLog.error("Unable to read patched client metadata: " + metadata, exception);
            return null;
        }
    }

    private void writeMetadata(Path metadata, JsonObject json) throws Exception {
        Files.createDirectories(metadata.getParent());
        Path temp = Files.createTempFile(metadata.getParent(), metadata.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, JsonWriter.indent("  ").string().value(json).done(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, metadata, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception exception) {
                Files.move(temp, metadata, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            deleteQuietly(temp);
        }
    }

    private String originalSha1(Path originalJar, String expectedOriginalSha1) throws GameLaunchException {
        String expected = normalizeSha1(expectedOriginalSha1);
        return expected.isBlank() ? DownloadService.sha1(originalJar) : expected;
    }

    private String normalizeSha1(String sha1) {
        return sha1 == null ? "" : sha1.trim().toLowerCase(Locale.ROOT);
    }

    private String patchText(String text) {
        return text == null ? "" : text.replace(OLD_JOIN, NEW_JOIN);
    }

    private int replacements(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int index = text.indexOf(OLD_JOIN, from);
            if (index < 0) return count;
            count++;
            from = index + OLD_JOIN.length();
        }
    }

    private boolean skipSignature(String name) {
        String upper = name == null ? "" : name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/") && (upper.endsWith(".SF") || upper.endsWith(".DSA") || upper.endsWith(".RSA") || upper.endsWith(".EC"));
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
        }
    }

    private record PatchResult(byte[] data, int replacements) {
    }

    private record PatchSummary(int replacements) {
    }
}
