package net.litelauncher.frontend.modules.skin;

public record SkinModelVector(double x, double y, double z) {

    double dot(SkinModelVector other) {
        return x * other.x + y * other.y + z * other.z;
    }

    SkinModelVector unit() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length == 0.0 ? this : new SkinModelVector(x / length, y / length, z / length);
    }
}
