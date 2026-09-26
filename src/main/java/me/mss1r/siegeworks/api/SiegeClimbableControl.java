package me.mss1r.siegeworks.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface SiegeClimbableControl {
    boolean isReadyForAutomatedClimb();

    Vec3 getAutomatedBottomApproach();

    Vec3 getAutomatedTopExit();

    Vec3 getAutomatedQueuePosition(LivingEntity climber, boolean upward);

    ClimbResult advanceAutomatedClimber(LivingEntity climber, boolean upward);

    void cancelAutomatedClimb(LivingEntity climber);

    enum ClimbResult {
        WAITING,
        IN_PROGRESS,
        COMPLETE,
        FAILED
    }
}
