package net.litelauncher.frontend.modules.skin;

import net.litelauncher.backend.LauncherLog;
import net.litelauncher.backend.auth.Profile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.UUID;

public final class DefaultPlayerSkin {

    private static final String[] NAMES = {
            "alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri"
    };
    private static final Skin[] SKINS = loadSkins();

    private DefaultPlayerSkin() {
    }

    public static Skin forProfile(Profile profile) {
        UUID uuid;
        try {
            uuid = UUID.fromString(profile == null ? null : profile.id());
        } catch (Exception _) {
            uuid = new UUID(0, 0);
        }
        return SKINS[Math.floorMod(uuid.hashCode(), SKINS.length)];
    }

    private static Skin[] loadSkins() {
        Skin[] skins = new Skin[NAMES.length * 2];
        for (int index = 0; index < NAMES.length; index++) {
            skins[index] = load(NAMES[index], true);
            skins[index + NAMES.length] = load(NAMES[index], false);
        }
        return skins;
    }

    private static Skin load(String name, boolean slim) {
        String model = slim ? "slim" : "wide";
        String resource = "assets/common/skins/" + model + "/" + name + ".png";
        try (InputStream stream = DefaultPlayerSkin.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing resource: " + resource);
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IllegalStateException("Invalid image: " + resource);
            return new Skin(image, slim);
        } catch (Exception exception) {
            LauncherLog.error("Unable to load default skin: " + resource, exception);
        }
        return new Skin(null, slim);
    }

    public record Skin(BufferedImage image, boolean slim) {
    }
}
