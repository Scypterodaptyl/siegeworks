package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.ScattershotProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntUnaryOperator;

public final class ScattershotVolley {
    private ScattershotVolley() {
    }

    public static int rollPelletCount(Random random, int loadedItems,
                                      int pelletsPerItemMin, int pelletsPerItemMax) {
        return rollPelletCount(random::nextInt, loadedItems, pelletsPerItemMin, pelletsPerItemMax);
    }

    public static int rollPelletCount(RandomSource random, int loadedItems,
                                      int pelletsPerItemMin, int pelletsPerItemMax) {
        return rollPelletCount(random::nextInt, loadedItems, pelletsPerItemMin, pelletsPerItemMax);
    }

    private static int rollPelletCount(IntUnaryOperator boundedRandom, int loadedItems,
                                       int pelletsPerItemMin, int pelletsPerItemMax) {
        int itemCount = Math.max(0, loadedItems);
        int minimum = Math.max(1, pelletsPerItemMin);
        int maximum = Math.max(minimum, pelletsPerItemMax);
        int pelletCount = 0;
        for (int item = 0; item < itemCount; item++) {
            pelletCount += minimum + boundedRandom.applyAsInt(maximum - minimum + 1);
        }
        return pelletCount;
    }

    public static List<ScattershotProjectile> spawn(ServerLevel level, LivingEntity shooter,
                                                    Vec3 origin, Vec3 velocity, int pelletCount,
                                                    float spreadDegrees, double damagePerPellet,
                                                    boolean stonePellets) {
        Vec3 forward = velocity.normalize();
        if (forward.lengthSqr() < 1.0E-8D || pelletCount <= 0) {
            return List.of();
        }

        Vec3 referenceUp = Math.abs(forward.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(referenceUp).normalize();
        Vec3 up = right.cross(forward).normalize();
        double speed = velocity.length();
        double coneRadius = Math.tan(Math.toRadians(Math.max(0.0F, spreadDegrees)));
        RandomSource random = level.random;
        List<ScattershotProjectile> pellets = new ArrayList<>(pelletCount);
        int impactReporters = Math.min(4, pelletCount);

        for (int index = 0; index < pelletCount; index++) {
            double radius = Math.sqrt(random.nextDouble()) * coneRadius;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 direction = forward
                    .add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius))
                    .normalize();
            double speedScale = 0.94D + random.nextDouble() * 0.09D;

            ScattershotProjectile pellet = new ScattershotProjectile(
                    SiegeworksEntities.SCATTERSHOT_PROJECTILE.get(), shooter, level);
            pellet.setPos(origin.x, origin.y, origin.z);
            pellet.setDeltaMovement(direction.scale(speed * speedScale));
            pellet.setBaseDamage(damagePerPellet);
            pellet.setStonePellet(stonePellets);
            pellet.setReportImpact(index < impactReporters);
            pellet.setOwner(shooter);
            level.addFreshEntity(pellet);
            pellets.add(pellet);
        }
        return List.copyOf(pellets);
    }
}
