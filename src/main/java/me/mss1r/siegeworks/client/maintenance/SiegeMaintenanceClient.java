package me.mss1r.siegeworks.client.maintenance;

import me.mss1r.siegeworks.network.OpenMaintenanceS2CPayload;
import net.minecraft.client.Minecraft;

public final class SiegeMaintenanceClient {
    private SiegeMaintenanceClient() {
    }

    public static void open(OpenMaintenanceS2CPayload payload) {
        Minecraft.getInstance().setScreen(new SiegeMaintenanceScreen(payload));
    }
}
