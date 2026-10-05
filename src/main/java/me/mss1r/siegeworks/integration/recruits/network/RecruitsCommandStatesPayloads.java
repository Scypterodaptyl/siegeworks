package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.integration.recruits.RecruitsCommandStates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Recruits screen request for which siege buttons the selected groups can use, and the reply. */
public final class RecruitsCommandStatesPayloads {
    private static final int MAX_GROUPS = 64;
    private static final int MAX_STATES = 256;
    private static final int MAX_TYPES = 32;
    private static final int QUERY_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Long> LAST_QUERY = new HashMap<>();

    private RecruitsCommandStatesPayloads() {
    }

    public record Query(int queryId, List<UUID> groupIds, int targetEntityId, BlockPos targetPos) {
        public Query {
            groupIds = List.copyOf(groupIds);
        }

        public static void encode(Query query, FriendlyByteBuf buffer) {
            buffer.writeVarInt(query.queryId);
            buffer.writeVarInt(query.groupIds.size());
            query.groupIds.forEach(buffer::writeUUID);
            buffer.writeVarInt(query.targetEntityId);
            buffer.writeBoolean(query.targetPos != null);
            if (query.targetPos != null) {
                buffer.writeBlockPos(query.targetPos);
            }
        }

        public static Query decode(FriendlyByteBuf buffer) {
            int queryId = buffer.readVarInt();
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_GROUPS) {
                throw new IllegalArgumentException("Invalid Recruits group count: " + count);
            }
            List<UUID> groupIds = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                groupIds.add(buffer.readUUID());
            }
            int targetEntityId = buffer.readVarInt();
            BlockPos targetPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
            return new Query(queryId, groupIds, targetEntityId, targetPos);
        }

        public static void handle(Query query, NetworkManager.PacketContext context) {
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer sender && Platform.isModLoaded("recruits")) {
                    long now = sender.serverLevel().getGameTime();
                    Long last = LAST_QUERY.put(sender.getUUID(), now);
                    if (last != null && now - last < QUERY_COOLDOWN_TICKS && now >= last) {
                        return;
                    }
                    RecruitsCommandStates.States states = RecruitsCommandStates.compute(
                            sender, query.groupIds, query.targetEntityId, query.targetPos);
                    RecruitsNetworking.sendToPlayer(sender, new Answer(query.queryId,
                            states.typeNames(), states.refusals()));
                }
            });
        }
    }

    /** States keyed by button; a null refusal means the button may be used. */
    public record Answer(int queryId, List<String> types, Map<String, Component> refusals) {
        private static Consumer<Answer> receiver = answer -> {
        };

        public static void receiveWith(Consumer<Answer> handler) {
            receiver = handler;
        }

        public static void encode(Answer answer, FriendlyByteBuf buffer) {
            buffer.writeVarInt(answer.queryId);
            buffer.writeVarInt(answer.types.size());
            answer.types.forEach(buffer::writeUtf);
            buffer.writeVarInt(answer.refusals.size());
            answer.refusals.forEach((key, refusal) -> {
                buffer.writeUtf(key);
                buffer.writeBoolean(refusal != null);
                if (refusal != null) {
                    buffer.writeComponent(refusal);
                }
            });
        }

        public static Answer decode(FriendlyByteBuf buffer) {
            int queryId = buffer.readVarInt();
            int typeCount = buffer.readVarInt();
            if (typeCount < 0 || typeCount > MAX_TYPES) {
                throw new IllegalArgumentException("Invalid siege type count: " + typeCount);
            }
            List<String> types = new ArrayList<>(typeCount);
            for (int i = 0; i < typeCount; i++) {
                types.add(buffer.readUtf());
            }
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_STATES) {
                throw new IllegalArgumentException("Invalid command state count: " + count);
            }
            Map<String, Component> refusals = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                String key = buffer.readUtf();
                refusals.put(key, buffer.readBoolean() ? buffer.readComponent() : null);
            }
            return new Answer(queryId, types, refusals);
        }

        public static void handle(Answer answer, NetworkManager.PacketContext context) {
            context.queue(() -> receiver.accept(answer));
        }
    }
}
