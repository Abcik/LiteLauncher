package net.litelauncher.backend.modules.auth;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.platform.OSUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class OfflineProfileStore {

    public List<Profile> read() {
        List<Profile> profiles = new ArrayList<>();
        try {
            if (!Files.isRegularFile(OSUtils.offlineSessionsFile())) return profiles;
            JsonArray accounts = JsonParser.object().from(Files.readString(OSUtils.offlineSessionsFile(), StandardCharsets.UTF_8))
                    .getArray("accounts", new JsonArray());
            for (Object item : accounts) {
                if (!(item instanceof JsonObject json)) continue;
                String username = AuthUtils.text(json.getString("username"));
                if (username != null) profiles.add(Profile.offline(username));
            }
        } catch (Exception exception) {
            LauncherLog.error("Unable to read offline profiles.", exception);
            AuthUtils.deleteQuietly(OSUtils.offlineSessionsFile());
        }
        return profiles;
    }

    public void write(List<Profile> profiles) {
        try {
            Files.createDirectories(OSUtils.authDirectory());

            JsonArray accounts = new JsonArray();
            if (profiles != null) {
                for (Profile profile : profiles) {
                    if (profile == null || profile.microsoft()) continue;
                    JsonObject json = new JsonObject();
                    json.put("id", profile.id());
                    json.put("username", profile.username());
                    accounts.add(json);
                }
            }

            JsonObject root = new JsonObject();
            root.put("version", 1);
            root.put("accounts", accounts);
            AuthUtils.writeAtomic(OSUtils.offlineSessionsFile(), JsonWriter.indent("  ").string().value(root).done());
        } catch (Exception exception) {
            LauncherLog.error("Unable to write offline profiles.", exception);
        }
    }
}
