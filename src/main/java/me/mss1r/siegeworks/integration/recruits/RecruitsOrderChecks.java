package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Reason an order to a machine can't be carried out now, or null if it can. Orders use the same checks, so an enabled
 * button always works and a disabled one says why.
 */
public final class RecruitsOrderChecks {
    private RecruitsOrderChecks() {
    }

    /** Unloading recruits from a tower over its bridge. */
    @Nullable
    public static Component unload(ServerPlayer player, SiegeTowerEntity tower,
                                   Predicate<AbstractRecruitEntity> chosen) {
        if (RecruitsCompat.aboard(player, tower, true, chosen) == 0) {
            return reason("nobody_inside");
        }
        return RecruitsCompat.bridgeWithinReach(player, tower) ? null : reason("no_driver");
    }

    /** Loading a tower's recruits back over its bridge. */
    @Nullable
    public static Component returnToTower(ServerPlayer player, SiegeTowerEntity tower,
                                          Predicate<AbstractRecruitEntity> chosen) {
        if (RecruitsCompat.awayFromTower(player, tower, chosen) == 0) {
            return reason("nobody_away");
        }
        return RecruitsCompat.bridgeWithinReach(player, tower) ? null : reason("no_driver");
    }

    /** Raising, lowering, or handing a tower's bridge to its driver. */
    @Nullable
    public static Component bridge(ServerPlayer player, SiegeTowerEntity tower) {
        return RecruitsCompat.setTowerBridge(player, tower, null, true) ? null : reason("no_driver");
    }

    /** Opening or closing a mantlet's flap, done by its driver. */
    @Nullable
    public static Component flap(ServerPlayer player, AbstractSiegeEntity siege) {
        if (!(siege instanceof MantletEntity) || !RecruitsCompat.commands(player, siege)) {
            return reason("not_yours");
        }
        return RecruitsCompat.commandedOperator(player, siege) == null ? reason("no_driver") : null;
    }

    /** Fire, attack and ammunition orders, carried out by the operator. */
    @Nullable
    public static Component weapon(ServerPlayer player, AbstractSiegeEntity siege) {
        return RecruitsCompat.commandedOperator(player, siege) == null ? reason("no_layer") : null;
    }

    /** Moving a machine, which needs an engineer on it or one free to board. */
    @Nullable
    public static Component drive(ServerPlayer player, AbstractSiegeEntity siege) {
        return RecruitsCompat.driveMachine(player, siege, null, true) ? null : reason("no_engineer");
    }

    private static Component reason(String key) {
        return Component.translatable("gui.siegeworks.rts.action." + key);
    }
}
