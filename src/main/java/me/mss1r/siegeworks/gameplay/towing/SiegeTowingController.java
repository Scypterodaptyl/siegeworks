package me.mss1r.siegeworks.gameplay.towing;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SiegeTowingController {
    private static final Set<String> WARNED_ABOUT_SETTINGS = ConcurrentHashMap.newKeySet();
    /** How far from the lead animal a player may stand to take up the reins. */
    private static final double REINS_REACH = 2.0D;

    private final AbstractSiegeEntity siege;

    public SiegeTowingController(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public static boolean isDraftMount(Entity entity) {
        return entity instanceof AbstractHorse;
    }

    @Nullable
    public AbstractHorse primaryMount() {
        for (Entity passenger : siege.getPassengers()) {
            if (passenger instanceof AbstractHorse mount) {
                return mount;
            }
        }
        return null;
    }

    public List<AbstractHorse> mounts() {
        return siege.getPassengers().stream()
                .filter(AbstractHorse.class::isInstance)
                .map(AbstractHorse.class::cast)
                .toList();
    }

    public int mountSlot(Entity entity) {
        return isDraftMount(entity) ? mounts().indexOf(entity) : -1;
    }

    public boolean towed() {
        return primaryMount() != null;
    }

    public boolean towable() {
        return siege.isFullyBuilt() && siege.towingProfile() != null;
    }

    public float modelTurnDegrees() {
        TowingProfile profile = siege.towingProfile();
        return profile == null ? 0.0F : profile.modelTurnDegrees();
    }

    public Vec3 defaultPivotOffset() {
        TowingProfile profile = siege.towingProfile();
        return profile == null || !towed() ? Vec3.ZERO : profile.pivotOffset();
    }

    public Vec3 pivotWorldOffset() {
        Vec3 pivot = siege.getTowPivotOffset();
        if (pivot.lengthSqr() == 0.0D) {
            return Vec3.ZERO;
        }
        float radians = (float) Math.toRadians(siege.getVisualRotationYInDegrees());
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(cos * pivot.x + sin * pivot.z, 0.0D, sin * pivot.x - cos * pivot.z);
    }

    @Nullable
    public Vec3 mountOffset(Entity entity) {
        TowingProfile profile = siege.towingProfile();
        if (profile == null || !isDraftMount(entity)) {
            return null;
        }
        int slot = mountSlot(entity);
        if (slot >= 0 && slot < profile.mountSlots().size()) {
            return profile.mountSlots().get(slot).mountOffset();
        }
        return profile.mountOffset();
    }

    public boolean canAddDraftMount(Entity entity) {
        if (!isDraftMount(entity)) {
            return false;
        }
        TowingProfile profile = siege.towingProfile();
        return profile != null
                && siege.getPassengers().stream().allMatch(SiegeTowingController::isDraftMount)
                && mounts().size() < profile.mountSlots().size();
    }

    public boolean operatorSlotAvailable() {
        return primaryMount() == null;
    }

    /**
     * The mount whose rider holds the reins: the first one a player rides, else the first one anyone rides. Whoever
     * sits on another mount of the team just pulls along, so it does not matter which of them a player climbs on.
     */
    @Nullable
    public AbstractHorse drivingMount() {
        AbstractHorse ridden = null;
        for (AbstractHorse mount : mounts()) {
            Entity rider = mount.getFirstPassenger();
            if (rider instanceof Player) {
                return mount;
            }
            if (ridden == null && rider instanceof LivingEntity) {
                ridden = mount;
            }
        }
        return ridden;
    }

    public boolean isDrivenDraftMount(Entity passenger) {
        return passenger != null && passenger == drivingMount();
    }

    public boolean isPlayerControlledDraftMount(Entity passenger) {
        return isDrivenDraftMount(passenger) && passenger.getFirstPassenger() instanceof Player;
    }

    @Nullable
    public LivingEntity reinsHolder() {
        AbstractHorse driving = drivingMount();
        return driving != null && driving.getFirstPassenger() instanceof LivingEntity rider ? rider : null;
    }

    public InteractionResult interact(Player player, InteractionHand hand, ServerLevel level) {
        ItemStack heldStack = player.getItemInHand(hand);
        AbstractHorse primaryMount = primaryMount();

        if (heldStack.is(Items.LEAD)) {
            Optional<Mob> waitingMount = findNearestLeashedMount(player, level);
            if (waitingMount.isPresent()) {
                return attach(player, waitingMount.get());
            }
            if (primaryMount != null) {
                return detachToPlayer(player, primaryMount);
            }
            return towable() ? InteractionResult.SUCCESS : null;
        }

        if (primaryMount != null) {
            if (heldStack.is(Items.SHEARS)) {
                return detachWithDroppedLead(player, primaryMount);
            }
            // The reins are taken at the animal, not by reaching the engine from wherever one stands on it.
            if (!primaryMount.getBoundingBox().inflate(REINS_REACH).contains(player.position())) {
                return null;
            }
            player.startRiding(primaryMount);
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    public double[] dismountOffset(Entity passenger) {
        float yawRadians = (float) Math.toRadians(siege.getPassengerVisualYaw(passenger));
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);

        Vec3 offset = siege.getPassengerOffset(passenger);
        double offsetX = rightX * offset.x - forwardX * offset.z;
        double offsetZ = rightZ * offset.x - forwardZ * offset.z;
        return new double[]{offsetX, offsetZ};
    }

    public float collisionYaw(float yaw) {
        TowingProfile profile = siege.towingProfile();
        return !towed() || profile == null ? yaw : yaw + profile.modelTurnDegrees();
    }

    private Optional<Mob> findNearestLeashedMount(Player player, ServerLevel level) {
        return level.getEntitiesOfClass(Mob.class, siege.getBoundingBox().inflate(7.0),
                        mob -> canAddDraftMount(mob)
                                && mob.getLeashHolder() == player
                                && canAttach(mob)
                                && mob.distanceToSqr(player) <= 100.0)
                .stream()
                .min(Comparator.comparingDouble(siege::distanceToSqr));
    }

    private InteractionResult attach(Player player, Mob mob) {
        mob.dropLeash(true, false);
        mob.stopRiding();
        if (!mob.startRiding(siege, true)) {
            mob.setLeashedTo(player, true);
            return InteractionResult.FAIL;
        }

        warnAboutUnusableSettings(mob);
        siege.stopAnimation("set_up");
        siege.triggerAnimation("pick_up");
        player.playNotifySound(SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private void warnAboutUnusableSettings(Entity mount) {
        double speed = siege.getVelocity(mount);
        double turn = siege.getSteeringSpeedDegrees(mount);
        if (speed > 0.0D && turn > 0.0D) {
            return;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(siege.getType()).getPath();
        if (WARNED_ABOUT_SETTINGS.add(id)) {
            Siegeworks.LOG.warn("{} can be hitched but will not be drawn: horseSpeed={} and"
                            + " horseTurnDegreesPerTick={} in the server config. A zero in either stops it.",
                    id, speed, turn);
        }
    }

    private InteractionResult detachToPlayer(Player player, Mob mob) {
        moveOffSiege(mob);
        mob.setLeashedTo(player, true);
        player.playNotifySound(SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult detachWithDroppedLead(Player player, Mob mob) {
        double[] offset = moveOffSiege(mob);
        ItemEntity leadEntity = new ItemEntity(siege.level(),
                siege.getX() + offset[0], siege.getY() + 0.5D, siege.getZ() + offset[1],
                new ItemStack(Items.LEAD));
        siege.level().addFreshEntity(leadEntity);
        player.playNotifySound(SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private double[] moveOffSiege(Mob mob) {
        mob.stopRiding();
        double[] offset = dismountOffset(mob);
        mob.setPos(siege.getX() + offset[0], siege.getY() + 0.5D, siege.getZ() + offset[1]);
        return offset;
    }

    private static boolean canAttach(Mob mob) {
        if (mob instanceof TamableAnimal tameable && !tameable.isTame()) {
            return false;
        }
        return !(mob instanceof Saddleable saddleable) || saddleable.isSaddled();
    }
}
