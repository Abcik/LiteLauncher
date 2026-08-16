package net.litelauncher.backend.loader;

import net.litelauncher.backend.BackendUtils;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.litelauncher.backend.LauncherLog;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class LoaderCatalog {

    private static final String FABRIC_LOADERS = "https://meta.fabricmc.net/v2/versions/loader/%s";
    private static final String QUILT_LOADERS = "https://meta.quiltmc.org/v3/versions/loader/%s";
    private static final String FORGE_PROMOTIONS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String NEOFORGE_METADATA = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String LEGACY_NEOFORGE_METADATA = "https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml";
    private static final String OPTIFINE_LIST = "https://bmclapi2.bangbang93.com/optifine/%s";
    private static final String OPTIFINE_DOWNLOAD = "https://bmclapi2.bangbang93.com/optifine/%s/%s/%s";

    private final HttpClient http = BackendUtils.http();

    private final Map<String, List<LoaderOption>> cache = new ConcurrentHashMap<>();

    public List<LoaderOption> resolve(String minecraftVersion) {
        String mc = safe(minecraftVersion);
        if (mc.isBlank()) return unavailableOptions();
        return cache.computeIfAbsent(mc, this::resolveUncached);
    }

    private List<LoaderOption> resolveUncached(String mc) {
        LoaderType[] types = LoaderType.values();
        LoaderOption[] options = new LoaderOption[types.length];
        List<Thread> workers = new ArrayList<>(types.length);
        for (int index = 0; index < types.length; index++) {
            int slot = index;
            LoaderType type = types[index];
            workers.add(Thread.ofVirtual().name("loader-catalog-" + type.name().toLowerCase(Locale.ROOT))
                    .unstarted(() -> options[slot] = resolveOption(type, mc)));
        }
        workers.forEach(Thread::start);

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                workers.forEach(Thread::interrupt);
                return unavailableOptions();
            }
        }
        return List.of(options);
    }

    private LoaderOption resolveOption(LoaderType type, String mc) {
        try {
            return new LoaderOption(type, resolve(type, mc));
        } catch (Exception exception) {
            LauncherLog.error("Unable to resolve " + type.title() + " for Minecraft " + mc + ".", exception);
            return new LoaderOption(type, null);
        }
    }

    private List<LoaderOption> unavailableOptions() {
        return Arrays.stream(LoaderType.values()).map(type -> new LoaderOption(type, null)).toList();
    }

    private LoaderVersion resolve(LoaderType type, String mc) throws Exception {
        return switch (type) {
            case FABRIC -> fabric(mc);
            case QUILT -> quilt(mc);
            case FORGE -> forge(mc);
            case NEOFORGE -> neoForge(mc);
            case OPTIFINE -> optiFine(mc);
        };
    }

    private LoaderVersion fabric(String mc) throws Exception {
        JsonArray array = JsonParser.array().from(get(FABRIC_LOADERS.formatted(segment(mc))));
        List<Candidate> candidates = new ArrayList<>();
        for (Object item : array) {
            if (!(item instanceof JsonObject object)) continue;
            JsonObject loader = object.getObject("loader");
            if (loader == null) loader = object;
            String version = loader.getString("version", "");
            if (version.isBlank()) continue;
            boolean stable = loader.getBoolean("stable", true);
            candidates.add(new Candidate(version, stable));
        }
        Candidate selected = candidates.stream().filter(Candidate::stable)
                .max(Comparator.comparing(Candidate::version, LoaderCatalog::compareVersions)).orElse(null);
        if (selected == null) return null;
        String id = "fabric-loader-" + selected.version() + "-" + mc;
        return new LoaderVersion(LoaderType.FABRIC, mc, selected.version(), id, selected.version(), "");
    }

    private LoaderVersion quilt(String mc) throws Exception {
        JsonArray array = JsonParser.array().from(get(QUILT_LOADERS.formatted(segment(mc))));
        List<Candidate> candidates = new ArrayList<>();
        for (Object item : array) {
            if (!(item instanceof JsonObject object)) continue;
            JsonObject loader = object.getObject("loader");
            if (loader == null) loader = object;
            String version = loader.getString("version", "");
            if (version.isBlank()) continue;
            boolean stable = !isPrerelease(version);
            candidates.add(new Candidate(version, stable));
        }
        Candidate selected = candidates.stream()
                .max(Comparator.comparing(Candidate::version, LoaderCatalog::compareVersions)).orElse(null);
        if (selected == null) return null;
        String id = "quilt-loader-" + selected.version() + "-" + mc;
        return new LoaderVersion(LoaderType.QUILT, mc, selected.version(), id, selected.version(), "");
    }

    private LoaderVersion forge(String mc) throws Exception {
        JsonObject root = JsonParser.object().from(get(FORGE_PROMOTIONS));
        JsonObject promos = root.getObject("promos", new JsonObject());
        String version = promos.getString(mc + "-recommended", "");
        if (version.isBlank()) version = promos.getString(mc + "-latest", "");
        if (version.isBlank()) return null;
        String coordinate = mc + "-" + version;
        String id = mc + "-forge-" + version;
        return new LoaderVersion(LoaderType.FORGE, mc, version, id, coordinate, "");
    }

    private LoaderVersion neoForge(String mc) throws Exception {
        boolean legacy = "1.20.1".equals(mc);
        String xml = get(legacy ? LEGACY_NEOFORGE_METADATA : NEOFORGE_METADATA);
        List<Candidate> candidates = new ArrayList<>();
        for (String artifact : readMavenVersions(xml)) {
            if (!neoForgeMatchesMinecraft(mc, artifact, legacy)) continue;
            candidates.add(new Candidate(artifact, !isPrerelease(artifact)));
        }
        Candidate selected = candidates.stream().filter(Candidate::stable)
                .max(Comparator.comparing(Candidate::version, LoaderCatalog::compareVersions))
                .orElseGet(() -> candidates.stream()
                        .filter(candidate -> candidate.version().toLowerCase(Locale.ROOT).contains("beta"))
                        .max(Comparator.comparing(Candidate::version, LoaderCatalog::compareVersions))
                        .orElse(null));
        if (selected == null) return null;

        String artifact = selected.version();
        String display = legacy && artifact.startsWith(mc + "-")
                ? artifact.substring((mc + "-").length())
                : artifact;
        String id = legacy ? mc + "-forge-" + display : "neoforge-" + artifact;
        return new LoaderVersion(LoaderType.NEOFORGE, mc, display, id, artifact, "");
    }

    private LoaderVersion optiFine(String mc) throws Exception {
        JsonArray array = JsonParser.array().from(get(OPTIFINE_LIST.formatted(segment(mc))));
        List<OptiFineCandidate> candidates = new ArrayList<>();
        for (Object item : array) {
            if (!(item instanceof JsonObject object)) continue;
            String itemMc = object.getString("mcversion", mc);
            if (!itemMc.isBlank() && !mc.equals(itemMc)) continue;
            String type = object.getString("type", "");
            String patch = object.getString("patch", "");
            String filename = object.getString("filename", "");
            if (type.isBlank() || patch.isBlank()) continue;
            String version = optiFineVersion(mc, type, patch, filename);
            boolean stable = !isPrerelease(version) && !filename.toLowerCase(Locale.ROOT).startsWith("preview_");
            candidates.add(new OptiFineCandidate(type, patch, version, stable));
        }
        OptiFineCandidate selected = candidates.stream()
                .filter(OptiFineCandidate::stable)
                .max(Comparator.comparing(OptiFineCandidate::version, LoaderCatalog::compareVersions))
                .orElseGet(() -> candidates.stream()
                        .max(Comparator.comparing(OptiFineCandidate::version, LoaderCatalog::compareVersions))
                        .orElse(null));
        if (selected == null) return null;

        String id = mc + "-OptiFine_" + selected.version();
        String url = OPTIFINE_DOWNLOAD.formatted(segment(mc), segment(selected.type()), segment(selected.patch()));
        return new LoaderVersion(LoaderType.OPTIFINE, mc, selected.version(), id,
                selected.type() + "/" + selected.patch(), url);
    }

    private List<String> readMavenVersions(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList nodes = document.getElementsByTagName("version");
        List<String> result = new ArrayList<>(nodes.getLength());
        for (int index = 0; index < nodes.getLength(); index++) {
            String version = nodes.item(index).getTextContent();
            if (version != null && !version.isBlank()) result.add(version.trim());
        }
        return result;
    }

    private boolean neoForgeMatchesMinecraft(String mc, String artifact, boolean legacy) {
        if (legacy) return artifact.startsWith(mc + "-");
        if (mc.matches("1\\.\\d+(?:\\.\\d+)?")) {
            String[] parts = mc.split("\\.");
            String patch = parts.length >= 3 ? parts[2] : "0";
            return artifact.startsWith(parts[1] + "." + patch + ".");
        }
        if (mc.matches("\\d+\\.\\d+(?:\\.\\d+)?")) {
            String[] parts = mc.split("\\.");
            String patch = parts.length >= 3 ? parts[2] : "0";
            return artifact.startsWith(parts[0] + "." + parts[1] + "." + patch + ".");
        }
        return false;
    }

    private String optiFineVersion(String mc, String type, String patch, String filename) {
        String value = safe(filename);
        if (!value.isBlank()) {
            if (value.startsWith("preview_")) value = value.substring("preview_".length());
            String prefix = "OptiFine_" + mc + "_";
            if (value.startsWith(prefix)) value = value.substring(prefix.length());
            if (value.toLowerCase(Locale.ROOT).endsWith(".jar")) value = value.substring(0, value.length() - 4);
            if (!value.isBlank()) return value;
        }
        return (safe(type) + "_" + safe(patch)).replace(' ', '_');
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .header("User-Agent", "LiteLauncher/" + System.getProperty("java.version", "java"))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Loader metadata returned HTTP " + response.statusCode() + " for " + url);
        return response.body();
    }

    private static boolean isPrerelease(String version) {
        String lower = safe(version).toLowerCase(Locale.ROOT);
        return lower.contains("beta") || lower.contains("alpha") || lower.contains("preview")
                || lower.contains("pre") || lower.contains("snapshot") || lower.contains("rc");
    }

    static int compareVersions(String left, String right) {
        List<Token> a = tokenize(left);
        List<Token> b = tokenize(right);
        int count = Math.max(a.size(), b.size());
        for (int i = 0; i < count; i++) {
            if (i >= a.size()) return -1;
            if (i >= b.size()) return 1;
            Token x = a.get(i);
            Token y = b.get(i);
            int cmp;
            if (x.number() && y.number()) {
                cmp = compareNumeric(x.value(), y.value());
            } else if (x.number() != y.number()) {
                cmp = x.number() ? 1 : -1;
            } else {
                cmp = x.value().compareToIgnoreCase(y.value());
            }
            if (cmp != 0) return cmp;
        }
        return safe(left).compareToIgnoreCase(safe(right));
    }

    private static int compareNumeric(String left, String right) {
        String a = left.replaceFirst("^0+(?!$)", "");
        String b = right.replaceFirst("^0+(?!$)", "");
        int length = Integer.compare(a.length(), b.length());
        return length != 0 ? length : a.compareTo(b);
    }

    private static List<Token> tokenize(String value) {
        List<Token> result = new ArrayList<>();
        String text = safe(value);
        if (text.isEmpty()) return result;
        StringBuilder token = new StringBuilder();
        boolean number = Character.isDigit(text.charAt(0));
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean digit = Character.isDigit(c);
            if (digit != number && !token.isEmpty()) {
                result.add(new Token(token.toString(), number));
                token.setLength(0);
                number = digit;
            }
            token.append(c);
        }
        if (!token.isEmpty()) result.add(new Token(token.toString(), number));
        return result;
    }

    private static String segment(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Candidate(String version, boolean stable) {}
    private record OptiFineCandidate(String type, String patch, String version, boolean stable) {}
    private record Token(String value, boolean number) {}
}
