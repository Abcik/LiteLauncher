package net.litelauncher.bootstrap;

import net.litelauncher.backend.platform.OSUtils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record LauncherManifest(
        String version,
        String file,
        String checksumAlgorithm,
        String checksum,
        long size,
        int javaMajor,
        String signature
) {

    static LauncherManifest parse(String json) throws BootstrapException {
        String version = string(json, "version");
        String file = string(json, "file");
        String sha256 = normalizeHash(string(json, "sha256"));
        String sha1 = normalizeHash(string(json, "sha1"));
        long size = number(json, "size", 0L);
        int javaMajor = (int) number(json, "javaMajor", 0L);
        String signature = string(json, "signature");

        String algorithm;
        String checksum;
        if (sha256.length() == 64) {
            algorithm = "SHA-256";
            checksum = sha256;
        } else if (sha1.length() == 40) {
            algorithm = "SHA-1";
            checksum = sha1;
        } else {
            throw new BootstrapException("Manifest error.");
        }

        if (version.isBlank()) throw new BootstrapException("Manifest error.");
        if (file.isBlank()) throw new BootstrapException("Manifest error.");
        if (size <= 0L) throw new BootstrapException("Manifest error.");
        if (javaMajor <= 0) javaMajor = OSUtils.currentJavaMajor();

        return new LauncherManifest(version, file, algorithm, checksum, size, javaMajor, signature);
    }

    boolean verify(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        if (signature == null || signature.isBlank()) return false;
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKeyBytes)));
        verifier.update(signaturePayload().getBytes(StandardCharsets.UTF_8));
        return verifier.verify(signatureBytes);
    }

    private String signaturePayload() {
        return version + "," + file + "," + checksum + "," + size + "," + javaMajor;
    }

    private static String normalizeHash(String value) {
        return value == null ? "" : value.replace(" ", "").replace(":", "").trim();
    }

    private static String string(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json == null ? "" : json);
        return matcher.find() ? unescape(matcher.group(1)).trim() : "";
    }

    private static long number(String json, String key, long fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json == null ? "" : json);
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
}
