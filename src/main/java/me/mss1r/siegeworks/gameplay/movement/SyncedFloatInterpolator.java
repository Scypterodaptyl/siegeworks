package me.mss1r.siegeworks.gameplay.movement;

import net.minecraft.util.Mth;

public final class SyncedFloatInterpolator {
    private boolean initialized;
    private float previous;
    private float current;

    public void tick(float synchronizedValue) {
        if (!initialized) {
            reset(synchronizedValue);
            return;
        }
        previous = current;
        current = synchronizedValue;
    }

    public void reset(float value) {
        initialized = true;
        previous = value;
        current = value;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public float sample(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previous, current);
    }
}
