package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RecruitsDriveOrders {
    private static final int ORDER_LIFETIME_TICKS = 6000;

    private static final int MAX_LEGS = 16;

    private static final Map<UUID, Order> BY_MACHINE = new HashMap<>();

    private RecruitsDriveOrders() {
    }

    static void replace(AbstractSiegeEntity machine, BlockPos destination, ServerPlayer commander) {
        Order order = new Order(commander.getUUID(), gameTime(machine));
        order.legs.addLast(destination.immutable());
        BY_MACHINE.put(machine.getUUID(), order);
    }

    static void append(AbstractSiegeEntity machine, BlockPos destination, ServerPlayer commander) {
        Order order = BY_MACHINE.get(machine.getUUID());
        if (order == null || expired(machine, order)
                || !order.commander.equals(commander.getUUID())) {
            replace(machine, destination, commander);
            return;
        }
        order.commander = commander.getUUID();
        order.issuedGameTime = gameTime(machine);
        if (order.legs.size() < MAX_LEGS) {
            order.legs.addLast(destination.immutable());
        }
    }

    static List<BlockPos> route(AbstractSiegeEntity machine) {
        Order order = BY_MACHINE.get(machine.getUUID());
        if (order == null) {
            return List.of();
        }
        if (expired(machine, order)) {
            BY_MACHINE.remove(machine.getUUID());
            return List.of();
        }
        return List.copyOf(order.legs);
    }

    static List<BlockPos> route(AbstractSiegeEntity machine, UUID commander) {
        Order order = BY_MACHINE.get(machine.getUUID());
        if (order == null || !order.commander.equals(commander)) {
            return List.of();
        }
        return route(machine);
    }

    static boolean any(AbstractSiegeEntity machine) {
        return !route(machine).isEmpty();
    }

    static boolean any(AbstractSiegeEntity machine, UUID commander) {
        Order order = BY_MACHINE.get(machine.getUUID());
        if (order == null || expired(machine, order)) {
            if (order != null) {
                BY_MACHINE.remove(machine.getUUID());
            }
            return false;
        }
        return order.commander.equals(commander) && !order.legs.isEmpty();
    }

    static void forget(AbstractSiegeEntity machine) {
        BY_MACHINE.remove(machine.getUUID());
    }

    static boolean forget(AbstractSiegeEntity machine, UUID commander) {
        Order order = BY_MACHINE.get(machine.getUUID());
        return order != null && order.commander.equals(commander)
                && BY_MACHINE.remove(machine.getUUID(), order);
    }

    static void clear() {
        BY_MACHINE.clear();
    }

    static boolean tick(SiegeEngineerEntity engineer) {
        AbstractSiegeEntity machine = RecruitsCompat.workedMachine(engineer);
        if (machine == null || !machine.isOperator(engineer)) {
            return false;
        }

        Order order = BY_MACHINE.get(machine.getUUID());
        if (order == null) {
            return false;
        }
        if (expired(machine, order)) {
            BY_MACHINE.remove(machine.getUUID());
            return false;
        }

        if (!engineer.isEffectedByCommand(order.commander)) {
            BY_MACHINE.remove(machine.getUUID());
            return false;
        }

        if (order.started && engineer.getShouldMovePos()) {
            return true;
        }

        if (!commanderPresent(engineer, machine, order)) {
            return false;
        }

        BlockPos leg = order.legs.peekFirst();
        if (leg == null) {
            BY_MACHINE.remove(machine.getUUID());
            return false;
        }

        order.started = true;
        order.issuedGameTime = gameTime(machine);
        RecruitsCompat.sendWhereToGo(engineer, leg);
        return true;
    }

    static boolean legReached(SiegeEngineerEntity engineer, BlockPos reached) {
        AbstractSiegeEntity machine = RecruitsCompat.workedMachine(engineer);
        if (machine == null || !machine.isOperator(engineer)) {
            return false;
        }

        Order order = BY_MACHINE.get(machine.getUUID());
        if (order == null || !order.started) {
            return false;
        }

        BlockPos leg = order.legs.peekFirst();
        if (leg == null || !leg.equals(reached)) {
            return false;
        }

        order.legs.pollFirst();
        BlockPos next = order.legs.peekFirst();
        if (next == null || !commanderPresent(engineer, machine, order)) {
            BY_MACHINE.remove(machine.getUUID());
            return false;
        }

        order.issuedGameTime = gameTime(machine);
        RecruitsCompat.sendWhereToGo(engineer, next);
        return true;
    }

    private static boolean commanderPresent(SiegeEngineerEntity engineer, AbstractSiegeEntity machine,
                                             Order order) {
        if (engineer.level().getServer() == null
                || engineer.level().getServer().getPlayerList().getPlayer(order.commander) == null
                || !engineer.isEffectedByCommand(order.commander)) {
            BY_MACHINE.remove(machine.getUUID());
            return false;
        }
        return true;
    }

    private static boolean expired(AbstractSiegeEntity machine, Order order) {
        return gameTime(machine) - order.issuedGameTime > ORDER_LIFETIME_TICKS;
    }

    private static long gameTime(AbstractSiegeEntity machine) {
        return machine.level().getGameTime();
    }

    private static final class Order {
        private final Deque<BlockPos> legs = new ArrayDeque<>();
        private UUID commander;
        private long issuedGameTime;
        private boolean started;

        private Order(UUID commander, long issuedGameTime) {
            this.commander = commander;
            this.issuedGameTime = issuedGameTime;
        }
    }
}
