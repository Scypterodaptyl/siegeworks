package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RecruitsFireZoneC2SPayload(List<UUID> groupIds, BlockPos center, int radius) {
    private static final int MAX_GROUPS = 64;

    public RecruitsFireZoneC2SPayload {
        groupIds = List.copyOf(groupIds);
    }

    public static void encode(RecruitsFireZoneC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.groupIds.size());
        packet.groupIds.forEach(buffer::writeUUID);
        buffer.writeBlockPos(packet.center);
        buffer.writeVarInt(packet.radius);
    }

    public static RecruitsFireZoneC2SPayload decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_GROUPS) {
            throw new IllegalArgumentException("Invalid Recruits group count: " + count);
        }
        List<UUID> groupIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            groupIds.add(buffer.readUUID());
        }
        return new RecruitsFireZoneC2SPayload(
                groupIds, buffer.readBlockPos(), Math.max(0, buffer.readVarInt()));
    }

    public static void handle(RecruitsFireZoneC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender && Platform.isModLoaded("recruits")) {
                RecruitsCompat.handleFireZoneCommand(sender, packet.groupIds, packet.center, packet.radius);
            }
        });
    }
}
