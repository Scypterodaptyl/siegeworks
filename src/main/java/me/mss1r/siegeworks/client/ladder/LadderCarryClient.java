package me.mss1r.siegeworks.client.ladder;

import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.network.LadderPutDownC2SPayload;
import me.mss1r.siegeworks.network.SiegeworksNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;

/** The client side of carrying a ladder: hands up on it, no running, and a click to set it down. */
public final class LadderCarryClient {
    /** Arms raised to the ladder over the head, a little apart. */
    private static final float ARMS_RAISED = (float) Math.toRadians(-170.0D);
    private static final float ARMS_APART = 0.22F;

    private LadderCarryClient() {
    }

    /**
     * Before the game reads its keys: the hands are full, so clicks do nothing but a shift-click, which sets the
     * ladder down, and the run key does nothing.
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
            // Nothing to swing with.
        }
        if (putDown && minecraft.screen == null) {
            SiegeworksNetworking.sendToServer(new LadderPutDownC2SPayload());
        }
    }

    /** After the game has moved the player: a double tap of forward may have started a run, which ends here. */
    public static void afterTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player != null && player.isSprinting() && LadderCarry.isCarrying(player)) {
            player.setSprinting(false);
        }
    }

    /** Leaving a world, or for another: the ladders seen carried there are gone with it. */
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
