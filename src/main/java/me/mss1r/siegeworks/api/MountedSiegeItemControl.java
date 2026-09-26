package me.mss1r.siegeworks.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public interface MountedSiegeItemControl {
    boolean acceptsMountedItem(Player player, InteractionHand hand);

    InteractionResult handleMountedItem(Player player, InteractionHand hand, ServerLevel serverLevel);
}
