package me.mss1r.siegeworks.gameplay.audio;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public final class SiegeAudioController {
    private static final double MOVEMENT_EPSILON = 1.0E-3D;
    private static final double FULL_VOLUME_RADIUS = 8.0D;

    private static final int STOP_GRACE_TICKS = 8;
    private static final int MOVEMENT_CLIP_INTERVAL_TICKS = 18;

    private final AbstractSiegeEntity siege;
    private final Random random = new Random();
    private int movementTicks;
    private int stationaryTicks;

    public SiegeAudioController(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public void tickMovement(ServerLevel level, int maximumIntervalTicks) {
        Vec3 movement = siege.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        boolean moving = horizontalSpeed > MOVEMENT_EPSILON;

        if (moving && siege.isAlive()) {
            stationaryTicks = 0;
            float speedFactor = Mth.clamp((float) (horizontalSpeed / 0.12D), 0.0F, 1.0F);
            float volume = Mth.lerp(speedFactor, 0.18F, 0.85F)
                    * siege.getSoundProfile().movement().volume();
            float pitch = Mth.lerp(speedFactor, 0.78F, 1.08F);
            int configuredInterval = siege.getSoundProfile().movement().intervalTicks();
            int baseInterval = Math.max(1, Math.min(MOVEMENT_CLIP_INTERVAL_TICKS,
                    Math.min(configuredInterval, maximumIntervalTicks)));
            int delay = Math.max(1, Math.round(baseInterval / pitch));
            if (movementTicks >= delay || movementTicks == 0) {
                playNearby(level, siege.getMoveSound(), siege.getSoundProfile().movement().range(),
                        volume, pitch, 0.025F);
                if (movementTicks != 0) {
                    movementTicks = 0;
                }
            }
            movementTicks++;
            return;
        }

        if (movementTicks == 0 && siege.isAlive()) {
            return;
        }

        stationaryTicks++;
        if (siege.isAlive() && stationaryTicks < STOP_GRACE_TICKS) {
            return;
        }

        movementTicks = 0;
        stationaryTicks = 0;
        if (!siege.isAlive()) {
            stop(level, siege.getMoveSound());
        }
    }

    public void playNearby(ServerLevel level, SoundEvent sound, double maxDistance, float volume) {
        playInRange(level, sound, 0.0D, maxDistance, volume, 0.05F, 1.0F, 0.25F);
    }

    public void playNearby(ServerLevel level, SoundEvent sound, double maxDistance,
                           float volume, float pitch, float pitchVariation) {
        playInRange(level, sound, 0.0D, maxDistance, volume, 0.05F, pitch, pitchVariation);
    }

    public void playInRange(ServerLevel level, SoundEvent sound, double minDistance,
                            double maxDistance, float volume, float minVolume) {
        playInRange(level, sound, minDistance, maxDistance, volume, minVolume, 1.0F, 0.25F);
    }

    public void playInRange(ServerLevel level, SoundEvent sound, double minDistance,
                            double maxDistance, float baseVolume, float minVolume,
                            float basePitch, float pitchVariation) {
        if (maxDistance <= minDistance) {
            return;
        }

        level.players().forEach(player -> {
            double distance = player.position().distanceTo(siege.position());
            if (distance < minDistance || distance > maxDistance) {
                return;
            }

            double normalizedDistance = (distance - minDistance) / (maxDistance - minDistance);
            double nearField = Math.min(1.0D, FULL_VOLUME_RADIUS / Math.max(distance, FULL_VOLUME_RADIUS));
            double tail = 1.0D - normalizedDistance * normalizedDistance * normalizedDistance;
            float volume = (float) (baseVolume * nearField * tail);
            if (volume <= 0.0F) {
                return;
            }

            float pitchOffset = pitchVariation > 0.0F
                    ? random.nextFloat(-pitchVariation, pitchVariation)
                    : 0.0F;
            float pitch = Mth.clamp(basePitch + pitchOffset, 0.5F, 2.0F);
            player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                    SoundSource.BLOCKS,
                    siege.getX(), siege.getY(), siege.getZ(),
                    Math.max(volume, minVolume),
                    pitch,
                    random.nextLong()));
        });
    }

    public void stop(ServerLevel level, SoundEvent sound) {
        level.players().forEach(player -> player.connection.send(
                new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.BLOCKS)));
    }
}
