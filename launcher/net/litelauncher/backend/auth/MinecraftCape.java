package net.litelauncher.backend.auth;

public record MinecraftCape(String id, String name, String png, boolean active, String textureUrl) {

    public MinecraftCape {
        id = AuthUtils.text(id);
        name = AuthUtils.firstText(name, "Cape");
        png = AuthUtils.text(png);
        active = active && id != null;
        textureUrl = AuthUtils.text(textureUrl);
    }
}
