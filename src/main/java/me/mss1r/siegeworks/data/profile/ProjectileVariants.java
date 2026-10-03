package me.mss1r.siegeworks.data.profile;

import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.resources.ResourceLocation;

/** Projectile profiles for ammunition that shares an entity type with another but hits differently. */
public final class ProjectileVariants {
    public static final ResourceLocation EXPLOSIVE_SINGIJEON = id("explosive_singijeon");
    public static final ResourceLocation STONE_SCATTERSHOT = id("stone_scattershot");
    public static final ResourceLocation MANGONEL_FIRE_PROJECTILE = id("mangonel_fire_projectile");
    public static final ResourceLocation TREBUCHET_FIRE_PROJECTILE = id("trebuchet_fire_projectile");

    private ProjectileVariants() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, path);
    }
}
