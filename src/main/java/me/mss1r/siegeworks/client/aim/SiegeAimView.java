package me.mss1r.siegeworks.client.aim;

import me.mss1r.siegeworks.entity.siege.AbstractBoltThrowerEntity;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class SiegeAimView {
    private SiegeAimView() {
    }

    public static AbstractSiegeEntity findControlledSiege(Player player) {
        AbstractSiegeEntity siege = findDirectlyRiddenSiege(player);
        return siege != null && supports(siege) && siege.getControllingPassenger() == player
                ? siege
                : null;
    }

    public static AbstractSiegeEntity findDirectlyRiddenSiege(Player player) {
        return player.getVehicle() instanceof AbstractSiegeEntity siege ? siege : null;
    }

    public static AbstractSiegeEntity findRiddenSiege(Player player) {
        Entity current = player.getVehicle();
        while (current != null) {
            if (current instanceof AbstractSiegeEntity siege) {
                return siege;
            }
            current = current.getVehicle();
        }
        return null;
    }

    public static boolean supports(AbstractSiegeEntity siege) {
        return siege instanceof AbstractBoltThrowerEntity
                || siege instanceof AbstractFieldGunEntity
                || siege instanceof MonsMegEntity
                || siege instanceof HwachaEntity;
    }
}
