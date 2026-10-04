package me.mss1r.siegeworks.gameplay.ladder;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import java.util.UUID;
*///?} else {
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//?}

import java.util.HashMap;
import java.util.Map;

/**
 * A player carrying a siege ladder in both hands. The ladder stays itself, an entity, and follows its carrier: a
 * short one level over the head, held at its middle, a long one held two thirds up with its foot dragging behind.
 * Carrying it takes the hands and the run out of the carrier; it is taken up at its foot and set down in front.
 */
public final class LadderCarry {
    private static final String TAG_CARRIED = "SiegeworksCarriedLadder";
    /** Where the raised hands hold a ladder: this high over the feet and this far before the body. */
    private static final double HANDS_HEIGHT = 1.8D;
    private static final double HANDS_FORWARD = 0.25D;
    /** A ladder this many sections long or shorter goes level over the head; a longer one drags its foot. */
    public static final int OVERHEAD_SECTIONS = 2;
    /** How far up a long ladder is held, as a share of its length from its foot. */
    private static final double GRIP_SHARE = 2.0D / 3.0D;
    /** How near its foot a ladder has to be taken up. */
    private static final double FOOT_REACH = 2.0D;
    private static final double FOOT_REACH_UP = 2.0D;
    /** How far in front of its carrier a ladder is set down. */
    private static final double PUT_DOWN_REACH = 1.0D;
    private static final double LADDER_HALF_WIDTH = 0.45D;
    /** How much each part of a ladder, its base and every section, slows whoever carries it. */
    private static final double SLOWDOWN_PER_PART = 0.06D;
    //? if forge {
    /*private static final UUID SLOWDOWN_ID = UUID.fromString("5d0f4c8e-6c47-4b0b-9a3f-2f7f0a9f1c21");
    *///?} else {
    private static final ResourceLocation SLOWDOWN_ID = MinecraftVersionCompat.id("siegeworks", "carrying_ladder");
    //?}

    private static final Map<java.util.UUID, SiegeLadderEntity> CARRIED = new HashMap<>();
    private static final Map<Integer, SiegeLadderEntity> SEEN_CARRIED = new HashMap<>();

    private LadderCarry() {
    }

    /** Where a carried ladder is: its foot, the way it points and how far it leans from upright, in degrees. */
    public record Pose(Vec3 foot, float yaw, double leanDegrees) {
    }

    public static Pose pose(Entity carrier, int sections, double length, double levelDegrees) {
        float yaw = carrier instanceof LivingEntity living ? living.yBodyRot : carrier.getYRot();
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 hands = carrier.position().add(0.0D, HANDS_HEIGHT, 0.0D).add(forward.scale(HANDS_FORWARD));
        double grip;
        double lean;
        if (sections <= OVERHEAD_SECTIONS) {
            grip = length * 0.5D;
            lean = Math.toRadians(levelDegrees);
        } else {
            grip = length * GRIP_SHARE;
            // Tilted so that the foot, below and behind the hands, rests on the ground the carrier walks on.
            lean = Math.acos(Mth.clamp(HANDS_HEIGHT / grip, 0.0D, 1.0D));
        }
        Vec3 foot = hands.subtract(forward.scale(grip * Math.sin(lean))).subtract(0.0D, grip * Math.cos(lean), 0.0D);
        return new Pose(foot, yaw, Math.toDegrees(lean));
    }

    /** Takes a ladder up into a player's hands, if the player stands at its foot with hands free for it. */
    public static boolean tryPickUp(Player player, SiegeLadderEntity ladder) {
        if (carried(player) != null) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carrying_one"), true);
            return false;
        }
        double dx = player.getX() - ladder.getX();
        double dz = player.getZ() - ladder.getZ();
        double dy = player.getY() - ladder.getY();
        if (dx * dx + dz * dz > FOOT_REACH * FOOT_REACH || dy < -1.0D || dy > FOOT_REACH_UP) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carry_from_foot"), true);
            return false;
        }
        take(player, ladder);
        player.level().playSound(null, ladder.blockPosition(), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.8F, 0.8F);
        player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carrying"), true);
        return true;
    }

    private static void take(Player player, SiegeLadderEntity ladder) {
        ladder.beginCarry(player);
        CARRIED.put(player.getUUID(), ladder);
        slow(player, ladder.getSections());
    }

    /** Sets the ladder a player carries upright in front of them, to lean on what it falls against. */
    public static void putDown(Player player) {
        SiegeLadderEntity ladder = carried(player);
        if (ladder == null) {
            return;
        }
        double yawRadians = Math.toRadians(player.getYRot());
        Vec3 foot = player.position().add(-Math.sin(yawRadians) * PUT_DOWN_REACH, 0.0D,
                Math.cos(yawRadians) * PUT_DOWN_REACH);
        AABB upright = new AABB(foot.x - LADDER_HALF_WIDTH, foot.y + 0.01D, foot.z - LADDER_HALF_WIDTH,
                foot.x + LADDER_HALF_WIDTH, foot.y + ladder.getLadderLength(), foot.z + LADDER_HALF_WIDTH);
        if (!player.level().noCollision(upright)) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carry_no_room"), true);
            return;
        }
        release(player, ladder);
        ladder.standAt(foot, player.getYRot());
        player.level().playSound(null, ladder.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Lets go of a ladder where it is, as when its carrier falls: it drops from there and topples. */
    public static void drop(SiegeLadderEntity ladder) {
        java.util.UUID carrier = ladder.carrierUuid();
        if (carrier != null && CARRIED.get(carrier) == ladder) {
            CARRIED.remove(carrier);
            if (ladder.carrier() != null) {
                unslow(ladder.carrier());
            }
        }
        ladder.endCarry();
    }

    private static void release(Player player, SiegeLadderEntity ladder) {
        CARRIED.remove(player.getUUID());
        unslow(player);
        ladder.endCarry();
    }

    /** Forgets a carried ladder that is gone. */
    public static void forget(SiegeLadderEntity ladder) {
        java.util.UUID carrier = ladder.carrierUuid();
        if (carrier != null && CARRIED.get(carrier) == ladder) {
            CARRIED.remove(carrier);
        }
    }

    /** The ladder a player carries, on the server, or null. */
    @Nullable
    public static SiegeLadderEntity carried(Player player) {
        SiegeLadderEntity ladder = CARRIED.get(player.getUUID());
        if (ladder != null && (ladder.isRemoved() || !ladder.isCarried())) {
            CARRIED.remove(player.getUUID());
            return null;
        }
        return ladder;
    }

    /** Whether an entity carries a ladder, as either side knows it. */
    public static boolean isCarrying(Entity entity) {
        if (!entity.level().isClientSide) {
            return entity instanceof Player player && carried(player) != null;
        }
        SiegeLadderEntity ladder = SEEN_CARRIED.get(entity.getId());
        return ladder != null && !ladder.isRemoved() && ladder.carrierId() == entity.getId();
    }

    /** Notes on the client whom a ladder is carried by, as it learns so. */
    public static void seen(SiegeLadderEntity ladder) {
        SEEN_CARRIED.values().removeIf(known -> known == ladder || known.isRemoved());
        if (ladder.isCarried() && !ladder.isRemoved()) {
            SEEN_CARRIED.put(ladder.carrierId(), ladder);
        }
    }

    /** A player leaving takes the ladder along, kept with them until they come back. */
    public static void stash(Player player) {
        SiegeLadderEntity ladder = carried(player);
        if (ladder == null) {
            return;
        }
        release(player, ladder);
        CompoundTag tag = new CompoundTag();
        ladder.saveWithoutId(tag);
        player.getPersistentData().put(TAG_CARRIED, tag);
        ladder.discard();
    }

    /** A player coming back, or into another world, carries again the ladder they left with. */
    public static void restore(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_CARRIED)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = data.getCompound(TAG_CARRIED);
        data.remove(TAG_CARRIED);
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        if (ladder == null) {
            return;
        }
        ladder.load(tag);
        ladder.setPos(player.position());
        if (level.getEntity(ladder.getUUID()) != null) {
            ladder.setUUID(Mth.createInsecureUUID(player.getRandom()));
        }
        if (level.addFreshEntity(ladder)) {
            take(player, ladder);
        }
    }

    private static void slow(Player player, int sections) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SLOWDOWN_ID);
        double amount = -SLOWDOWN_PER_PART * (1 + sections);
        //? if forge {
        /*speed.addTransientModifier(new AttributeModifier(SLOWDOWN_ID, "Carrying a siege ladder", amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        *///?} else {
        speed.addTransientModifier(new AttributeModifier(SLOWDOWN_ID, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        //?}
    }

    private static void unslow(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOWDOWN_ID);
        }
    }

    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(LadderCarry::stash);
        PlayerEvent.PLAYER_JOIN.register(LadderCarry::restore);
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> {
            // The ladder stayed behind in the world the player left; it goes with them.
            if (carried(player) != null) {
                stash(player);
                restore(player);
            }
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof Player player) {
                SiegeLadderEntity ladder = carried(player);
                if (ladder != null) {
                    drop(ladder);
                }
            }
            return EventResult.pass();
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (isCarrying(player)) {
                player.setSprinting(false);
            }
        });
        // Both hands are on the ladder.
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> isCarrying(player)
                ? CompoundEventResult.interruptFalse(player.getItemInHand(hand))
                : CompoundEventResult.pass());
        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, face) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        LifecycleEvent.SERVER_STOPPED.register(server -> CARRIED.clear());
    }
}
