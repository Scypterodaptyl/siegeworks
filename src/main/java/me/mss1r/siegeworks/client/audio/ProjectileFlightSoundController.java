package me.mss1r.siegeworks.client.audio;

import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.entity.projectile.GiantCannonProjectile;
import me.mss1r.siegeworks.entity.projectile.ScattershotProjectile;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class ProjectileFlightSoundController {
    private static final double MIN_FLIGHT_SPEED_SQR = 0.03D * 0.03D;
    private static final int FADE_OUT_TICKS = 10;
    private static final Map<Entity, ProjectileFlightSoundInstance> ACTIVE = new IdentityHashMap<>();

    private static ClientLevel trackedLevel;

    private ProjectileFlightSoundController() {
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            stopAll();
            trackedLevel = level;
        }
        if (level == null) {
            return;
        }

        Iterator<Map.Entry<Entity, ProjectileFlightSoundInstance>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, ProjectileFlightSoundInstance> entry = iterator.next();
            if (entry.getValue().isStopped() && entry.getKey().isRemoved()) {
                iterator.remove();
            }
        }

        for (Entity entity : level.entitiesForRendering()) {
            FlightSoundProfile profile = profileFor(entity);
            if (profile == null || ACTIVE.containsKey(entity)
                    || entity.tickCount < 1 || entity.getDeltaMovement().lengthSqr() < MIN_FLIGHT_SPEED_SQR) {
                continue;
            }

            ProjectileFlightSoundInstance sound = new ProjectileFlightSoundInstance(entity, profile);
            ACTIVE.put(entity, sound);
            minecraft.getSoundManager().play(sound);
        }
    }

    private static FlightSoundProfile profileFor(Entity entity) {
        if (entity instanceof TrebuchetProjectile) {
            return new FlightSoundProfile(0.95F, 0.95F);
        }
        if (entity instanceof GiantCannonProjectile) {
            return new FlightSoundProfile(1.35F, 0.62F);
        }
        if (entity instanceof CannonProjectile && !(entity instanceof ScattershotProjectile)) {
            return new FlightSoundProfile(1.05F, 0.78F);
        }
        return null;
    }

    private static void stopAll() {
        ACTIVE.values().forEach(ProjectileFlightSoundInstance::stopNow);
        ACTIVE.clear();
    }

    private record FlightSoundProfile(float volume, float pitch) {
    }

    private static final class ProjectileFlightSoundInstance extends AbstractTickableSoundInstance {
        private final Entity projectile;
        private final float fullVolume;
        private int stillTicks;
        private int fadeTicks = FADE_OUT_TICKS;

        private ProjectileFlightSoundInstance(Entity projectile, FlightSoundProfile profile) {
            super(SiegeworksSounds.PROJECTILE_FLIGHT.get(), SoundSource.PLAYERS, RandomSource.create());
            this.projectile = projectile;
            this.fullVolume = profile.volume();
            this.volume = fullVolume;
            this.pitch = profile.pitch();
            this.looping = true;
            this.delay = 0;
            updatePosition();
        }

        @Override
        public boolean canPlaySound() {
            return !projectile.isSilent();
        }

        @Override
        public void tick() {
            if (projectile.isRemoved()) {
                fadeOut();
                return;
            }

            updatePosition();
            if (projectile.tickCount > 2 && projectile.getDeltaMovement().y <= 0.0D) {
                fadeOut();
                return;
            }

            if (projectile.getDeltaMovement().lengthSqr() < MIN_FLIGHT_SPEED_SQR) {
                if (++stillTicks >= 3) {
                    fadeOut();
                    return;
                }
            } else {
                stillTicks = 0;
            }
        }

        private void fadeOut() {
            fadeTicks--;
            if (fadeTicks <= 0) {
                volume = 0.0F;
                stop();
                return;
            }
            volume = fullVolume * fadeTicks / FADE_OUT_TICKS;
        }

        private void updatePosition() {
            x = projectile.getX();
            y = projectile.getY();
            z = projectile.getZ();
        }

        private void stopNow() {
            stop();
        }
    }
}
