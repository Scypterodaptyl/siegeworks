package me.mss1r.siegeworks.gameplay.loading;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public final class SiegeLoadingController {
    public interface Host {
        AbstractSiegeEntity siege();

        int stageDuration(String configKey);

        void loadingStarted(ServerLevel level);

        void loadingTick(ServerLevel level, Player player, InteractionHand hand,
                         int elapsedTicks, int totalTicks);

        void loadingCompleted(ServerLevel level, Player player, InteractionHand hand, int stageIndex);

        void loadingCancelled();

        boolean playSoundOnStart();

        boolean loopVisualReloadSound();

        int visualReloadSoundInterval();

        void playReloadSound(ServerLevel level);

        double serviceRange();

        int cooldownTotalTicks();

        String cooldownStatusKey();
    }

    private final Host host;
    private Player actor;
    private UUID actorUuid;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private int stageIndex = -1;
    private int remainingTicks;
    private int totalTicks;
    private Item item = Items.AIR;
    private LoadingStatus status = LoadingStatus.LOADING_AMMUNITION;
    private Item completingItem = Items.AIR;
    private int visualReloadSoundTicks;

    public SiegeLoadingController(Host host) {
        this.host = host;
    }

    public boolean active() {
        return stageIndex >= 0;
    }

    public Item activeItem() {
        return item == Items.AIR ? completingItem : item;
    }

    public void clear() {
        stageIndex = -1;
        remainingTicks = 0;
        totalTicks = 0;
        item = Items.AIR;
        status = LoadingStatus.LOADING_AMMUNITION;
        actor = null;
        actorUuid = null;
        hand = InteractionHand.MAIN_HAND;
    }

    public void cancelIfStageChanged(int newStage) {
        if (active() && stageIndex != newStage) {
            cancelSession();
        }
    }

    public boolean continueIfHeldItemMatches(Player player) {
        if (!active()) {
            return false;
        }
        if (actorUuid != null && actorUuid.equals(player.getUUID()) && !hasActiveItem(player)) {
            cancel(player, Component.translatable("siege.loading.cancelled"));
            return false;
        }
        return true;
    }

    public InteractionResult begin(Player player, InteractionHand usedHand, ServerLevel level,
                                   int newStageIndex, LoadingRequirement stage) {
        return begin(player, usedHand, level, newStageIndex, stage, host.stageDuration(stage.timingKey()));
    }

    public InteractionResult begin(Player player, InteractionHand usedHand, ServerLevel level,
                                   int newStageIndex, LoadingRequirement stage, int durationTicks) {
        if (active()) {
            return showProgress(player);
        }

        ItemStack stack = player.getItemInHand(usedHand);
        stageIndex = newStageIndex;
        remainingTicks = Math.max(1, durationTicks);
        totalTicks = remainingTicks;
        item = stack.getItem();
        status = stage.feedback();
        actor = player;
        actorUuid = player.getUUID();
        hand = usedHand;
        host.loadingStarted(level);
        if (host.playSoundOnStart()) {
            host.playReloadSound(level);
        }
        showProgress(player);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult beginMountedStage(Player player, InteractionHand usedHand,
                                               ServerLevel level, LoadingRequirement[] stages) {
        AbstractSiegeEntity siege = siege();
        if (siege.getFirstPassenger() != player) {
            return InteractionResult.PASS;
        }
        if (active()) {
            return showProgress(player);
        }
        if (siege.getCooldown() > 0) {
            return showCooldown(player);
        }

        int currentStage = siege.getLoadStage();
        if (currentStage < 0 || currentStage >= stages.length) {
            return InteractionResult.SUCCESS;
        }

        LoadingRequirement stage = stages[currentStage];
        if (!canBegin(player, usedHand, stage)) {
            return InteractionResult.SUCCESS;
        }
        return begin(player, usedHand, level, currentStage, stage);
    }

    public boolean acceptsMountedItem(Player player, InteractionHand usedHand, LoadingRequirement[] stages) {
        AbstractSiegeEntity siege = siege();
        if (siege.getFirstPassenger() != player) {
            return false;
        }

        ItemStack stack = player.getItemInHand(usedHand);
        if (active()) {
            return stack.is(activeItem());
        }

        int currentStage = siege.getLoadStage();
        return currentStage >= 0 && currentStage < stages.length
                && stages[currentStage].matches(stack.getItem());
    }

    public void tick(ServerLevel level) {
        if (!active()) {
            return;
        }

        Player activeActor = actor;
        if (activeActor == null || !activeActor.isAlive() || activeActor.isRemoved()
                || activeActor.level() != level) {
            cancelSession();
            return;
        }

        double serviceRange = host.serviceRange();
        if (activeActor.distanceToSqr(siege()) > serviceRange * serviceRange
                || siege().getLoadStage() != stageIndex) {
            cancel(activeActor, Component.translatable("siege.loading.cancelled"));
            return;
        }
        if (!hasActiveItem(activeActor)) {
            cancel(activeActor, Component.translatable("siege.loading.cancelled"));
            return;
        }

        remainingTicks--;
        int elapsedTicks = totalTicks - remainingTicks;
        host.loadingTick(level, activeActor, hand, elapsedTicks, totalTicks);

        if (remainingTicks > 0) {
            if (remainingTicks % 10 == 0) {
                showProgress(activeActor);
            }
            return;
        }

        int completedStage = stageIndex;
        InteractionHand completedHand = hand;
        Item completedItem = item;
        clear();
        completingItem = completedItem;
        try {
            host.loadingCompleted(level, activeActor, completedHand, completedStage);
        } finally {
            completingItem = Items.AIR;
        }
    }

    public void tickVisualReloadSound(ServerLevel level) {
        AbstractSiegeEntity siege = siege();
        if (!host.loopVisualReloadSound() || active() || !siege.hasAmmoLoaded()
                || siege.getWindingTime() <= 0 || !siege.isAlive()) {
            visualReloadSoundTicks = 0;
            return;
        }

        if (visualReloadSoundTicks <= 0) {
            host.playReloadSound(level);
            visualReloadSoundTicks = Math.max(1, host.visualReloadSoundInterval());
            return;
        }

        visualReloadSoundTicks--;
    }

    public InteractionResult showProgress(Player player) {
        if (!active()) {
            return InteractionResult.SUCCESS;
        }

        if (actorUuid != null && !player.getUUID().equals(actorUuid)) {
            player.displayClientMessage(Component.translatable("siege.loading.busy"), true);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(Component.translatable(status.translationKey(),
                progressBar(totalTicks - remainingTicks, totalTicks)), true);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult showCooldown(Player player) {
        int remaining = siege().getCooldown();
        if (remaining <= 0) {
            return InteractionResult.SUCCESS;
        }

        int total = Math.max(host.cooldownTotalTicks(), remaining);
        player.displayClientMessage(Component.translatable(host.cooldownStatusKey(),
                progressBar(total - remaining, total)), true);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult showWinding(Player player) {
        int remaining = siege().getWindingTime();
        if (remaining <= 0) {
            return InteractionResult.SUCCESS;
        }

        int total = Math.max(siege().getWindingTotal(), remaining);
        player.displayClientMessage(Component.translatable("siege.loading.state.winding",
                progressBar(total - remaining, total)), true);
        return InteractionResult.SUCCESS;
    }

    public boolean canBegin(Player player, InteractionHand usedHand, LoadingRequirement required) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!required.matches(stack.getItem())) {
            showRequiredItem(player, required);
            return false;
        }

        if (!player.isCreative() && stack.getCount() < required.requiredStackCount()) {
            showRequiredAmount(player, required);
            return false;
        }
        return true;
    }

    public boolean consume(Player player, InteractionHand usedHand, LoadingRequirement required) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!required.matches(stack.getItem())
                || (activeItem() != Items.AIR && !stack.is(activeItem()))) {
            showRequiredItem(player, required);
            return false;
        }

        if (!player.isCreative()) {
            if (stack.getCount() < required.requiredStackCount()) {
                showRequiredAmount(player, required);
                return false;
            }
            if (required.itemPolicy() == LoadingRequirement.ItemPolicy.CONSUME) {
                stack.shrink(required.amount());
            }
            if (required.itemPolicy() == LoadingRequirement.ItemPolicy.DAMAGE_TOOL) {
                MinecraftVersionCompat.damageHeldItem(stack, required.durabilityCost(), player, usedHand);
            }
        }
        return true;
    }

    private void cancel(Player actor, Component reason) {
        cancelSession();
        actor.displayClientMessage(reason, true);
    }

    private boolean hasActiveItem(Player actor) {
        return item != Items.AIR && actor.getItemInHand(hand).is(item);
    }

    private void cancelSession() {
        host.loadingCancelled();
        clear();
    }

    private static void showRequiredItem(Player player, LoadingRequirement required) {
        player.displayClientMessage(
                Component.translatable("siege.loading.next", required.item().getDefaultInstance().getHoverName()),
                true);
    }

    private static void showRequiredAmount(Player player, LoadingRequirement required) {
        player.displayClientMessage(
                Component.translatable("siege.loading.need_amount", required.amount(),
                        required.item().getDefaultInstance().getHoverName()), true);
    }

    private static String progressBar(int doneTicks, int totalTicks) {
        int width = 10;
        int done = Math.max(0, doneTicks);
        int total = Math.max(1, totalTicks);
        int filled = Math.max(0, Math.min(width, Math.round(done * width / (float) total)));
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }

    private AbstractSiegeEntity siege() {
        return host.siege();
    }
}
