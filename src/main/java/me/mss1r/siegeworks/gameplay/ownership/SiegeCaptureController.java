package me.mss1r.siegeworks.gameplay.ownership;

import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An enemy captures an unattended engine by holding its controls (seated, or standing at it for engines without a
 * player seat) until the timer runs out without taking damage.
 */
public final class SiegeCaptureController {
    private static final double BAR_RANGE_SQR = 48.0D * 48.0D;
    private static final double STANDING_REACH = 3.0D;
    private static final int BAR_AUDIENCE_REFRESH_TICKS = 10;

    public enum Refusal {
        DISABLED("message.siegeworks.access.denied"),
        DEFENDED("message.siegeworks.capture.defended"),
        DEFENDERS_OFFLINE("message.siegeworks.capture.defenders_offline"),
        LIMIT("message.siegeworks.capture.limit");

        private final String key;

        Refusal(String key) {
            this.key = key;
        }

        public Component message() {
            return Component.translatable(key);
        }
    }

    private final AbstractSiegeEntity siege;
    @Nullable
    private LivingEntity capturer;
    private boolean standing;
    private int progressTicks;
    @Nullable
    private ServerBossEvent attackerBar;
    @Nullable
    private ServerBossEvent defenderBar;

    public SiegeCaptureController(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public boolean isActive() {
        return capturer != null;
    }

    public boolean isCapturer(Entity entity) {
        return capturer != null && capturer == entity;
    }

    @Nullable
    public Refusal refusal(Entity entity) {
        if (!(entity instanceof LivingEntity)
                || !(siege.level() instanceof ServerLevel level)
                || !SiegeworksServerConfig.isOwnershipEnforced()
                || !SiegeworksServerConfig.isCaptureAllowed()
                || !siege.isCapturable()
                || !siege.isFullyBuilt()
                || siege.isDismantling()) {
            return Refusal.DISABLED;
        }
        UUID owner = siege.getOwnerUuid();
        UUID captor = SiegeOwnership.claimantOf(entity);
        if (owner == null || captor == null || SiegeAccess.relationOf(entity, siege) != SiegeRelation.HOSTILE) {
            return Refusal.DISABLED;
        }
        if (siege.getPassengers().stream().anyMatch(passenger -> passenger != entity
                && SiegeAccess.relationOf(passenger, siege) != SiegeRelation.HOSTILE)) {
            return Refusal.DEFENDED;
        }
        if (SiegeworksServerConfig.captureRequiresDefenderOnline() && !SiegeTeams.sideOnline(level, owner)) {
            return Refusal.DEFENDERS_OFFLINE;
        }
        if (!SiegeDeploymentLimits.check(level, siege.getType(),
                SiegeDeploymentLimits.forOwner(level, captor)).allowed()) {
            return Refusal.LIMIT;
        }
        return null;
    }

    public boolean beginStanding(Player player) {
        if (refusal(player) != null) {
            return false;
        }
        start(player, true);
        return true;
    }

    public void tick(ServerLevel level) {
        LivingEntity current = standing ? capturer : hostileCrew();
        if (current == null || standing && !withinReach(current)) {
            stop();
            return;
        }
        if (current != capturer) {
            if (refusal(current) != null) {
                stop();
                return;
            }
            start(current, false);
        } else if (refusal(current) != null) {
            stop();
            return;
        }

        progressTicks = current.hurtTime > 0 ? 0 : progressTicks + 1;
        int requiredTicks = SiegeworksServerConfig.getCaptureTicks(SiegeMaintenanceData.dismantleRequiredHits(siege));
        if (progressTicks >= requiredTicks) {
            complete(level, current);
            return;
        }
        updateBars(level, (float) progressTicks / requiredTicks);
    }

    public void stop() {
        capturer = null;
        standing = false;
        progressTicks = 0;
        if (attackerBar != null) {
            attackerBar.removeAllPlayers();
            attackerBar = null;
        }
        if (defenderBar != null) {
            defenderBar.removeAllPlayers();
            defenderBar = null;
        }
    }

    /**
     * Found by seat rather than operator status, since the locked controls don't give the capturer operator status.
     */
    @Nullable
    private LivingEntity hostileCrew() {
        for (Entity passenger : siege.getPassengers()) {
            if (passenger instanceof LivingEntity living && !AbstractSiegeEntity.isDraftMount(living)
                    && SiegeAccess.relationOf(living, siege) == SiegeRelation.HOSTILE) {
                return living;
            }
        }
        return null;
    }

    private boolean withinReach(LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && entity.level() == siege.level()
                && !entity.isPassenger()
                && siege.getBoundingBox().inflate(STANDING_REACH).contains(entity.position());
    }

    private void start(LivingEntity entity, boolean standing) {
        stop();
        capturer = entity;
        this.standing = standing;
        Component name = siege.getDisplayName();
        attackerBar = new ServerBossEvent(Component.translatable("bossbar.siegeworks.capture.attacker", name),
                BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        defenderBar = new ServerBossEvent(Component.translatable("bossbar.siegeworks.capture.defender", name),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        notifyOwner(Component.translatable("message.siegeworks.capture.started", name));
    }

    private void complete(ServerLevel level, LivingEntity entity) {
        UUID captor = SiegeOwnership.claimantOf(entity);
        stop();
        if (captor == null) {
            return;
        }
        Component name = siege.getDisplayName();
        notifyOwner(Component.translatable("message.siegeworks.capture.lost", name));
        SiegeDeploymentLimits.Deployment deployment = SiegeDeploymentLimits.forOwner(level, captor);
        siege.setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
        siege.setOwnerUuid(captor);
        ServerPlayer captorPlayer = level.getServer().getPlayerList().getPlayer(captor);
        if (captorPlayer != null) {
            captorPlayer.displayClientMessage(Component.translatable("message.siegeworks.capture.completed", name), true);
        }
    }

    private void updateBars(ServerLevel level, float progress) {
        if (attackerBar == null || defenderBar == null || capturer == null) {
            return;
        }
        attackerBar.setProgress(progress);
        defenderBar.setProgress(progress);
        if (progressTicks % BAR_AUDIENCE_REFRESH_TICKS != 1) {
            return;
        }
        UUID captor = SiegeOwnership.playerOf(capturer);
        for (ServerPlayer player : level.players()) {
            boolean inRange = player.distanceToSqr(siege) <= BAR_RANGE_SQR;
            Team team = player.getTeam();
            boolean attacking = SiegeAccess.relation(level, player.getUUID(),
                    team == null ? null : team.getName(), captor) != SiegeRelation.HOSTILE;
            updateAudience(attackerBar, player, inRange && attacking);
            updateAudience(defenderBar, player, inRange && !attacking);
        }
    }

    private static void updateAudience(ServerBossEvent bar, ServerPlayer player, boolean shown) {
        if (shown) {
            bar.addPlayer(player);
        } else {
            bar.removePlayer(player);
        }
    }

    private void notifyOwner(Component message) {
        UUID owner = siege.getOwnerUuid();
        if (owner != null && siege.level().getServer() != null) {
            ServerPlayer player = siege.level().getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }
}
