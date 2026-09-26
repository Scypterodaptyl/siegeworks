package me.mss1r.siegeworks.gameplay.ballistics;

import net.minecraft.world.level.Explosion;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.gameplay.damage.StructuralDamageSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ProjectileBlockImpact {
    private ProjectileBlockImpact() {
    }

    public static boolean damageImpactBlocks(SiegeProjectile projectile, BlockHitResult hit,
                                             ProjectilePhysicsProfile physics, float damageScale) {
        if (!(projectile.level() instanceof ServerLevel serverLevel)
                || !(projectile.getOwner() instanceof AbstractSiegeEntity)) {
            return false;
        }

        double radius = impactBlockDamageRadius(projectile, physics);
        float centerDamage = impactBlockDamage(projectile, physics) * damageScale;
        damageBlocksInSphere(serverLevel, hit.getLocation(), radius, centerDamage,
                SiegeBlockBreaker.responsiblePlayer(projectile.getOwner()));
        return true;
    }

    public static double impactBlockDamageRadius(SiegeProjectile projectile, ProjectilePhysicsProfile physics) {
        double radius = 0.25D
                + Math.sqrt(Math.max(1.0D, projectile.getBaseDamage())) * 0.10D
                + Math.sqrt(Math.max(1.0D, physics.mass())) * 0.15D
                + Math.max(0.0D, physics.blockDamageMultiplier()) * 0.20D;
        return Mth.clamp(radius, 0.9D, 4.25D);
    }

    public static float impactBlockDamage(SiegeProjectile projectile, ProjectilePhysicsProfile physics) {
        return (float) Math.max(0.85, projectile.getBaseDamage() / 24.0)
                * (float) Math.max(0.0, physics.blockDamageMultiplier());
    }

    public static void damageBlocksInSphere(ServerLevel serverLevel, Vec3 center,
                                            double radius, float centerDamage,
                                            @Nullable Player breaker) {
        int blockRadius = Math.max(1, (int) Math.ceil(radius));
        Explosion probe = SiegeBlockBreaker.damageProbe(serverLevel, center, breaker);
        BlockPos centerPos = BlockPos.containing(center);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        impact:
        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int y = -blockRadius; y <= blockRadius; y++) {
                for (int z = -blockRadius; z <= blockRadius; z++) {
                    mutablePos.set(centerPos.getX() + x, centerPos.getY() + y, centerPos.getZ() + z);
                    BlockPos pos = mutablePos.immutable();
                    double distance = Vec3.atCenterOf(pos).distanceTo(center);
                    double edgeDistance = Math.max(0.0, distance - 0.5);
                    if (edgeDistance > radius) {
                        continue;
                    }

                    BlockState state = serverLevel.getBlockState(pos);
                    float hardness = SiegeBlockBreaker.siegeResistance(serverLevel, pos, state, probe);
                    if (state.isAir() || hardness < 0.0F) {
                        continue;
                    }

                    double falloff = 1.0D - edgeDistance / radius;
                    float damage = (float) (centerDamage * Math.pow(falloff, 1.35D));
                    if (damage <= 0.025F) {
                        continue;
                    }

                    if (StructuralDamageSystem.applyImpact(serverLevel, pos, damage, hardness)
                            == StructuralDamageSystem.ImpactResult.BREAK_BLOCK) {
                        boolean launched = ExplosionPhysics.launchDestroyedBlock(
                                serverLevel, pos, center, Math.max(1.0F, centerDamage));
                        if (!launched && !SiegeBlockBreaker.breakBlock(serverLevel, pos, breaker)) {
                            break impact;
                        }
                    }
                }
            }
        }
    }

    public static void carvePenetrationTunnel(ServerLevel serverLevel, BlockPos corePos, Vec3 direction,
                                              ProjectilePhysicsProfile physics, float baseDamage, double speed,
                                              @Nullable Player breaker) {
        Vec3 normalizedDirection = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        Vec3 center = Vec3.atCenterOf(corePos);
        double radius = penetrationTunnelRadius(physics, baseDamage);
        float centerDamage = penetrationTunnelDamage(physics, baseDamage, speed);
        int blockRadius = Math.max(1, (int) Math.ceil(radius + 0.5D));
        Explosion probe = SiegeBlockBreaker.damageProbe(serverLevel, center, breaker);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        impact:
        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int y = -blockRadius; y <= blockRadius; y++) {
                for (int z = -blockRadius; z <= blockRadius; z++) {
                    mutablePos.set(corePos.getX() + x, corePos.getY() + y, corePos.getZ() + z);
                    BlockPos pos = mutablePos.immutable();
                    Vec3 offset = Vec3.atCenterOf(pos).subtract(center);
                    double along = offset.dot(normalizedDirection);
                    if (Math.abs(along) > 0.95D) {
                        continue;
                    }

                    Vec3 radialOffset = offset.subtract(normalizedDirection.scale(along));
                    double edgeDistance = Math.max(0.0D, radialOffset.length() - 0.35D);
                    if (edgeDistance > radius) {
                        continue;
                    }

                    BlockState state = serverLevel.getBlockState(pos);
                    float hardness = SiegeBlockBreaker.siegeResistance(serverLevel, pos, state, probe);
                    if (state.isAir() || hardness < 0.0F) {
                        continue;
                    }

                    if (pos.equals(corePos)) {
                        if (!SiegeBlockBreaker.breakBlock(serverLevel, pos, breaker)) {
                            break impact;
                        }
                        continue;
                    }

                    double falloff = 1.0D - edgeDistance / radius;
                    float damage = (float) (centerDamage * Math.pow(falloff, 1.15D));
                    if (damage > 0.08F
                            && StructuralDamageSystem.applyImpact(serverLevel, pos, damage, hardness)
                            == StructuralDamageSystem.ImpactResult.BREAK_BLOCK
                            && !SiegeBlockBreaker.breakBlock(serverLevel, pos, breaker)) {
                        break impact;
                    }
                }
            }
        }

        ProjectileImpactEffects.spawnPenetrationParticles(
                serverLevel, center, radius, normalizedDirection.reverse());
    }

    private static double penetrationTunnelRadius(ProjectilePhysicsProfile physics, float baseDamage) {
        double radius = 0.25D
                + Math.sqrt(Math.max(1.0D, physics.mass())) * 0.045D
                + Math.max(0.0D, physics.blockDamageMultiplier()) * 0.14D
                + Math.sqrt(Math.max(1.0F, baseDamage)) * 0.025D;
        return Mth.clamp(radius, 0.65D, 2.25D);
    }

    private static float penetrationTunnelDamage(ProjectilePhysicsProfile physics,
                                                 float baseDamage, double speed) {
        double speedScale = Mth.clamp(
                0.78D + Math.log1p(Math.max(0.0D, speed)) * 0.24D, 0.75D, 1.55D);
        return (float) (Math.max(1.15F, baseDamage / 20.0F)
                * Math.max(0.7D, physics.blockDamageMultiplier())
                * speedScale);
    }
}
