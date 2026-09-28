package me.mss1r.siegeworks.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/** Handles item use without making the mounted player dismount first. */
public interface MountedSiegeItemControl {
    boolean acceptsMountedItem(Player player, InteractionHand hand);

    /** Called on the server after {@link #acceptsMountedItem} accepts the interaction. */
    InteractionResult handleMountedItem(Player player, InteractionHand hand, ServerLevel serverLevel);
}
