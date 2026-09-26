package me.mss1r.siegeworks.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MountedSiegeItemHandler {
    private MountedSiegeItemHandler() {
    }

    public static void register() {
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            InteractionResult result = handleMountedItem(player, hand);
            ItemStack heldItem = player.getItemInHand(hand);
            return result == null
                    ? CompoundEventResult.pass()
                    : CompoundEventResult.interrupt(eventValue(result), heldItem);
        });
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                toEventResult(handleMountedItem(player, hand)));
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) ->
                toEventResult(handleMountedItem(player, hand)));
    }

    private static EventResult toEventResult(InteractionResult result) {
        return result == null ? EventResult.pass() : EventResult.interrupt(eventValue(result));
    }

    private static Boolean eventValue(InteractionResult result) {
        if (result == InteractionResult.PASS) {
            return null;
        }
        return result.consumesAction();
    }

    private static InteractionResult handleMountedItem(Player player, InteractionHand hand) {
        if (!(player.getVehicle() instanceof MountedSiegeItemControl control)
                || !control.acceptsMountedItem(player, hand)) {
            return null;
        }

        InteractionResult result = InteractionResult.SUCCESS;
        if (player.level() instanceof ServerLevel serverLevel) {
            result = control.handleMountedItem(player, hand, serverLevel);
        }
        return result;
    }
}
