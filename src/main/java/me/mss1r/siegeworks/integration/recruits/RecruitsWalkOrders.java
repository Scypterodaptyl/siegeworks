package me.mss1r.siegeworks.integration.recruits;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

final class RecruitsWalkOrders {
    private static final double GOAL_CHANGE_DISTANCE_SQR = 0.25D;

    private static final Map<Mob, Vec3> ISSUED = new WeakHashMap<>();

    private RecruitsWalkOrders() {
    }

    static void walkTo(Mob walker, Vec3 goal, double speed) {
        PathNavigation navigation = walker.getNavigation();
        Vec3 issued = ISSUED.get(walker);
        if (issued != null && !navigation.isDone()
                && issued.distanceToSqr(goal) <= GOAL_CHANGE_DISTANCE_SQR) {
            return;
        }
        ISSUED.put(walker, goal);
        navigation.moveTo(goal.x, goal.y, goal.z, speed);
    }

    static void stop(Mob walker) {
        ISSUED.remove(walker);
        walker.getNavigation().stop();
    }

    static void clear() {
        ISSUED.clear();
    }
}
