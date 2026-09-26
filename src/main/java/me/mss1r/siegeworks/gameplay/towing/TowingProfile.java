package me.mss1r.siegeworks.gameplay.towing;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public record TowingProfile(List<MountSlot> mountSlots, float modelTurnDegrees, Vec3 pivotOffset) {
    private static final float DRAWN_FROM_BEHIND = 180.0F;

    public TowingProfile {
        mountSlots = List.copyOf(mountSlots);
        if (mountSlots.isEmpty()) {
            throw new IllegalArgumentException("A towing profile needs at least one mount slot");
        }
    }

    public record MountSlot(Vec3 mountOffset, String leftAnchor, String rightAnchor) {
        private static MountSlot single(Vec3 mountOffset) {
            return new MountSlot(mountOffset, "shaft_left", "shaft_right");
        }

        private static MountSlot numbered(Vec3 mountOffset, int number) {
            return new MountSlot(mountOffset, "shaft_left_" + number, "shaft_right_" + number);
        }
    }

    public Vec3 mountOffset() {
        return mountSlots.get(0).mountOffset();
    }

    public static TowingProfile drawnFromBehind(double mountDistance) {
        return standing(new Vec3(0.0D, 0.0D, -mountDistance));
    }

    public static TowingProfile drawnFromBehind(double mountDistance, double... lateralOffsets) {
        if (lateralOffsets.length == 0) {
            return drawnFromBehind(mountDistance);
        }
        List<MountSlot> slots = java.util.stream.IntStream.range(0, lateralOffsets.length)
                .mapToObj(index -> MountSlot.numbered(
                        new Vec3(lateralOffsets[index], 0.0D, -mountDistance), index + 1))
                .toList();
        return new TowingProfile(slots, DRAWN_FROM_BEHIND, Vec3.ZERO);
    }

    public static TowingProfile pushedFromBehind(double mountDistance, double... lateralOffsets) {
        if (lateralOffsets.length == 0) {
            throw new IllegalArgumentException("A pushing team needs at least one lateral slot");
        }
        List<MountSlot> slots = java.util.stream.IntStream.range(0, lateralOffsets.length)
                .mapToObj(index -> MountSlot.numbered(
                        new Vec3(-lateralOffsets[index], 0.0D, mountDistance), index + 1))
                .toList();
        return new TowingProfile(slots, 0.0F, Vec3.ZERO);
    }

    public static TowingProfile standing(Vec3 mountOffset) {
        return new TowingProfile(List.of(MountSlot.single(mountOffset)), DRAWN_FROM_BEHIND,
                Vec3.ZERO);
    }

    public TowingProfile turningAbout(double axleOffset) {
        return new TowingProfile(mountSlots, modelTurnDegrees,
                new Vec3(0.0D, 0.0D, axleOffset));
    }
}
