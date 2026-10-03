package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.BlockMaterialProfile;
import me.mss1r.siegeworks.data.profile.BlockMaterialProfiles;
import me.mss1r.siegeworks.gameplay.damage.StructuralDamageSystem;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Projectile penetration, craters and accumulated cracks. Distances are in metres and energy in joules. */
public final class ProjectileImpacts {
    public static final TagKey<Block> PROJECTILE_PROOF = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "projectile_proof"));

    /** Normalizes vanilla blast resistance and hardness against stone. */
    private static final double STONE_RESISTANCE = 6.0D;
    private static final double RESISTANCE_OFFSET = 0.3D;
    private static final double STONE_HARDNESS = 1.5D;
    private static final double HARDNESS_EXPONENT = 0.3D;
    private static final double RESISTANCE_CAP = 1200.0D;
    /** Reference stone strength (Pa) and Poncelet drag (kg/m³). */
    private static final double STONE_STRENGTH = 16_700_000.0D;
    private static final double STONE_DRAG = 625.0D;
    /** Light a block of solid matter stops; one that lets light through is that much the less matter. */
    private static final double OPAQUE_LIGHT = 15.0D;
    /** Porous strength scales with the solid fraction to this power; drag scales linearly. */
    private static final double POROUS_STRENGTH_EXPONENT = 1.5D;
    /** Share of impact energy reserved for cracks around the crater. */
    private static final double RING_SHARE = 1.0D / 3.0D;
    /** Converts Poncelet drag to estimated density for the debris lifting cost. */
    private static final double DENSITY_PER_DRAG = 2600.0D / 625.0D;
    private static final double GRAVITY = 9.81D;
    /** Spall cone dimensions as multiples of projectile diameter. */
    private static final double SPALL_RADIUS_CALIBRES = 2.5D;
    private static final double SPALL_DEPTH_CALIBRES = 1.5D;
    /** Maximum crack progress per hit; prevents the surrounding ring from breaking in one blow. */
    private static final double MAX_CRACK_PER_BLOW = 0.5D;
    /** How far past a crater's edge its cracked ring reaches, in blocks. */
    private static final double RING_WIDTH = 1.5D;
    /** The farthest a single crater reaches from where it opens, in blocks. */
    private static final double MAX_CRATER_REACH = 12.0D;
    /** How ragged a crater's edge comes out, in blocks: no material breaks along a ruled line. */
    private static final double RAGGEDNESS = 0.7D;
    /** Share of the energy taken out of a projectile that goes into breaking the material, not into heat. */
    private static final double FRACTURE_SHARE = 0.75D;
    /** Reference speed in m/s for momentum-based fracture scaling. Independent of engine launch speed. */
    private static final double REFERENCE_IMPACT_SPEED = 400.0D;
    /** Shattering loss at equal projectile and block hardness. */
    private static final double SHATTER_SHARE = 0.5D;
    /** Fraction of an open-air blast coupled into block fracture. */
    private static final double BLAST_SHARE = 0.15D;
    /** Minimum crack progress; smaller shares are redistributed to nearer blocks. */
    private static final double LEAST_CRACK = 0.02D;
    /** Slower than this, in metres per second, a projectile has come to rest. */
    private static final double LEAST_FLIGHT_SPEED = 5.0D;
    /** Fragments a blow throws out of its crater: some for any blow, more for each block it broke. */
    private static final int LEAST_FRAGMENTS = 16;
    private static final int FRAGMENTS_PER_BLOCK = 14;
    private static final int MOST_FRAGMENTS = 96;
    /** Of the fragments, the share thrown as a jet out of the mouth; the rest burst round it, and dust settles. */
    private static final double JET_SHARE = 0.4D;
    /** How widely fragments spread about the way out, and how fast, in blocks a tick, they leave. */
    private static final double FRAGMENT_SPREAD = 1.4D;
    private static final double FRAGMENT_SPEED = 0.35D;
    private static final double DRIVE_SAMPLE_STEP = 0.2D;
    private static final double MAX_DRIVE_DISTANCE = 64.0D;

    private ProjectileImpacts() {
    }

    /** Strength (Pa), drag (kg/m³), fracture energy (J/m³), solid volume (m³) and solid fraction. */
    public record Material(double strength, double drag, double fractureEnergy, double volume, double matter) {
        /** Energy to break the whole block, in joules. */
        public double breakEnergy() {
            return fractureEnergy * volume;
        }
    }

    /** The material of a block, or null if nothing breaks it. */
    @Nullable
    public static Material material(BlockGetter level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || state.is(PROJECTILE_PROOF)) {
            return null;
        }
        double resistance = Math.min(RESISTANCE_CAP, state.getBlock().getExplosionResistance());
        double standing = (resistance + RESISTANCE_OFFSET) / (STONE_RESISTANCE + RESISTANCE_OFFSET)
                * Math.pow(Math.max(0.1D, hardness) / STONE_HARDNESS, HARDNESS_EXPONENT);
        double matter = matter(level, pos, state);
        double root = Math.sqrt(standing);
        double crushing = Math.pow(matter, POROUS_STRENGTH_EXPONENT);
        var override = BlockMaterialProfiles.forState(state);
        return new Material(override.flatMap(BlockMaterialProfile::strength).orElse(STONE_STRENGTH * root) * crushing,
                override.flatMap(BlockMaterialProfile::drag).orElse(STONE_DRAG * root) * matter,
                override.flatMap(BlockMaterialProfile::fractureEnergy)
                        .orElse(SiegeworksServerConfig.getStoneFractureEnergy() * standing) * crushing,
                solidVolume(level, pos, state), matter);
    }

    /** Estimates the solid fraction of full collision blocks from opacity, including leaves. */
    private static double matter(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.canOcclude() || !state.isCollisionShapeFullBlock(level, pos)) {
            return 1.0D;
        }
        return Math.max(1.0D, state.getLightBlock(level, pos)) / OPAQUE_LIGHT;
    }

    private static double solidVolume(BlockGetter level, BlockPos pos, BlockState state) {
        double volume = 0.0D;
        for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
            volume += box.getXsize() * box.getYsize() * box.getZsize();
        }
        return Math.min(1.0D, volume);
    }

    /** Whether a projectile is hard enough to cut through a block at all. */
    public static boolean canCut(ProjectilePhysicsProfile physics, BlockGetter level, BlockPos pos, BlockState state) {
        return material(level, pos, state) != null
                && BlockMaterialProfiles.resistance(state) <= physics.hardness();
    }

    /**
     * The speed a projectile keeps after going {@code length} metres through a material, or zero if it stops.
     * This is Poncelet's law integrated exactly.
     */
    public static double speedThrough(Material material, double mass, double diameter, double speed, double length) {
        double area = Math.PI * diameter * diameter * 0.25D;
        double k = material.drag() * area / mass;
        if (!(material.drag() > 0.0D)) {
            double squared = speed * speed - 2.0D * material.strength() * area * length / mass;
            return squared <= 0.0D ? 0.0D : Math.sqrt(squared);
        }
        double c = material.strength() / material.drag();
        double squared = (speed * speed + c) * Math.exp(-2.0D * k * length) - c;
        return squared <= 0.0D ? 0.0D : Math.sqrt(squared);
    }

    /**
     * Drives a projectile from where it met a block for as long as it goes through, breaking every block it
     * passes: a block of the world is whole or gone, and one a shot went through is not whole.
     * What it loses beyond the channel itself bores on along its path from the face it came in by.
     *
     * @param velocity its real velocity, in metres per second
     * @return where it ended up and its real speed; {@link Drive#passedThrough()} when it came out into the open
     */
    public static Drive drive(ServerLevel level, ProjectilePhysicsProfile physics, double diameter,
                              Vec3 entry, BlockPos firstBlock, Vec3 velocity, @Nullable Player breaker) {
        double speed = velocity.length();
        if (speed < 1.0E-6D) {
            return new Drive(false, entry, 0.0D, firstBlock, entry, faceNormal(firstBlock, entry));
        }
        Vec3 direction = velocity.scale(1.0D / speed);
        Vec3 exit = entry;
        Vec3 mouth = null;
        Vec3 mouthFace = null;
        double mouthMatter = 0.0D;
        double spare = 0.0D;
        BlockPos previous = null;
        int openSamples = 0;
        Drive result = null;

        for (double distance = 0.02D; distance <= MAX_DRIVE_DISTANCE; distance += DRIVE_SAMPLE_STEP) {
            Vec3 sample = entry.add(direction.scale(distance));
            BlockPos pos = previous == null ? firstBlock : BlockPos.containing(sample);
            if (pos.equals(previous)) {
                continue;
            }
            previous = pos;
            BlockState state = level.getBlockState(pos);
            double[] span = collisionSpan(level, pos, state, entry, direction);
            if (span == null) {
                if (mouth != null && ++openSamples >= 2) {
                    result = new Drive(true, exit.add(direction.scale(0.1D)), speed, pos, mouth, mouthFace);
                    break;
                }
                continue;
            }
            openSamples = 0;
            Vec3 contact = entry.add(direction.scale(span[0]));
            Material material = cracked(level, pos, state);
            double next = material == null ? 0.0D
                    : speedThrough(material, physics.mass(), diameter, speed, span[1] - span[0]);
            double cost = material == null ? 0.0D
                    : material.breakEnergy() * (1.0D - StructuralDamageSystem.progress(level, pos, state));
            if (material == null || !canCut(physics, level, pos, state) || next < LEAST_FLIGHT_SPEED
                    || !breakThrough(level, pos, state, contact, direction, breaker)) {
                boolean denser = mouth == null || material == null || material.matter() > mouthMatter;
                result = new Drive(false, contact, speed, pos, denser ? contact : mouth,
                        denser ? faceNormal(pos, contact) : mouthFace);
                break;
            }
            // In a block that is mostly gaps, most of what it lost went into thrashing them, not into breaking.
            double fracture = 0.5D * physics.mass() * (speed * speed - next * next) * craterYield(speed)
                    * FRACTURE_SHARE * material.matter() * kept(physics.hardness(), state);
            speed = next;
            // The crater opens where it went into solid matter, not into the leaves in front of it.
            if (mouth == null || material.matter() > mouthMatter) {
                mouth = contact;
                mouthFace = faceNormal(pos, contact);
                mouthMatter = material.matter();
            }
            spare += Math.max(0.0D, fracture - cost);
            exit = entry.add(direction.scale(span[1]));
        }
        if (result == null) {
            result = new Drive(mouth != null, exit, speed, previous == null ? firstBlock : previous,
                    mouth != null ? mouth : entry, mouthFace != null ? mouthFace : direction.reverse());
        }
        crush(level, result.mouth(), result.face(), direction, spare, diameter, 0.0D, physics.hardness(), breaker);
        return result;
    }

    /**
     * Where a projectile flying {@code along} stops in a face looking {@code outward}, what it still carried
     * bores on into what it struck, at {@code speed} metres per second; a share of its energy goes into fracture.
     */
    public static void stop(ServerLevel level, Vec3 center, Vec3 outward, Vec3 along,
                            ProjectilePhysicsProfile physics, double speed, double diameter,
                            @Nullable Player breaker) {
        Vec3 into = outward.lengthSqr() > 1.0E-8D ? outward.normalize().scale(-0.5D) : Vec3.ZERO;
        BlockPos struckPos = BlockPos.containing(center.add(into));
        BlockState struck = level.getBlockState(struckPos);
        Material material = struck.isAir() ? null : cracked(level, struckPos, struck);
        double channel = material == null ? 0.0D : penetration(material, physics.mass(), diameter, speed);
        double energy = physics.kineticEnergy(speed) * craterYield(speed);
        crush(level, center, outward, along, energy * FRACTURE_SHARE * kept(physics.hardness(), struck), diameter,
                channel, physics.hardness(), breaker);
    }

    /**
     * The material of a block as cracked as it is: cracked through, it bears a blow that much the less, though it
     * weighs the same and is still in the way.
     */
    @Nullable
    private static Material cracked(ServerLevel level, BlockPos pos, BlockState state) {
        Material material = material(level, pos, state);
        if (material == null) {
            return null;
        }
        double whole = 1.0D - StructuralDamageSystem.progress(level, pos, state);
        return new Material(material.strength() * whole, material.drag(), material.fractureEnergy(),
                material.volume(), material.matter());
    }

    /** How deep, in metres, a projectile at {@code speed} goes into {@code material} before it stops. */
    private static double penetration(Material material, double mass, double diameter, double speed) {
        if (!(material.drag() > 0.0D) || !(speed > 0.0D)) {
            return 0.0D;
        }
        double area = Math.PI * diameter * diameter / 4.0D;
        double k = material.drag() * area / mass;
        double c = material.strength() / material.drag();
        return Math.log1p(speed * speed / c) / (2.0D * k);
    }

    /**
     * How much a joule of a blow at {@code speed} breaks against one at the reference speed: what it breaks goes
     * with its momentum, mass times speed, so a joule of it goes the further the slower it comes.
     */
    private static double craterYield(double speed) {
        return speed > 0.0D ? REFERENCE_IMPACT_SPEED / speed : 1.0D;
    }

    /**
     * The share of a blow left to the struck block once a projectile of {@code hardness} has spent its part on
     * breaking up against it: all of it on a block half as hard or softer, half of it on one as hard.
     */
    private static double kept(double hardness, BlockState struck) {
        if (!(hardness > 0.0D) || Double.isInfinite(hardness)) {
            return 1.0D;
        }
        double likeness = BlockMaterialProfiles.resistance(struck) / hardness;
        return 1.0D - SHATTER_SHARE * Mth.clamp(2.0D * likeness - 1.0D, 0.0D, 1.0D);
    }

    /** A charge of {@code energy} joules going off at {@code center}, breaking whatever is around it. */
    public static void blast(ServerLevel level, Vec3 center, Vec3 outward, double energy, @Nullable Player breaker) {
        crush(level, center, outward, null, energy * BLAST_SHARE, 0.0D, 0.0D, Double.POSITIVE_INFINITY, breaker);
    }

    /**
     * Spends energy on a penetration channel or blast crater, then cracks the surrounding blocks.
         * Protected blocks absorb energy without breaking.
         *
         * @param outward outward crater direction
         * @param along flight direction, or null for an explosive charge
         * @param channel penetration depth in metres
     */
    public static void crush(ServerLevel level, Vec3 center, Vec3 outward, @Nullable Vec3 along, double energy,
                             double diameter, double channel, double hardness, @Nullable Player breaker) {
        if (!(energy > 0.0D)) {
            return;
        }
        BlockPos centerPos = BlockPos.containing(center);
        double fracture = nearestFracture(level, centerPos, hardness);
        if (!(fracture > 0.0D)) {
            return;
        }
        Vec3 axis = along != null && along.lengthSqr() > 1.0E-8D ? along.normalize() : null;
        double upward = outward.lengthSqr() > 1.0E-8D ? Math.max(0.0D, outward.normalize().y) : 0.0D;
        double volume = energy / fracture;
        Vec3 into = outward.lengthSqr() > 1.0E-8D ? outward.normalize().reverse() : axis;
        double spallRadius = SPALL_RADIUS_CALIBRES * diameter;
        double spallDepth = SPALL_DEPTH_CALIBRES * diameter;
        double channelWidth = Math.max(0.5D, diameter * 0.5D);
        // Far enough to hold the channel and the spalled cone, or the half-sphere of a charge, this energy could
        // break out of that material, and the ring cracked round either.
        double radius = Math.min(MAX_CRATER_REACH, 1.0D + RAGGEDNESS + RING_WIDTH + (axis != null
                ? Math.max(channel, spallRadius)
                : diameter * 0.5D + Math.cbrt(3.0D * volume / (2.0D * Math.PI))));
        BlockPos corePos = BlockPos.containing(center);
        int reach = Mth.ceil(radius) + 1;
        List<Target> targets = new ArrayList<>();
        List<Target> around = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(corePos.offset(-reach, -reach, -reach),
                corePos.offset(reach, reach, reach))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || BlockMaterialProfiles.resistance(state) > hardness) {
                continue;
            }
            Material material = material(level, pos, state);
            Vec3 offset = Vec3.atCenterOf(pos).subtract(center);
            double distance = offset.length();
            boolean inCrater = axis == null
                    || inChannel(offset, axis, channel, channelWidth)
                    || level.random.nextDouble() < spallShare(offset, into, spallRadius, spallDepth);
            distance += (level.random.nextDouble() - 0.5D) * RAGGEDNESS;
            if (material != null && distance <= radius) {
                double climb = Math.max(0.0D, -offset.y) * upward;
                double lift = material.drag() * DENSITY_PER_DRAG * material.volume() * GRAVITY * climb;
                (inCrater ? targets : around).add(new Target(pos.immutable(), material.breakEnergy(), lift, distance,
                        SiegeBlockBreaker.mayDamage(level, pos, state, breaker)));
            }
        }
        targets.sort(Comparator.comparingDouble(Target::distance));
        BlockState struck = targets.isEmpty() ? null : level.getBlockState(targets.get(0).pos());
        float debrisPower = (float) Math.max(1.0D, radius * 2.0D);
        double budget = energy * (1.0D - RING_SHARE);
        List<Vec3> broken = new ArrayList<>();
        double deepest = 0.0D;
        int standing = 0;
        for (; standing < targets.size(); standing++) {
            Target target = targets.get(standing);
            double cost = target.breakEnergy()
                    * (1.0D - StructuralDamageSystem.progress(level, target.pos(), level.getBlockState(target.pos())))
                    + target.lift();
            if (budget < cost) {
                break;
            }
            // A protected block still stands in the way and takes its share.
            budget -= cost;
            if (target.mayBreak()) {
                breakOut(level, target.pos(), center, outward, debrisPower, breaker);
                Vec3 at = Vec3.atCenterOf(target.pos());
                broken.add(at);
                if (axis != null) {
                    deepest = Math.max(deepest, at.subtract(center).dot(axis));
                }
            }
        }
        // What was too little to break the next block out goes into that block, the one it stopped at.
        double left = budget;
        if (standing < targets.size() && budget > 0.0D) {
            Target stoppedAt = targets.get(standing);
            if (stoppedAt.mayBreak() && stoppedAt.breakEnergy() > 0.0D) {
                double whole = stoppedAt.breakEnergy() + stoppedAt.lift();
                double crack = Math.min(MAX_CRACK_PER_BLOW, budget / whole);
                left = budget - crack * whole;
                if (StructuralDamageSystem.applyImpact(level, stoppedAt.pos(), (float) crack, 1.0F)
                        == StructuralDamageSystem.ImpactResult.BREAK_BLOCK) {
                    breakOut(level, stoppedAt.pos(), center, outward, debrisPower, breaker);
                }
            }
        }
        // The channel runs from the mouth as deep as the crater went.
        Vec3 boreEnd = axis != null ? center.add(axis.scale(deepest)) : center;
        List<Target> ring = new ArrayList<>(targets.subList(Math.min(targets.size(), standing + 1), targets.size()));
        ring.addAll(around);
        crackRing(level, ring, left + energy * RING_SHARE, center, boreEnd, broken, outward, debrisPower, breaker);
        if (struck != null && !struck.isAir()) {
            throwFragments(level, center, outward, struck, broken.size(), energy / fracture);
        }
    }

    /** Emits block fragments and dust, directed out of the crater mouth. */
    private static void throwFragments(ServerLevel level, Vec3 center, Vec3 outward, BlockState struck, int broke,
                                       double blocksWorth) {
        Vec3 out = outward.lengthSqr() > 1.0E-8D ? outward.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
        int count = Mth.clamp((int) Math.round(LEAST_FRAGMENTS + FRAGMENTS_PER_BLOCK * (broke + Math.min(1.0D,
                blocksWorth))), LEAST_FRAGMENTS, MOST_FRAGMENTS);
        BlockParticleOption fragment = new BlockParticleOption(SiegeworksParticles.FRAGMENT.get(), struck);
        RandomSource random = level.random;
        // How far the burst and the dust reach grows with the blow.
        double reach = 0.4D + 0.25D * Math.sqrt(count);
        Vec3 mouth = center.add(out.scale(0.3D));
        level.sendParticles(fragment, mouth.x, mouth.y, mouth.z, count, reach * 0.5D, reach * 0.5D, reach * 0.5D,
                FRAGMENT_SPEED * 0.6D);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, struck), mouth.x,
                mouth.y + reach * 0.3D, mouth.z, count / 2, reach, reach * 0.6D, reach, 0.0D);
        int jet = (int) Math.round(count * JET_SHARE);
        for (int i = 0; i < jet; i++) {
            Vec3 way = out.add((random.nextDouble() - 0.5D) * FRAGMENT_SPREAD, (random.nextDouble() - 0.5D)
                    * FRAGMENT_SPREAD, (random.nextDouble() - 0.5D) * FRAGMENT_SPREAD).normalize();
            Vec3 from = center.add(out.scale(0.15D));
            double speed = FRAGMENT_SPEED * (0.5D + random.nextDouble());
            level.sendParticles(fragment, from.x, from.y, from.z, 0, way.x, way.y, way.z, speed);
        }
    }

    /** Whether a block at {@code offset} from where a shot went in lies in the channel it drove. */
    private static boolean inChannel(Vec3 offset, Vec3 axis, double channel, double width) {
        double along = offset.dot(axis);
        double aside = Math.sqrt(Math.max(0.0D, offset.lengthSqr() - along * along));
        return along >= -0.5D && along <= channel && aside <= width;
    }

    /** Fraction of a block inside the spall cone; used as its break-out probability. */
    private static double spallShare(Vec3 offset, Vec3 into, double radius, double depth) {
        double down = offset.dot(into);
        double across = Math.sqrt(Math.max(0.0D, offset.lengthSqr() - down * down));
        if (down <= 0.5D && across <= 0.5D) {
            return 1.0D;
        }
        // A block lies as deep as its near face, half a block short of its centre.
        double reached = Math.max(0.0D, down - 0.5D);
        if (down < -0.5D || reached > depth) {
            return 0.0D;
        }
        double reach = radius * (1.0D - reached / Math.max(depth, 1.0E-6D));
        return Mth.clamp(reach + 0.5D - across, 0.0D, 1.0D);
    }

    /** Distributes remaining energy as cracks, weighted toward the channel and broken blocks. */
    private static void crackRing(ServerLevel level, List<Target> ring, double energy, Vec3 from, Vec3 to,
                                  List<Vec3> broke, Vec3 outward, float debrisPower, @Nullable Player breaker) {
        List<Target> cracked = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Target target : ring) {
            Vec3 at = Vec3.atCenterOf(target.pos());
            double gap = Math.max(0.0D, distanceToSegment(at, from, to) - 0.5D);
            for (Vec3 gone : broke) {
                gap = Math.min(gap, Math.max(0.0D, at.distanceTo(gone) - 1.0D));
            }
            double reach = 1.0D - gap / RING_WIDTH;
            if (reach > 0.0D && target.mayBreak() && target.breakEnergy() > 0.0D) {
                cracked.add(target);
                weights.add(reach * reach);
            }
        }
        if (cracked.isEmpty()) {
            return;
        }
        // Nearest first; keep as many as the energy can crack by at least the least crack each.
        Integer[] order = new Integer[cracked.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(weights.get(b), weights.get(a)));
        int kept = 1;
        double keptWeight = weights.get(order[0]);
        for (int i = 1; i < order.length; i++) {
            double weight = weights.get(order[i]);
            if (energy * weight / (keptWeight + weight) / cracked.get(order[i]).breakEnergy() < LEAST_CRACK) {
                break;
            }
            kept++;
            keptWeight += weight;
        }
        for (int i = 0; i < kept; i++) {
            Target target = cracked.get(order[i]);
            double crack = Math.min(MAX_CRACK_PER_BLOW, energy * weights.get(order[i]) / keptWeight / target.breakEnergy());
            if (StructuralDamageSystem.applyImpact(level, target.pos(), (float) crack, 1.0F)
                    == StructuralDamageSystem.ImpactResult.BREAK_BLOCK) {
                breakOut(level, target.pos(), from, outward, debrisPower, breaker);
            }
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 span = to.subtract(from);
        double length = span.lengthSqr();
        double share = length > 1.0E-8D ? Mth.clamp(point.subtract(from).dot(span) / length, 0.0D, 1.0D) : 0.0D;
        return point.distanceTo(from.add(span.scale(share)));
    }

    private static void breakOut(ServerLevel level, BlockPos pos, Vec3 center, Vec3 outward, float debrisPower,
                                 @Nullable Player breaker) {
        StructuralDamageSystem.applyImpact(level, pos, 1.0F, 1.0F);
        if (!level.getBlockState(pos).isAir()
                && !ExplosionPhysics.launchDestroyedBlock(level, pos, center, outward, debrisPower, breaker)) {
            SiegeBlockBreaker.breakBlock(level, pos, breaker);
        }
    }

    /** Breaks a block a projectile goes through, throwing it back out of the hole when it can get out. */
    private static boolean breakThrough(ServerLevel level, BlockPos pos, BlockState state, Vec3 contact,
                                       Vec3 direction, @Nullable Player breaker) {
        if (!SiegeBlockBreaker.mayDamage(level, pos, state, breaker)) {
            return false;
        }
        StructuralDamageSystem.applyImpact(level, pos, 1.0F, 1.0F);
        return ExplosionPhysics.launchDestroyedBlock(level, pos, contact, direction.reverse(), 2.0F, breaker)
                || SiegeBlockBreaker.breakBlock(level, pos, breaker);
    }

    /** The fracture energy of the solid block nearest {@code center}, which sets how far a crater reaches. */
    private static double nearestFracture(ServerLevel level, BlockPos center, double hardness) {
        double best = -1.0D;
        int bestDistance = Integer.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || BlockMaterialProfiles.resistance(state) > hardness) {
                continue;
            }
            Material material = material(level, pos, state);
            int distance = pos.distManhattan(center);
            if (material != null && distance < bestDistance) {
                bestDistance = distance;
                best = material.fractureEnergy();
            }
        }
        return best;
    }

    /** Sets fire around where an incendiary stopped. */
    public static void ignite(ServerLevel level, Vec3 center, ProjectilePhysicsProfile.Fire fire,
                               @Nullable Player breaker) {
        if (fire.radius() <= 0.0D || fire.chance() <= 0.0D) {
            return;
        }
        RandomSource random = level.random;
        int reach = Mth.ceil(fire.radius());
        BlockPos centerPos = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.withinManhattan(centerPos, reach, reach, reach)) {
            double distance = Math.sqrt(pos.distSqr(centerPos));
            if (distance > fire.radius() || !level.getBlockState(pos).isAir()
                    || !Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                continue;
            }
            double chance = fire.chance() * (1.0D - 0.75D * distance / fire.radius());
            if (random.nextDouble() < chance) {
                SiegeBlockBreaker.placeFire(level, pos, breaker);
            }
        }
    }

    /** Where a ray from {@code origin} enters and leaves a block's collision shape, or null if it misses. */
    @Nullable
    private static double[] collisionSpan(BlockGetter level, BlockPos pos, BlockState state,
                                          Vec3 origin, Vec3 direction) {
        if (state.isAir()) {
            return null;
        }
        double enter = Double.POSITIVE_INFINITY;
        double exit = Double.NEGATIVE_INFINITY;
        for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
            double[] span = raySpan(box.move(pos), origin, direction);
            if (span != null) {
                enter = Math.min(enter, span[0]);
                exit = Math.max(exit, span[1]);
            }
        }
        return exit > enter ? new double[]{Math.max(0.0D, enter), exit} : null;
    }

    @Nullable
    private static double[] raySpan(AABB box, Vec3 origin, Vec3 direction) {
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        double[] start = {origin.x, origin.y, origin.z};
        double[] step = {direction.x, direction.y, direction.z};
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(step[axis]) < 1.0E-9D) {
                if (start[axis] < min[axis] || start[axis] > max[axis]) {
                    return null;
                }
                continue;
            }
            double first = (min[axis] - start[axis]) / step[axis];
            double second = (max[axis] - start[axis]) / step[axis];
            enter = Math.max(enter, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
        }
        return exit < Math.max(0.0D, enter) ? null : new double[]{enter, exit};
    }

    /** Position and speed after penetration, with the last contact block and outward crater face. */
    public record Drive(boolean passedThrough, Vec3 position, double speed, BlockPos block, Vec3 mouth, Vec3 face) {
    }

    /** The outward normal of the face of the block at {@code pos} that {@code contact} lies on. */
    private static Vec3 faceNormal(BlockPos pos, Vec3 contact) {
        Vec3 offset = contact.subtract(Vec3.atCenterOf(pos));
        double x = Math.abs(offset.x);
        double y = Math.abs(offset.y);
        double z = Math.abs(offset.z);
        if (x >= y && x >= z) {
            return new Vec3(Math.signum(offset.x), 0.0D, 0.0D);
        }
        return y >= z ? new Vec3(0.0D, Math.signum(offset.y), 0.0D) : new Vec3(0.0D, 0.0D, Math.signum(offset.z));
    }

    private record Target(BlockPos pos, double breakEnergy, double lift, double distance, boolean mayBreak) {
    }
}
