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

    /** Combines a child rotation inside this parent rotation into one set of model part angles. */
    public SiegePoseRotation composedWith(SiegePoseRotation child) {
        return matrix().times(child.matrix()).toRotation();
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

        private Matrix times(Matrix o) {
            return new Matrix(
                    m00 * o.m00 + m01 * o.m10 + m02 * o.m20,
                    m00 * o.m01 + m01 * o.m11 + m02 * o.m21,
                    m00 * o.m02 + m01 * o.m12 + m02 * o.m22,
                    m10 * o.m00 + m11 * o.m10 + m12 * o.m20,
                    m10 * o.m01 + m11 * o.m11 + m12 * o.m21,
                    m10 * o.m02 + m11 * o.m12 + m12 * o.m22,
                    m20 * o.m00 + m21 * o.m10 + m22 * o.m20,
                    m20 * o.m01 + m21 * o.m11 + m22 * o.m21,
                    m20 * o.m02 + m21 * o.m12 + m22 * o.m22
            );
        }

        /** Extracts Z*Y*X angles, the order model parts apply them in. */
        private SiegePoseRotation toRotation() {
            float y = (float) Math.asin(Mth.clamp(-m20, -1.0F, 1.0F));
            if (Math.abs(Mth.cos(y)) > 1.0E-6F) {
                return new SiegePoseRotation((float) Math.atan2(m21, m22), y, (float) Math.atan2(m10, m00));
            }
            return new SiegePoseRotation((float) Math.atan2(-m12, m11), y, 0.0F);
        }
    }
}
