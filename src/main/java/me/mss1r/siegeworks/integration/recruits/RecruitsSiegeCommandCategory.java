package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.network.MessageBackToMountEntity;
import com.talhanation.recruits.network.MessageMountEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import me.mss1r.axiomata.blueprint.api.BlueprintStacks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.api.SiegeAmmunitionControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.api.SiegeDeployableControl;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import net.minecraft.world.entity.Entity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsSiegeCommandC2SPayload;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsNetworking;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsTowerCrewC2SPayload;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
*///?} else {
import net.neoforged.api.distmarker.Dist;
//?}
//? if forge {
/*import net.minecraftforge.api.distmarker.OnlyIn;
*///?} else {
import net.neoforged.api.distmarker.OnlyIn;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
final class RecruitsSiegeCommandCategory implements ICommandCategory {
    private static final String HOLD_FIRE_STATE = "hold_fire";
    private static final String STRATEGIC_FIRE_STATE = "strategic_fire";
    private static boolean registered;

    static void register() {
        if (!registered) {
            CommandCategoryManager.register(new RecruitsSiegeCommandCategory(), 0);
            registered = true;
        }
    }

    @Override
    public Component getToolTipName() {
        return text("category");
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(SiegeworksItems.CANNON_BALL.get());
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        boolean hasActiveGroup = groups.stream().anyMatch(group -> !group.isDisabled());
        AbstractSiegeEntity machine = screen.rayEntity instanceof AbstractSiegeEntity siege ? siege : null;
        Layout layout = new Layout();

        if (machine instanceof SiegeTowerEntity tower) {
            layout.add(screen, "crew_machine", hasActiveGroup,
                    () -> sendTowerCrewCommand(groups, tower, RecruitsTowerCrewC2SPayload.ACTION_BOARD));
            layout.add(screen, "return", hasActiveGroup,
                    () -> sendTowerCrewCommand(groups, tower, RecruitsTowerCrewC2SPayload.ACTION_RETURN));
            layout.add(screen, "unload_tower", hasActiveGroup,
                    () -> sendTowerCrewCommand(groups, tower, RecruitsTowerCrewC2SPayload.ACTION_UNLOAD));
        } else if (machine != null && !(machine instanceof SiegeLadderEntity)) {
            layout.add(screen, "crew", hasActiveGroup,
                    () -> forEachActiveGroup(groups, group -> Main.SIMPLE_CHANNEL.sendToServer(
                            new MessageMountEntity(player.getUUID(), machine.getUUID(), group.getUUID()))));
            layout.add(screen, "return", hasActiveGroup,
                    () -> forEachActiveGroup(groups, group -> Main.SIMPLE_CHANNEL.sendToServer(
                            new MessageBackToMountEntity(player.getUUID(), group.getUUID()))));
        }
        if (machine != null && !(machine instanceof SiegeLadderEntity)) {
            layout.addCustom(screen, "leave", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_LEAVE_ENGINE);
        }

        if (machine instanceof SiegeDeployableControl) {
            layout.column();
            layout.addCustom(screen, "bridge_lower", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_LOWER);
            layout.addCustom(screen, "bridge_raise", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_RAISE);
            layout.addCustom(screen, "bridge_auto", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_AUTO);
        }

        if (machine instanceof MantletEntity) {
            layout.column();
            layout.addTargeted(screen, "flap_open", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_FLAP_OPEN, machine);
            layout.addTargeted(screen, "flap_close", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_FLAP_CLOSE, machine);
        }

        if (machine instanceof SiegeArtilleryControl) {
            layout.column();
            layout.add(screen, "fire_position", hasActiveGroup && screen.rayBlockPos != null,
                    () -> {
                        sendCustomCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_FIRE_POSITION,
                                screen.rayBlockPos);
                        forEachActiveGroup(groups, group -> {
                            ClientManager.addGroupSpecialState(group.getUUID(), STRATEGIC_FIRE_STATE);
                            ClientManager.removeGroupSpecialState(group.getUUID(), HOLD_FIRE_STATE);
                        });
                    });
            layout.add(screen, "stop_attack", hasActiveGroup,
                    () -> {
                        sendCustomCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_HOLD_FIRE, null);
                        forEachActiveGroup(groups, group -> {
                            ClientManager.addGroupSpecialState(group.getUUID(), HOLD_FIRE_STATE);
                            ClientManager.removeGroupSpecialState(group.getUUID(), STRATEGIC_FIRE_STATE);
                        });
                    });
            layout.add(screen, "fire_at_will", hasActiveGroup,
                    () -> {
                        sendCustomCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_FIRE_AT_WILL, null);
                        forEachActiveGroup(groups, group -> {
                            ClientManager.removeGroupSpecialState(group.getUUID(), HOLD_FIRE_STATE);
                            ClientManager.removeGroupSpecialState(group.getUUID(), STRATEGIC_FIRE_STATE);
                        });
                    });
        }

        if (machine instanceof SiegeAmmunitionControl ammunition) {
            layout.column();
            addAmmunitionButton(screen, layout, groups, hasActiveGroup, ammunition,
                    SiegeAmmunitionMode.AUTO, "ammo_auto", RecruitsSiegeCommandC2SPayload.ACTION_AMMO_AUTO);
            addAmmunitionButton(screen, layout, groups, hasActiveGroup, ammunition,
                    SiegeAmmunitionMode.STANDARD, "ammo_standard",
                    RecruitsSiegeCommandC2SPayload.ACTION_AMMO_STANDARD);
            addAmmunitionButton(screen, layout, groups, hasActiveGroup, ammunition,
                    SiegeAmmunitionMode.EXPLOSIVE, "ammo_explosive",
                    RecruitsSiegeCommandC2SPayload.ACTION_AMMO_EXPLOSIVE);
            addAmmunitionButton(screen, layout, groups, hasActiveGroup, ammunition,
                    SiegeAmmunitionMode.INCENDIARY, "ammo_incendiary",
                    RecruitsSiegeCommandC2SPayload.ACTION_AMMO_INCENDIARY);
        }

        if (machine instanceof SiegeLadderEntity) {
            layout.column();
            layout.addTargeted(screen, "pickup_ladder", hasActiveGroup,
                    groups, RecruitsSiegeCommandC2SPayload.ACTION_PICKUP_LADDER, machine);
        }
        layout.column();
        layout.addTargetedAt(screen, "place_ladder",
                hasActiveGroup && screen.rayBlockPos != null, groups,
                RecruitsSiegeCommandC2SPayload.ACTION_PLACE_LADDER, screen.rayBlockPos);

        layout.column();
        layout.addTargetedAt(screen, "build",
                hasActiveGroup && screen.rayBlockPos != null
                        && BlueprintStacks.isBlueprint(player.getOffhandItem()),
                groups, RecruitsSiegeCommandC2SPayload.ACTION_BUILD, screen.rayBlockPos);
        layout.addTargetedAt(screen, "supply", hasActiveGroup && screen.rayBlockPos != null,
                groups, RecruitsSiegeCommandC2SPayload.ACTION_SET_SUPPLIES, screen.rayBlockPos);

        if (machine != null) {
            int targetEntityId = machine.getId();
            layout.add(screen, "repair", hasActiveGroup,
                    () -> sendMaintenanceCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_REPAIR,
                            targetEntityId));
            layout.add(screen, "dismantle", hasActiveGroup,
                    () -> sendMaintenanceCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_DISMANTLE,
                            targetEntityId));
            layout.add(screen, "cancel_work", hasActiveGroup,
                    () -> sendMaintenanceCommand(groups,
                            RecruitsSiegeCommandC2SPayload.ACTION_CANCEL_MAINTENANCE, targetEntityId));
        }

        layout.place(screen, x, y);
    }

    private static void addAmmunitionButton(CommandScreen screen, Layout layout,
                                            List<RecruitsGroup> groups, boolean hasActiveGroup,
                                            SiegeAmmunitionControl ammunition, SiegeAmmunitionMode mode,
                                            String key, int action) {
        if (ammunition.supportsAmmunitionMode(mode)) {
            layout.addCustom(screen, key, hasActiveGroup, groups, action);
        }
    }

    private static final class Layout {
        private static final int COLUMN_WIDTH = 104;
        private static final int ROW_HEIGHT = 25;
        private static final int ROWS_PER_COLUMN = 5;

        private final List<Entry> entries = new ArrayList<>();

        private record Entry(String key, boolean enabled, Runnable action) {
        }

        private void column() {
        }

        private void add(CommandScreen screen, String key, boolean enabled, Runnable action) {
            entries.add(new Entry(key, enabled, action));
        }

        private void addCustom(CommandScreen screen, String key, boolean enabled,
                               List<RecruitsGroup> groups, int action) {
            add(screen, key, enabled, () -> sendCustomCommand(groups, action, null));
        }

        private void addTargeted(CommandScreen screen, String key, boolean enabled,
                                 List<RecruitsGroup> groups, int action, Entity target) {
            add(screen, key, enabled, () -> sendMaintenanceCommand(groups, action, target.getId()));
        }

        private void addTargetedAt(CommandScreen screen, String key, boolean enabled,
                                   List<RecruitsGroup> groups, int action, BlockPos target) {
            add(screen, key, enabled, () -> sendCustomCommand(groups, action, target));
        }

        private void place(CommandScreen screen, int originX, int originY) {
            if (entries.isEmpty()) {
                return;
            }
            int columns = (entries.size() + ROWS_PER_COLUMN - 1) / ROWS_PER_COLUMN;
            int rows = Math.min(entries.size(), ROWS_PER_COLUMN);
            for (int index = 0; index < entries.size(); index++) {
                Entry entry = entries.get(index);
                int column = index / ROWS_PER_COLUMN;
                int row = index % ROWS_PER_COLUMN;
                int x = originX + Math.round((column - (columns - 1) / 2.0F) * COLUMN_WIDTH);
                int y = originY + Math.round((row - (rows - 1) / 2.0F) * ROW_HEIGHT);
                addButton(screen, x, y, entry.key(), entry.enabled(), entry.action());
            }
        }
    }

    private static void addMaintenanceButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups,
                                              boolean hasActiveGroup, boolean towerLayout) {
        boolean hasSiegeTarget = hasActiveGroup && screen.rayEntity instanceof AbstractSiegeEntity;
        int targetEntityId = hasSiegeTarget
                ? screen.rayEntity.getId()
                : RecruitsSiegeCommandC2SPayload.NO_TARGET_ENTITY;
        int maintenanceX = towerLayout ? x + 100 : x - 50;
        int repairY = towerLayout ? y - 50 : y + 25;
        int dismantleY = towerLayout ? y - 25 : y + 50;
        int cancelX = towerLayout ? x + 100 : x + 150;
        int cancelY = towerLayout ? y : y + 50;
        addButton(screen, maintenanceX, repairY, "repair", hasSiegeTarget,
                () -> sendMaintenanceCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_REPAIR,
                        targetEntityId));
        addButton(screen, maintenanceX, dismantleY, "dismantle", hasSiegeTarget,
                () -> sendMaintenanceCommand(groups, RecruitsSiegeCommandC2SPayload.ACTION_DISMANTLE,
                        targetEntityId));
        addButton(screen, cancelX, cancelY, "cancel_work", hasActiveGroup,
                () -> sendMaintenanceCommand(groups,
                        RecruitsSiegeCommandC2SPayload.ACTION_CANCEL_MAINTENANCE,
                        RecruitsSiegeCommandC2SPayload.NO_TARGET_ENTITY));

        if (towerLayout) {
            return;
        }

        boolean hasLadderTarget = hasActiveGroup && screen.rayEntity instanceof SiegeLadderEntity;
        int ladderEntityId = hasLadderTarget
                ? screen.rayEntity.getId()
                : RecruitsSiegeCommandC2SPayload.NO_TARGET_ENTITY;
        addButton(screen, x + 50, y + 25, "pickup_ladder", hasLadderTarget,
                () -> sendMaintenanceCommand(groups,
                        RecruitsSiegeCommandC2SPayload.ACTION_PICKUP_LADDER, ladderEntityId));
        addTargetedCustomCommandButton(screen, x + 50, y + 50, "place_ladder",
                hasActiveGroup && screen.rayBlockPos != null && !hasLadderTarget,
                groups, RecruitsSiegeCommandC2SPayload.ACTION_PLACE_LADDER, screen.rayBlockPos);
    }

    private static void addCustomCommandButton(CommandScreen screen, int x, int y, String key, boolean active,
                                               List<RecruitsGroup> groups, int action) {
        addTargetedCustomCommandButton(screen, x, y, key, active, groups, action, null);
    }

    private static void addTargetedCustomCommandButton(CommandScreen screen, int x, int y, String key,
                                                       boolean active, List<RecruitsGroup> groups, int action,
                                                       BlockPos targetPos) {
        addButton(screen, x, y, key, active, () -> sendCustomCommand(groups, action, targetPos));
    }

    private static void sendCustomCommand(List<RecruitsGroup> groups, int action, BlockPos targetPos) {
        RecruitsNetworking.sendToServer(
                new RecruitsSiegeCommandC2SPayload(action, activeGroupIds(groups), targetPos));
    }

    private static void sendMaintenanceCommand(List<RecruitsGroup> groups, int action, int targetEntityId) {
        RecruitsNetworking.sendToServer(new RecruitsSiegeCommandC2SPayload(
                action, activeGroupIds(groups), null, targetEntityId));
    }

    private static void sendTowerCrewCommand(List<RecruitsGroup> groups, SiegeTowerEntity tower, int action) {
        RecruitsNetworking.sendToServer(
                new RecruitsTowerCrewC2SPayload(action, tower.getId(), activeGroupIds(groups)));
    }

    private static void addButton(CommandScreen screen, int x, int y, String key, boolean active, Runnable action) {
        RecruitsCommandButton button = new RecruitsCommandButton(x, y, text("text." + key), ignored -> action.run());
        button.setTooltip(Tooltip.create(text("tooltip." + key)));
        button.active = active;
        screen.addRenderableWidget(button);
    }

    private static List<UUID> activeGroupIds(List<RecruitsGroup> groups) {
        return groups.stream().filter(group -> !group.isDisabled()).map(RecruitsGroup::getUUID).toList();
    }

    private static void forEachActiveGroup(List<RecruitsGroup> groups,
                                           java.util.function.Consumer<RecruitsGroup> action) {
        groups.stream().filter(group -> !group.isDisabled()).forEach(action);
    }

    private static Component text(String suffix) {
        return Component.translatable("gui.siegeworks.recruits.siege_commands." + suffix);
    }
}
