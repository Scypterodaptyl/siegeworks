package me.mss1r.siegeworks.api;

import net.minecraft.world.phys.Vec3;

public interface ProjectilePassThroughControl {
    boolean allowsProjectilePassage(Vec3 start, Vec3 end);
}
