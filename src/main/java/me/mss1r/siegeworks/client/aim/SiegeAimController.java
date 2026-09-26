package me.mss1r.siegeworks.client.aim;

import me.mss1r.siegeworks.client.SiegeworksKeyMappings;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class SiegeAimController {
    private static AbstractSiegeEntity controlledSiege;
    private static float freeLookYaw;
    private static float freeLookPitch;
    private static boolean freeLookInitialized;
    private static boolean freeLookLastTick;

    private SiegeAimController() {
    }

    public static void tick(LocalPlayer player) {
        AbstractSiegeEntity siege = SiegeAimView.findControlledSiege(player);
        if (siege == null) {
            reset();
            return;
        }

        initializeFor(siege);
        lockPlayerBodyToSiege(player, siege);
        boolean freeLook = isFreeLookActive(player, siege);
        if (freeLook && !freeLookLastTick) {
            initializeFreeLook(siege);
        }
        if (freeLookLastTick && !freeLook) {
            alignPlayerWithSiege(player, siege, true);
            freeLookInitialized = false;
        }
        freeLookLastTick = freeLook;
    }

    public static boolean isFreeLookActive(Player player, AbstractSiegeEntity siege) {
        return player.isLocalPlayer()
                && SiegeAimView.findControlledSiege(player) == siege
                && SiegeworksKeyMappings.FREE_LOOK.isDown();
    }

    public static void applyFreeLookInput(AbstractSiegeEntity siege, float yawDelta, float pitchDelta) {
        initializeFor(siege);
        initializeFreeLook(siege);
        freeLookYaw = Mth.wrapDegrees(freeLookYaw + yawDelta);
        freeLookPitch = Mth.clamp(freeLookPitch + pitchDelta, -89.0F, 89.0F);
    }

    public static float getFreeLookYaw(AbstractSiegeEntity siege) {
        initializeFor(siege);
        initializeFreeLook(siege);
        return freeLookYaw;
    }

    public static float getFreeLookPitch(AbstractSiegeEntity siege) {
        initializeFor(siege);
        initializeFreeLook(siege);
        return freeLookPitch;
    }

    public static void alignPlayerWithSiege(LocalPlayer player, AbstractSiegeEntity siege,
                                             boolean includePitch) {
        float yaw = siege.getRenderedAimYaw();
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        lockPlayerBodyToSiege(player, siege);
        if (includePitch) {
            float pitch = siege.getRenderedAimPitch();
            player.setXRot(pitch);
            player.xRotO = pitch;
        }
    }

    private static void lockPlayerBodyToSiege(LocalPlayer player, AbstractSiegeEntity siege) {
        float bodyYaw = siege.getPassengerVisualYaw(player);
        player.setYBodyRot(bodyYaw);
        player.yBodyRotO = bodyYaw;
    }

    private static void initializeFor(AbstractSiegeEntity siege) {
        if (controlledSiege == siege) {
            return;
        }

        controlledSiege = siege;
        freeLookInitialized = false;
        freeLookLastTick = false;
    }

    private static void initializeFreeLook(AbstractSiegeEntity siege) {
        if (freeLookInitialized) {
            return;
        }

        freeLookYaw = siege.getRenderedAimYaw();
        freeLookPitch = siege.getRenderedAimPitch();
        freeLookInitialized = true;
    }

    private static void reset() {
        controlledSiege = null;
        freeLookYaw = 0.0F;
        freeLookPitch = 0.0F;
        freeLookInitialized = false;
        freeLookLastTick = false;
    }
}
