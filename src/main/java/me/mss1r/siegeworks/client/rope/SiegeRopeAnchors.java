package me.mss1r.siegeworks.client.rope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class SiegeRopeAnchors {
    private SiegeRopeAnchors() {
    }

    public static Optional<Vec3> modelPosition(GeoModel<?> model, String boneName) {
        return model.getBone(boneName).map(SiegeRopeAnchors::pivotPosition);
    }

    private static Vec3 pivotPosition(GeoBone target) {
        List<GeoBone> chain = new ArrayList<>();
        for (GeoBone bone = target; bone != null; bone = bone.getParent()) {
            chain.add(bone);
        }

        Vec3 position = Vec3.ZERO;
        Frame frame = Frame.IDENTITY;

        for (int index = chain.size() - 1; index >= 0; index--) {
            GeoBone bone = chain.get(index);

            position = position.add(frame.apply(
                    -bone.getPosX() / 16.0D, bone.getPosY() / 16.0D, bone.getPosZ() / 16.0D));
            position = position.add(frame.apply(
                    bone.getPivotX() / 16.0D, bone.getPivotY() / 16.0D, bone.getPivotZ() / 16.0D));

            frame = frame.turnedBy(bone);

            if (index > 0) {
                position = position.add(frame.apply(
                        -bone.getPivotX() / 16.0D, -bone.getPivotY() / 16.0D, -bone.getPivotZ() / 16.0D));
            }
        }
        return position;
    }

    private record Frame(Vec3 x, Vec3 y, Vec3 z) {
        private static final Frame IDENTITY = new Frame(
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D));

        private Vec3 apply(double alongX, double alongY, double alongZ) {
            return x.scale(alongX).add(y.scale(alongY)).add(z.scale(alongZ));
        }

        private Vec3 apply(Vec3 local) {
            return apply(local.x, local.y, local.z);
        }

        private Frame turnedBy(GeoBone bone) {
            double rotX = bone.getRotX();
            double rotY = bone.getRotY();
            double rotZ = bone.getRotZ();
            if (rotX == 0.0D && rotY == 0.0D && rotZ == 0.0D) {
                return this;
            }
            return new Frame(
                    apply(turn(new Vec3(1.0D, 0.0D, 0.0D), rotX, rotY, rotZ)),
                    apply(turn(new Vec3(0.0D, 1.0D, 0.0D), rotX, rotY, rotZ)),
                    apply(turn(new Vec3(0.0D, 0.0D, 1.0D), rotX, rotY, rotZ)));
        }

        private static Vec3 turn(Vec3 value, double rotX, double rotY, double rotZ) {
            return rotateZ(rotateY(rotateX(value, rotX), rotY), rotZ);
        }

        private static Vec3 rotateX(Vec3 value, double angle) {
            if (angle == 0.0D) {
                return value;
            }
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            return new Vec3(value.x, value.y * cos - value.z * sin, value.y * sin + value.z * cos);
        }

        private static Vec3 rotateY(Vec3 value, double angle) {
            if (angle == 0.0D) {
                return value;
            }
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            return new Vec3(value.x * cos + value.z * sin, value.y, -value.x * sin + value.z * cos);
        }

        private static Vec3 rotateZ(Vec3 value, double angle) {
            if (angle == 0.0D) {
                return value;
            }
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            return new Vec3(value.x * cos - value.y * sin, value.x * sin + value.y * cos, value.z);
        }
    }
}
