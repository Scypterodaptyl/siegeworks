package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Which siege commands the Recruits screen may offer the selected groups, and why the rest are refused. Each state
 * follows the rule its command is carried out by, so a button that lights up always does something.
 *
 * <p>States are keyed by the screen's button: {@code target.<button>} for the machine looked at,
 * {@code <TYPE>.<button>} for a machine type's tab, and a bare {@code <button>} for the rest. A key that maps to
 * null may be used.</p>
 */
public final class RecruitsCommandStates {
    private RecruitsCommandStates() {
    }

    /** The machine types the groups are tied to, and the state of every button the screen can show them. */
    public record States(List<SiegeCommandType> types, Map<String, Component> refusals) {
        public List<String> typeNames() {
            return types.stream().map(Enum::name).toList();
        }

        public boolean allows(String key) {
            return refusals.containsKey(key) && refusals.get(key) == null;
        }

        @Nullable
        public Component refusal(String key) {
            return refusals.get(key);
        }
    }

    public static States compute(ServerPlayer player, List<UUID> groupIds, int targetEntityId,
                                 @Nullable BlockPos targetPos) {
        Set<UUID> groups = new HashSet<>(groupIds);
        Predicate<AbstractRecruitEntity> chosen =
                recruit -> RecruitsCompat.isSelectedAndCommandable(player, recruit, groups);
        List<AbstractRecruitEntity> recruits = RecruitsCompat.commandRangeRecruits(player).stream()
                .filter(chosen)
                .toList();
        Map<String, Component> states = new LinkedHashMap<>();

        Entity target = targetEntityId < 0 ? null : player.serverLevel().getEntity(targetEntityId);
        if (target instanceof AbstractSiegeEntity siege && !siege.isRemoved()) {
            targetStates(player, siege, recruits, chosen, states);
        }
        genericStates(player, recruits, targetPos, states);

        Map<SiegeCommandType, List<AbstractSiegeEntity>> machines = tiedMachines(player, recruits);
        for (Map.Entry<SiegeCommandType, List<AbstractSiegeEntity>> entry : machines.entrySet()) {
            typeStates(player, entry.getKey(), entry.getValue(), recruits, chosen, states);
        }
        return new States(List.copyOf(machines.keySet()), states);
    }

    private static void targetStates(ServerPlayer player, AbstractSiegeEntity siege,
                                     List<AbstractRecruitEntity> recruits,
                                     Predicate<AbstractRecruitEntity> chosen, Map<String, Component> states) {
        boolean inRange = RecruitsCompat.withinCommandRange(player, siege);
        Component tooFar = reason("out_of_range.reason");

        Component crewing = !inRange ? tooFar
                : !SiegeAccess.allows(player, siege, SiegeAccess.Action.USE) && siege.captureRefusal(player) != null
                        ? siege.captureRefusal(player).message()
                        : recruits.stream().anyMatch(recruit -> recruit.getVehicle() != siege
                                && recruit.distanceToSqr(siege) <= RecruitsCompat.COMMAND_RANGE * RecruitsCompat.COMMAND_RANGE)
                                ? null : reason("no_men");
        states.put("target.crew", crewing);
        states.put("target.crew_machine", crewing);

        if (siege instanceof SiegeTowerEntity tower) {
            states.put("target.return", !inRange ? tooFar
                    : !SiegeAccess.allows(player, tower, SiegeAccess.Action.USE) ? reason("not_yours")
                    : RecruitsOrderChecks.returnToTower(player, tower, chosen));
        }
        if (siege instanceof SiegeLadderEntity ladder) {
            states.put("target.pickup_ladder", !inRange ? tooFar
                    : RecruitsCompat.assignLadderPickup(player, ladder, chosen, true)
                            ? null : reason("nobody_to_carry"));
        }

        RecruitsCompat.MaintenanceRefusal repair = RecruitsCompat.assignMaintenance(
                player, siege, false, chosen, true, recruits);
        states.put("target.repair", !inRange ? tooFar
                : siege.getHealth() >= siege.getMaxHealth() ? reason("undamaged")
                : repair == RecruitsCompat.MaintenanceRefusal.NONE ? null : repair.message());
        RecruitsCompat.MaintenanceRefusal dismantle = RecruitsCompat.assignMaintenance(
                player, siege, true, chosen, true, recruits);
        states.put("target.dismantle", !inRange ? tooFar
                : dismantle == RecruitsCompat.MaintenanceRefusal.NONE ? null : dismantle.message());
    }

    private static void genericStates(ServerPlayer player, List<AbstractRecruitEntity> recruits,
                                      @Nullable BlockPos targetPos, Map<String, Component> states) {
        boolean validTarget = RecruitsCompat.isValidCommandTarget(player, targetPos);
        Component noTarget = reason("out_of_range.reason");

        states.put("place_ladder", !validTarget ? noTarget
                : recruits.stream().anyMatch(recruit -> RecruitsCompat.isAvailableForLadderRelocation(recruit)
                        && RecruitsCompat.carriesLadder(recruit)) ? null : reason("no_ladder_carrier"));

        List<SiegeEngineerEntity> freeEngineers = recruits.stream()
                .filter(SiegeEngineerEntity.class::isInstance)
                .map(SiegeEngineerEntity.class::cast)
                .filter(engineer -> !engineer.isPassenger()
                        && !RecruitsConstructionController.hasTask(engineer))
                .toList();
        states.put("build", !validTarget ? noTarget
                : freeEngineers.isEmpty()
                        ? Component.translatable("message.siegeworks.recruits.build_requires_engineer")
                        : freeEngineers.stream().noneMatch(RecruitsConstructionController::hasConstructionHammer)
                                ? Component.translatable("message.siegeworks.recruits.build_requires_hammer")
                                : null);

        boolean store = validTarget && player.serverLevel().mayInteract(player, targetPos)
                && RecruitsCompat.mayOpenInClaim(player, targetPos)
                && RecruitsCompat.isContainer(player, targetPos);
        states.put("supply", !store ? reason("not_a_store")
                : recruits.stream().anyMatch(SiegeEngineerEntity.class::isInstance)
                        ? null : reason("no_engineer_in_groups"));

        states.put("cancel_work", recruits.stream().anyMatch(recruit ->
                RecruitsLadderRelocationController.hasTask(recruit)
                        || recruit instanceof SiegeEngineerEntity engineer
                        && (RecruitsMaintenanceController.hasTask(engineer)
                                || RecruitsConstructionController.hasTask(engineer)))
                ? null : reason("nothing_to_cancel"));
    }

    private static void typeStates(ServerPlayer player, SiegeCommandType type, List<AbstractSiegeEntity> machines,
                                   List<AbstractRecruitEntity> recruits,
                                   Predicate<AbstractRecruitEntity> chosen, Map<String, Component> states) {
        String prefix = type.name() + ".";
        states.put(prefix + "leave", recruits.stream()
                .anyMatch(recruit -> RecruitsCompat.matchesSelectedMachine(recruit, type.id())
                        && !(type.kind() == SiegeCommandType.Kind.LADDER))
                ? null : reason("nobody_aboard"));

        // Weapon, bridge and flap orders go to the selected groups' own man at the levers.
        boolean manned = machines.stream().anyMatch(machine -> {
            SiegeEngineerEntity operator = RecruitsCompat.operatorOf(machine);
            return operator != null && chosen.test(operator);
        });
        switch (type.kind()) {
            case ARTILLERY, RAM -> {
                Component weapon = manned ? null : reason("no_layer");
                for (String key : List.of("fire_position", "stop_attack", "fire_at_will", "attack_at_will",
                        "ammo_auto", "ammo_standard", "ammo_explosive", "ammo_incendiary")) {
                    states.put(prefix + key, weapon);
                }
            }
            case TOWER -> {
                Component bridge = manned ? null : reason("no_driver");
                for (String key : List.of("bridge_lower", "bridge_raise", "bridge_auto")) {
                    states.put(prefix + key, bridge);
                }
                states.put(prefix + "unload_tower", towerUnload(player, recruits));
                states.put(prefix + "return_tower", towerReturn(player, recruits, chosen));
            }
            case MANTLET -> {
                Component flap = machines.stream().anyMatch(machine -> machine instanceof MantletEntity
                        && RecruitsCompat.operatorOf(machine) != null && chosen.test(RecruitsCompat.operatorOf(machine)))
                        ? null : reason("no_driver");
                states.put(prefix + "flap_open", flap);
                states.put(prefix + "flap_close", flap);
            }
            case LADDER -> {
            }
        }
    }

    /** As the unload order picks its towers: those the groups ride in or drive. */
    @Nullable
    private static Component towerUnload(ServerPlayer player, List<AbstractRecruitEntity> recruits) {
        Set<SiegeTowerEntity> towers = new LinkedHashSet<>();
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.getVehicle() instanceof SiegeTowerEntity tower
                    && (tower.isInteriorPassenger(recruit)
                            || recruit instanceof SiegeEngineerEntity && tower.isOperator(recruit))) {
                towers.add(tower);
            }
        }
        return firstAllowed(towers, tower -> RecruitsOrderChecks.unload(player, tower,
                recruit -> RecruitsCompat.isCommandable(player, recruit)), reason("nobody_inside"));
    }

    /** As the return order picks its towers: those the groups' men on foot belong to. */
    @Nullable
    private static Component towerReturn(ServerPlayer player, List<AbstractRecruitEntity> recruits,
                                         Predicate<AbstractRecruitEntity> chosen) {
        Set<SiegeTowerEntity> towers = new LinkedHashSet<>();
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.getVehicle() == null && recruit.getMountUUID() != null
                    && player.serverLevel().getEntity(recruit.getMountUUID()) instanceof SiegeTowerEntity tower) {
                towers.add(tower);
            }
        }
        return firstAllowed(towers, tower -> RecruitsOrderChecks.returnToTower(player, tower, chosen),
                reason("nobody_away"));
    }

    @Nullable
    private static Component firstAllowed(Set<SiegeTowerEntity> towers,
                                          java.util.function.Function<SiegeTowerEntity, Component> check,
                                          Component none) {
        Component first = none;
        boolean any = false;
        for (SiegeTowerEntity tower : towers) {
            Component refusal = check.apply(tower);
            if (refusal == null) {
                return null;
            }
            if (!any) {
                first = refusal;
                any = true;
            }
        }
        return first;
    }

    /** Every machine of a known type the groups work, ride in, push, draw or belong to. */
    private static Map<SiegeCommandType, List<AbstractSiegeEntity>> tiedMachines(
            ServerPlayer player, List<AbstractRecruitEntity> recruits) {
        Map<SiegeCommandType, List<AbstractSiegeEntity>> machines = new LinkedHashMap<>();
        EnumSet<SiegeCommandType> order = EnumSet.noneOf(SiegeCommandType.class);
        Set<UUID> seen = new HashSet<>();
        List<AbstractSiegeEntity> found = new ArrayList<>();
        for (AbstractRecruitEntity recruit : recruits) {
            AbstractSiegeEntity machine = machineOf(player, recruit);
            if (machine != null && seen.add(machine.getUUID())) {
                found.add(machine);
            }
        }
        for (AbstractSiegeEntity machine : found) {
            SiegeCommandType type = SiegeCommandType.from(machine);
            if (type != null) {
                order.add(type);
            }
        }
        for (SiegeCommandType type : order) {
            machines.put(type, found.stream().filter(type::matches).toList());
        }
        return machines;
    }

    @Nullable
    private static AbstractSiegeEntity machineOf(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (recruit instanceof SiegeEngineerEntity engineer && RecruitsCompat.workedMachine(engineer) != null) {
            return RecruitsCompat.workedMachine(engineer);
        }
        Entity vehicle = recruit.getVehicle() instanceof AbstractHorse mount ? mount.getVehicle() : recruit.getVehicle();
        if (vehicle instanceof AbstractSiegeEntity siege) {
            return siege;
        }
        return recruit.getMountUUID() != null
                && player.serverLevel().getEntity(recruit.getMountUUID()) instanceof AbstractSiegeEntity siege
                ? siege : null;
    }

    private static Component reason(String key) {
        return Component.translatable("gui.siegeworks.rts.action." + key);
    }
}
