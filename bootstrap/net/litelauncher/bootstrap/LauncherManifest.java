package net.litelauncher.bootstrap;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LauncherManifest {

    final String version;
    final String file;
    final String sha256;
    final long size;
    final int javaMajor;
    final String signature;

    private LauncherManifest(String version, String file, String sha256, long size, int javaMajor, String signature) {
        this.version = version;
        this.file = file;
        this.sha256 = sha256;
        this.size = size;
        this.javaMajor = javaMajor;
        this.signature = signature;
    }

    static LauncherManifest parse(String json) throws BootstrapException {
        String version = string(json, "version");
        String file = string(json, "file");
        String sha256 = string(json, "sha256");
        long size = number(json, "size", 0L);
        int javaMajor = (int) number(json, "javaMajor", 0L);
        String signature = string(json, "signature");

        if (version.isBlank()) throw new BootstrapException("Manifest error.");
        if (file.isBlank()) throw new BootstrapException("Manifest error.");
        if (sha256.length() != 64) throw new BootstrapException("Manifest error.");
        if (size <= 0L) throw new BootstrapException("Manifest error.");
        if (javaMajor <= 0) javaMajor = currentJavaMajor();

        return new LauncherManifest(version, file, sha256, size, javaMajor, signature);
    }

    String checksumAlgorithm() {
        return "SHA-256";
    }

    String checksum() {
        return sha256;
    }

    String signature() {
        return signature;
    }

    private static String string(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json == null ? "" : json);
        return matcher.find() ? unescape(matcher.group(1)).trim() : "";
    }

    private static long number(String json, String key, long fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json == null ? "" : json);
        if (!matcher.find()) return fallback;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                if (c == 'n') result.append('\n');
                else if (c == 'r') result.append('\r');
                else if (c == 't') result.append('\t');
                else result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static int currentJavaMajor() {
        String version = System.getProperty("java.version", "17");
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.substring(2, 3));
            int dot = version.indexOf('.');
            return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
        } catch (Exception ignored) {
            return 17;
        }
    }

    public boolean verify(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKeyBytes)));
        String payload = version + "," + file + "," + sha256 + "," + size + "," + javaMajor;
        verifier.update(payload.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(signatureBytes);
    }

}
