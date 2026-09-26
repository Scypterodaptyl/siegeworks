package me.mss1r.siegeworks.gameplay.weapon;

import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeOperationState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class SiegeWeaponState {
    public static final float MINIMUM_SHOT_POWER = 0.20F;
    public static final float MAXIMUM_SHOT_POWER = 1.10F;
    public static final float SHOT_POWER_STEP = 0.10F;

    private static final String TAG_COOLDOWN = "Cooldown";
    private static final String TAG_COOLDOWN_TOTAL = "CooldownTotal";
    private static final String TAG_LOAD_STAGE = "LoadStage";
    private static final String TAG_AMMO_LOADED = "AmmoLoaded";
    private static final String TAG_SCATTERSHOT_COUNT = "ScattershotCount";
    private static final String TAG_AUTOMATED_AMMUNITION_MODE = "AutomatedAmmunitionMode";
    private static final String TAG_WINDING_TIME = "WindingTime";
    private static final String TAG_WINDING_TOTAL = "WindingTotal";
    private static final String TAG_SHOT_POWER = "ShotPower";
    private static final String TAG_ATTACK_HAPPENED = "AttackHappened";

    private final Host host;
    private int cooldownTotalTicks;

    public SiegeWeaponState(Host host) {
        this.host = host;
    }

    public void save(CompoundTag tag) {
        tag.putInt(TAG_COOLDOWN, cooldown());
        tag.putInt(TAG_COOLDOWN_TOTAL, cooldownTotalTicks);
        tag.putInt(TAG_LOAD_STAGE, loadStage());
        tag.putString(TAG_AMMO_LOADED, ammunition());
        tag.putInt(TAG_SCATTERSHOT_COUNT, scattershotCount());
        tag.putString(TAG_AUTOMATED_AMMUNITION_MODE, automatedAmmunitionMode().serializedName());
        tag.putInt(TAG_WINDING_TIME, windingTime());
        tag.putInt(TAG_WINDING_TOTAL, windingTotal());
        tag.putFloat(TAG_SHOT_POWER, shotPower());
        tag.putBoolean(TAG_ATTACK_HAPPENED, attackHappened());
    }

    public void load(CompoundTag tag) {
        if (tag.contains(TAG_COOLDOWN)) {
            setCooldown(tag.getInt(TAG_COOLDOWN));
        }
        if (tag.contains(TAG_COOLDOWN_TOTAL)) {
            cooldownTotalTicks = Math.max(cooldown(), tag.getInt(TAG_COOLDOWN_TOTAL));
        }
        if (tag.contains(TAG_LOAD_STAGE)) {
            setLoadStage(tag.getInt(TAG_LOAD_STAGE));
        }
        if (tag.contains(TAG_AMMO_LOADED)) {
            setAmmunition(tag.getString(TAG_AMMO_LOADED));
        }
        if (tag.contains(TAG_SCATTERSHOT_COUNT)) {
            setScattershotCount(tag.getInt(TAG_SCATTERSHOT_COUNT));
        }
        if (tag.contains(TAG_AUTOMATED_AMMUNITION_MODE)) {
            setAutomatedAmmunitionMode(SiegeAmmunitionMode.fromSerializedName(
                    tag.getString(TAG_AUTOMATED_AMMUNITION_MODE)));
        }
        if (tag.contains(TAG_WINDING_TIME)) {
            setWindingTime(tag.getInt(TAG_WINDING_TIME));
        }
        if (tag.contains(TAG_WINDING_TOTAL)) {
            setWindingTotal(tag.getInt(TAG_WINDING_TOTAL));
        }
        if (tag.contains(TAG_SHOT_POWER)) {
            setShotPower(tag.getFloat(TAG_SHOT_POWER));
        }
        if (tag.contains(TAG_ATTACK_HAPPENED)) {
            setAttackHappened(tag.getBoolean(TAG_ATTACK_HAPPENED));
        }
    }

    public void setCooldown(int ticks) {
        int current = cooldown();
        int normalized = Math.max(0, ticks);
        if (normalized > current) {
            cooldownTotalTicks = normalized;
        } else if (normalized == 0) {
            cooldownTotalTicks = 0;
        }
        host.setCooldown(normalized);
    }

    public int cooldown() {
        return host.cooldown();
    }

    public int cooldownTotalTicks() {
        return cooldownTotalTicks;
    }

    public int cooldownElapsedTicks() {
        return Math.max(0, cooldownTotalTicks - cooldown());
    }

    public void setLoadStage(int stage) {
        host.setLoadStage(Math.max(0, stage));
    }

    public int loadStage() {
        return host.loadStage();
    }

    public void setAmmunition(String ammunition) {
        host.setAmmunition(ammunition == null ? "" : ammunition);
    }

    public String ammunition() {
        return host.ammunition();
    }

    public boolean hasAmmunition() {
        return !ammunition().isEmpty();
    }

    public void setScattershotCount(int count) {
        host.setScattershotCount(Math.max(0, count));
    }

    public int scattershotCount() {
        return host.scattershotCount();
    }

    public void setAutomatedAmmunitionMode(SiegeAmmunitionMode mode) {
        SiegeAmmunitionMode resolved = mode == null ? SiegeAmmunitionMode.AUTO : mode;
        host.setAutomatedAmmunitionMode(resolved.serializedName());
    }

    public SiegeAmmunitionMode automatedAmmunitionMode() {
        return SiegeAmmunitionMode.fromSerializedName(host.automatedAmmunitionMode());
    }

    public void setWindingTime(int ticks) {
        int normalized = Math.max(0, ticks);
        if (normalized > windingTime()) {
            setWindingTotal(normalized);
        }
        host.setWindingTime(normalized);
    }

    public int windingTime() {
        return host.windingTime();
    }

    public void setWindingTotal(int ticks) {
        host.setWindingTotal(Math.max(0, ticks));
    }

    public int windingTotal() {
        return host.windingTotal();
    }

    public boolean windingComplete() {
        return windingTime() <= 0;
    }

    public void setShotPower(float power) {
        host.setShotPower(Mth.clamp(power, MINIMUM_SHOT_POWER, MAXIMUM_SHOT_POWER));
    }

    public float shotPower() {
        return host.shotPower();
    }

    public void cycleShotPower() {
        float next = shotPower() + SHOT_POWER_STEP;
        if (next > MAXIMUM_SHOT_POWER + 0.001F) {
            next = MINIMUM_SHOT_POWER;
        }
        setShotPower(Math.round(next * 10.0F) / 10.0F);
    }

    public String shotPowerBar() {
        int width = 10;
        int filled = Math.round((shotPower() - MINIMUM_SHOT_POWER) / SHOT_POWER_STEP) + 1;
        filled = Math.max(1, Math.min(width, filled));
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }

    public void setAttackHappened(boolean happened) {
        host.setAttackHappened(happened);
    }

    public boolean attackHappened() {
        return host.attackHappened();
    }

    public SiegeOperationState operationState(boolean loading) {
        if (loading) {
            return SiegeOperationState.LOADING;
        }
        if (cooldown() > 0) {
            return SiegeOperationState.COOLDOWN;
        }
        if (!hasAmmunition()) {
            return SiegeOperationState.IDLE;
        }
        if (!windingComplete()) {
            return SiegeOperationState.WINDING;
        }
        return SiegeOperationState.READY;
    }

    public interface Host {
        int cooldown();
        void setCooldown(int ticks);
        int loadStage();
        void setLoadStage(int stage);
        String ammunition();
        void setAmmunition(String ammunition);
        int scattershotCount();
        void setScattershotCount(int count);
        String automatedAmmunitionMode();
        void setAutomatedAmmunitionMode(String mode);
        int windingTime();
        void setWindingTime(int ticks);
        int windingTotal();
        void setWindingTotal(int ticks);
        float shotPower();
        void setShotPower(float power);
        boolean attackHappened();
        void setAttackHappened(boolean happened);
    }
}
