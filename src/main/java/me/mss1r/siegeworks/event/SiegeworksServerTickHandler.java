package me.mss1r.siegeworks.event;

import dev.architectury.event.events.common.TickEvent;
import me.mss1r.siegeworks.gameplay.damage.StructuralDamageSystem;
import net.minecraft.server.level.ServerLevel;

public final class SiegeworksServerTickHandler {
    private SiegeworksServerTickHandler() {
    }

    public static void register() {
        TickEvent.SERVER_POST.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                StructuralDamageSystem.tick(level);
            }
        });
    }

}
