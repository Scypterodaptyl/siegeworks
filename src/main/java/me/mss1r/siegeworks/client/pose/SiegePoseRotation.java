package me.mss1r.siegeworks.client.pose;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public record SiegePoseRotation(float x, float y, float z) {
    public static final SiegePoseRotation ZERO = new SiegePoseRotation(0.0F, 0.0F, 0.0F);

    public static SiegePoseRotation fromDegrees(float x, float y, float z) {
        return new SiegePoseRotation(x * Mth.DEG_TO_RAD, y * Mth.DEG_TO_RAD, z * Mth.DEG_TO_RAD);
    }

    public SiegePoseRotation plusX(float delta) {
        return Math.abs(delta) < 1.0E-6F ? this : new SiegePoseRotation(x + delta, y, z);
    }

    public SiegePoseRotation plus(SiegePoseRotation other) {
        return new SiegePoseRotation(x + other.x, y + other.y, z + other.z);
    }

    public void apply(ModelPart part) {
        part.setRotation(x, y, z);
    }

    public Point rotate(float pointX, float pointY, float pointZ) {
        return matrix().rotate(pointX, pointY, pointZ);
    }

    public record Point(float x, float y, float z) {
    }

    private Matrix matrix() {
        float sinX = Mth.sin(x);
        float cosX = Mth.cos(x);
        float sinY = Mth.sin(y);
        float cosY = Mth.cos(y);
        float sinZ = Mth.sin(z);
        float cosZ = Mth.cos(z);
        return new Matrix(
                cosZ * cosY,
                cosZ * sinY * sinX - sinZ * cosX,
                cosZ * sinY * cosX + sinZ * sinX,
                sinZ * cosY,
                sinZ * sinY * sinX + cosZ * cosX,
                sinZ * sinY * cosX - cosZ * sinX,
                -sinY,
                cosY * sinX,
                cosY * cosX
        );
    }

    private record Matrix(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22
    ) {
        private Point rotate(float x, float y, float z) {
            return new Point(
                    m00 * x + m01 * y + m02 * z,
                    m10 * x + m11 * y + m12 * z,
                    m20 * x + m21 * y + m22 * z
            );
        }

    }
}
