package me.mss1r.siegeworks.client.ladder;

import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.network.LadderPutDownC2SPayload;
import me.mss1r.siegeworks.network.SiegeworksNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;

/** Client side of ladder carrying: raised arms, no sprinting, shift+use to put it down. */
public final class LadderCarryClient {
    /** Arm pose for holding a ladder overhead. */
    private static final float ARMS_RAISED = (float) Math.toRadians(-170.0D);
    private static final float ARMS_APART = 0.22F;

    private LadderCarryClient() {
    }

    /**
     * Runs before key handling. While carrying, clicks are swallowed, shift+use sends the put-down request, and sprint
     * is blocked.
     */
    public static void beforeTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || !LadderCarry.isCarrying(player)) {
            return;
        }
        minecraft.options.keySprint.setDown(false);
        boolean putDown = false;
        while (minecraft.options.keyUse.consumeClick()) {
            putDown |= player.isShiftKeyDown();
        }
        while (minecraft.options.keyAttack.consumeClick()) {
            // Attacks do nothing while carrying.
        }
        if (putDown && minecraft.screen == null) {
            SiegeworksNetworking.sendToServer(new LadderPutDownC2SPayload());
        }
    }

    /** Runs after movement: double-tapping forward can still start a sprint, so cancel it here. */
    public static void afterTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player != null && player.isSprinting() && LadderCarry.isCarrying(player)) {
            player.setSprinting(false);
        }
    }

    /** Clears client-side carry state on disconnect or dimension change. */
    public static void leaveWorld() {
        LadderCarry.forgetSeen();
    }

    public static void raiseArms(HumanoidModel<?> model) {
        model.rightArm.xRot = ARMS_RAISED;
        model.leftArm.xRot = ARMS_RAISED;
        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;
        model.rightArm.zRot = -ARMS_APART;
        model.leftArm.zRot = ARMS_APART;
    }
}
