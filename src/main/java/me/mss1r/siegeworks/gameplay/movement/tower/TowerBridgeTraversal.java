package me.mss1r.siegeworks.gameplay.movement.tower;

import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class TowerBridgeTraversal {
    private static final AABB DECK = GeneratedCollisionShapes.SIEGE_TOWER_BRIDGE.parts().get(0).box();
    private static final Vec3 PIVOT = GeneratedCollisionShapes.SIEGE_TOWER_BRIDGE.pivot();
    private static final double CLEARANCE = 0.025D;
    private static final double ENTRY_REACH = 0.75D;

    private final TowerBridgeController.Host host;

    TowerBridgeTraversal(TowerBridgeController.Host host) {
        this.host = host;
    }

    static double start() {
        return PIVOT.z + 0.25D;
    }

    static double end(double angle) {
        return PIVOT.z + (DECK.maxY - PIVOT.y) * Math.sin(angle)
                + (DECK.minZ - PIVOT.z) * Math.cos(angle);
    }

    static double surfaceY(double angle, double forward) {
        return PIVOT.y + (forward - PIVOT.z) / Math.tan(angle)
                - (DECK.minZ - PIVOT.z) / Math.sin(angle);
    }

    private CollidableStructure structure() {
        return (CollidableStructure) host.vehicle();
    }

    Vec3 local(LivingEntity passenger) {
        return structure().collisionTransform().toLocal(passenger.position());
    }

    private double halfWidth(LivingEntity passenger) {
        double yaw = Math.toRadians(host.visualYaw());
        double width = passenger == null ? 0.6D : passenger.getBbWidth();
        return width * 0.5D * (Math.abs(Math.cos(yaw)) + Math.abs(Math.sin(yaw)));
    }

    Vec3 surfacePoint(LivingEntity passenger, double angle, double side, double forward) {
        Vec3 point = deckPoint(passenger, angle, side, forward);
        double radius = passenger == null ? 0.3D : passenger.getBbWidth() * 0.5D;
        double floorY = point.y;
        // Sample the footprint, not just its centre, when crossing a wall's lip.
        for (double x : new double[]{point.x - radius, point.x, point.x + radius}) {
            for (double z : new double[]{point.z - radius, point.z, point.z + radius}) {
                Vec3 floor = host.findSafeAtHeight(x, point.y, z);
                if (floor != null) {
                    floorY = Math.max(floorY, floor.y);
                }
            }
        }
        return new Vec3(point.x, floorY, point.z);
    }

    private Vec3 deckPoint(LivingEntity passenger, double angle, double side, double forward) {
        double y = surfaceY(angle, forward) + Math.abs(1.0D / Math.tan(angle)) * halfWidth(passenger) + CLEARANCE;
        return structure().collisionTransform().toWorld(new Vec3(side, y, forward));
    }

    boolean clear(LivingEntity passenger, Vec3 point) {
        AABB box = passenger == null
                ? new AABB(point.x - 0.3D, point.y, point.z - 0.3D, point.x + 0.3D, point.y + 1.95D, point.z + 0.3D)
                : passenger.getBoundingBox().move(point.subtract(passenger.position()));
        return host.level().noCollision(passenger, box)
                && !StructureCollisionResolver.intersects(structure(), structure().solidCollisionGroups(), box, 0.0D);
    }

    boolean onBridge(LivingEntity passenger, double angle) {
        Vec3 local = local(passenger);
        double margin = halfWidth(passenger);
        if (local.x < DECK.minX + margin || local.x > DECK.maxX - margin
                || local.z < start() || local.z > end(angle) - margin) {
            return false;
        }
        Vec3 surface = deckPoint(passenger, angle, local.x, local.z);
        return passenger.getY() >= surface.y - 0.1D
                && passenger.getY() <= surface.y + 0.35D && clear(passenger, surface);
    }

    boolean hasPassengers(double angle) {
        Vec3 near = deckPoint(null, angle, DECK.minX, start());
        AABB bounds = new AABB(near, near);
        for (double side : new double[]{DECK.minX, DECK.maxX}) {
            for (double forward : new double[]{start(), end(angle)}) {
                bounds = bounds.minmax(new AABB(deckPoint(null, angle, side, forward),
                        deckPoint(null, angle, side, forward)));
            }
        }
        return !host.level().getEntitiesOfClass(LivingEntity.class, bounds.inflate(0.5D, 2.0D, 0.5D),
                passenger -> passenger.isAlive() && !passenger.isPassenger() && onBridge(passenger, angle)).isEmpty();
    }

    Entry findEntry(LivingEntity passenger, double angle) {
        Entry best = null;
        double bestDistance = Double.MAX_VALUE;
        double margin = halfWidth(passenger) + 0.1D;
        for (double forward = start(); forward <= end(angle) - margin; forward += 0.5D) {
            for (int direction : new int[]{-1, 1}) {
                double outside = direction < 0 ? DECK.minX - margin : DECK.maxX + margin;
                double inside = direction < 0 ? DECK.minX + margin : DECK.maxX - margin;
                Entry candidate = entryAt(passenger, angle, inside, forward, outside, forward);
                if (candidate == null) {
                    continue;
                }
                double distance = passenger.distanceToSqr(candidate.landing());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        double tip = end(angle);
        for (double side = DECK.minX + margin; side <= DECK.maxX - margin; side += 0.5D) {
            Entry candidate = entryAt(passenger, angle, side, tip - margin, side, tip + margin);
            if (candidate != null && passenger.distanceToSqr(candidate.landing()) < bestDistance) {
                bestDistance = passenger.distanceToSqr(candidate.landing());
                best = candidate;
            }
        }
        return best;
    }

    private Entry entryAt(LivingEntity passenger, double angle, double side, double forward,
                          double outsideSide, double outsideForward) {
        Vec3 entry = surfacePoint(passenger, angle, side, forward);
        Vec3 sample = structure().collisionTransform().toWorld(new Vec3(outsideSide, entry.y - host.y(), outsideForward));
        Vec3 landing = host.findSafeAtHeight(sample.x, sample.y, sample.z);
        if (landing == null || Math.abs(landing.y - entry.y) > 1.5D
                || !clear(passenger, landing) || !clear(passenger, entry)) {
            return null;
        }
        Vec3 lift = new Vec3(landing.x, Math.max(landing.y, entry.y), landing.z);
        return clear(passenger, lift) ? new Entry(side, forward, landing, lift, entry) : null;
    }

    boolean canEnter(LivingEntity passenger, Entry entry) {
        return passenger.distanceToSqr(entry.landing()) <= ENTRY_REACH * ENTRY_REACH;
    }

    double clampedSide(LivingEntity passenger) {
        double margin = halfWidth(passenger) + 0.1D;
        return Mth.clamp(local(passenger).x, DECK.minX + margin, DECK.maxX - margin);
    }

    record Entry(double side, double forward, Vec3 landing, Vec3 lift, Vec3 deck) {
    }
}
