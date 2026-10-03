package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ProjectileImpactEffects {
    public enum Style {
        STANDARD,
        HEAVY,
        TREBUCHET
    }

    private ProjectileImpactEffects() {
    }

    public static void playPenetrationReport(ServerLevel level, Vec3 impact, float volume, float pitch) {
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, volume * 0.28F, pitch * 1.35F);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, volume * 0.18F, pitch * 1.25F);
    }

    public static void playImpactReport(ServerLevel level, Vec3 impact, float volume, float pitch, Style style) {
        playImpactReport(level, impact, volume, pitch, style, new Vec3(0.0D, 1.0D, 0.0D));
    }

    public static void playImpactReport(ServerLevel level, Vec3 impact, float volume, float pitch,
                                        Style style, Vec3 outwardNormal) {
        switch (style) {
            case HEAVY -> playHeavyReport(level, impact, volume, pitch, outwardNormal);
            case TREBUCHET -> playTrebuchetReport(level, impact, volume, pitch, outwardNormal);
            case STANDARD -> playStandardReport(level, impact, volume, pitch, outwardNormal);
        }
    }

    public static void spawnImpactParticles(ServerLevel level, Vec3 impact, float intensity) {
        spawnImpactParticles(level, impact, intensity, new Vec3(0.0D, 1.0D, 0.0D));
    }

    public static void spawnImpactParticles(ServerLevel level, Vec3 impact, float intensity, Vec3 outwardNormal) {
        SiegeParticleEffects.impact(level, impact, intensity, false, outwardNormal);
    }

    public static void spawnScattershotImpactParticles(ServerLevel level, Vec3 impact,
                                                       boolean stonePellet, Vec3 outwardNormal) {
        SiegeParticleEffects.scattershotImpact(level, impact, stonePellet, outwardNormal);
    }

    public static void spawnPenetrationParticles(ServerLevel level, Vec3 center,
                                                 double radius, Vec3 outwardNormal) {
        SiegeParticleEffects.penetrationImpact(level, center, radius, outwardNormal);
    }

    private static void playStandardReport(ServerLevel level, Vec3 impact, float volume, float pitch,
                                           Vec3 outwardNormal) {
        float scale = Mth.clamp(volume / 4.0F, 0.85F, 1.2F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_EXPLOSION_LAYER.get(), 0.78F * scale, pitch * 0.95F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_EXPLOSION_LAYER.get(), 0.25F * scale, pitch * 1.08F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_METAL_LAYER.get(), 0.62F * scale, pitch * 0.9F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_NETHERITE_LAYER.get(), 0.92F * scale, pitch * 0.95F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_BODY_LAYER.get(), 0.58F * scale, pitch * 0.85F);
        spawnImpactParticles(level, impact, volume, outwardNormal);
    }

    private static void playHeavyReport(ServerLevel level, Vec3 impact, float volume, float pitch,
                                        Vec3 outwardNormal) {
        float scale = Mth.clamp(volume / 8.0F, 1.0F, 1.3F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_EXPLOSION_LAYER.get(), 0.95F * scale, pitch * 1.05F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_EXPLOSION_LAYER.get(), 0.48F * scale, pitch * 1.18F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_FIREWORK_LAYER.get(), 0.45F * scale, pitch * 1.05F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_ANVIL_LAYER.get(), 0.28F * scale, pitch * 0.95F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_METAL_LAYER.get(), 0.68F * scale, pitch * 1.02F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_NETHERITE_LAYER.get(), 1.0F * scale, pitch * 1.05F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_FALL_LAYER.get(), 0.62F * scale, pitch);
        SiegeParticleEffects.impact(level, impact, volume, true, outwardNormal);
    }

    private static void playTrebuchetReport(ServerLevel level, Vec3 impact, float volume, float pitch,
                                            Vec3 outwardNormal) {
        float scale = Mth.clamp(volume / 4.0F, 0.85F, 1.2F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_EXPLOSION_LAYER.get(), 0.45F * scale, pitch * 1.1F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_BODY_LAYER.get(), 0.78F * scale, pitch * 1.05F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_STONE_LAYER.get(), 1.0F * scale, pitch * 1.05F);
        playLayer(level, impact, SiegeworksSounds.IMPACT_SHARP_STONE_LAYER.get(), 0.9F * scale, pitch * 1.12F);
        spawnImpactParticles(level, impact, volume, outwardNormal);
    }

    private static void playLayer(ServerLevel level, Vec3 impact,
                                  net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, impact.x, impact.y, impact.z,
                sound, SoundSource.BLOCKS, volume, Mth.clamp(pitch, 0.8F, 1.2F));
    }
}
