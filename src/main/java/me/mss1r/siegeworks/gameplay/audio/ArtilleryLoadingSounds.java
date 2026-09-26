package me.mss1r.siegeworks.gameplay.audio;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class ArtilleryLoadingSounds {
    private static final int POWDER = 0;
    private static final int RAM_CHARGE = 1;
    private static final int AMMUNITION = 2;
    private static final int RAM_PROJECTILE = 3;
    private static final int PRIME = 4;

    private ArtilleryLoadingSounds() {
    }

    public static void playStage(ServerLevel level, AbstractSiegeEntity siege, int stage, boolean heavy) {
        float weight = heavy ? 1.2F : 1.0F;
        float pitchScale = heavy ? 0.78F : 1.0F;

        switch (stage) {
            case POWDER -> play(level, siege, SoundEvents.SAND_PLACE,
                    0.72F * weight, 0.82F * pitchScale);
            case RAM_CHARGE, RAM_PROJECTILE -> {
                play(level, siege, SoundEvents.WOOD_HIT,
                        0.72F * weight, 0.72F * pitchScale);
                play(level, siege, SoundEvents.CHAIN_STEP,
                        0.28F * weight, 0.76F * pitchScale);
            }
            case AMMUNITION -> {
                play(level, siege, SoundEvents.STONE_PLACE,
                        0.9F * weight, 0.68F * pitchScale);
                play(level, siege, SoundEvents.ANVIL_LAND,
                        0.18F * weight, 1.45F * pitchScale);
            }
            case PRIME -> play(level, siege, SoundEvents.FLINTANDSTEEL_USE,
                    0.72F * weight, 0.92F * pitchScale);
            default -> {
            }
        }
    }

    private static void play(ServerLevel level, AbstractSiegeEntity siege,
                             SoundEvent sound, float volume, float pitch) {
        level.playSound(null, siege.getX(), siege.getY() + 0.8D, siege.getZ(),
                sound, SoundSource.BLOCKS, volume, pitch);
    }
}
