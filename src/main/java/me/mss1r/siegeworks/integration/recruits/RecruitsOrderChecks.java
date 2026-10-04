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
 * Why an order to a machine cannot be carried out right now, or null when it can. The orders themselves go by the
 * same rules, so a button these allow always does something and a button they refuse says why.
 */
public final class RecruitsOrderChecks {
    private RecruitsOrderChecks() {
    }

    /** Sending recruits out of a tower over its bridge. */
    @Nullable
    public static Component unload(ServerPlayer player, SiegeTowerEntity tower,
                                   Predicate<AbstractRecruitEntity> chosen) {
        if (RecruitsCompat.aboard(player, tower, true, chosen) == 0) {
            return reason("nobody_inside");
        }
        return RecruitsCompat.bridgeWithinReach(player, tower) ? null : reason("no_driver");
    }

    /** Bringing a tower's recruits back aboard over its bridge. */
    @Nullable
    public static Component returnToTower(ServerPlayer player, SiegeTowerEntity tower,
                                          Predicate<AbstractRecruitEntity> chosen) {
        if (RecruitsCompat.awayFromTower(player, tower, chosen) == 0) {
            return reason("nobody_away");
        }
        return RecruitsCompat.bridgeWithinReach(player, tower) ? null : reason("no_driver");
    }

    /** Raising, lowering or leaving a tower's bridge to its driver. */
    @Nullable
    public static Component bridge(ServerPlayer player, SiegeTowerEntity tower) {
        return RecruitsCompat.setTowerBridge(player, tower, null, true) ? null : reason("no_driver");
    }

    /** Opening or closing a mantlet's flap, which its driver works. */
    @Nullable
    public static Component flap(ServerPlayer player, AbstractSiegeEntity siege) {
        if (!(siege instanceof MantletEntity) || !RecruitsCompat.commands(player, siege)) {
            return reason("not_yours");
        }
        return RecruitsCompat.commandedOperator(player, siege) == null ? reason("no_driver") : null;
    }

    /** Fire, attack and ammunition orders, which the man at the machine carries out. */
    @Nullable
    public static Component weapon(ServerPlayer player, AbstractSiegeEntity siege) {
        return RecruitsCompat.commandedOperator(player, siege) == null ? reason("no_layer") : null;
    }

    /** Driving a machine somewhere, which takes an engineer at it or one free to board it. */
    @Nullable
    public static Component drive(ServerPlayer player, AbstractSiegeEntity siege) {
        return RecruitsCompat.driveMachine(player, siege, null, true) ? null : reason("no_engineer");
    }

    private static Component reason(String key) {
        return Component.translatable("gui.siegeworks.rts.action." + key);
    }
}
