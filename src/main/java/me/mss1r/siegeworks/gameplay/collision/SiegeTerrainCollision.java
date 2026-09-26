package me.mss1r.siegeworks.gameplay.collision;

import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.axiomata.collision.system.StructureTerrainCollision;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.phys.Vec3;

public final class SiegeTerrainCollision {
    private SiegeTerrainCollision() {
    }

    public static Vec3 clampMovement(AbstractSiegeEntity siege, Vec3 wanted) {
        if (!siege.usesGeometryTerrainCollision()) {
            return wanted;
        }
        return StructureTerrainCollision.clampMovement(
                siege, wanted, siege.geometryStepHeight(), siege::isTerrainCollisionExcluded);
    }

    public static boolean canOccupy(AbstractSiegeEntity siege, StructureTransform current,
                                    StructureTransform wanted) {
        return !siege.usesGeometryTerrainCollision()
                || StructureTerrainCollision.canOccupy(
                        siege, current, wanted,
                        siege.geometryStepHeight(), siege::isTerrainCollisionExcluded);
    }

    public static void unstick(AbstractSiegeEntity siege) {
        if (siege.usesGeometryTerrainCollision()) {
            StructureTerrainCollision.unstick(
                    siege, siege.geometryStepHeight(), siege::isTerrainCollisionExcluded);
        }
    }
}
