package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The pot a stone thrower holds ready to throw: what it holds, and its fuse once lit. A lit pot not thrown in time
 * bursts where it lies; a thrown one carries its filling and its fuse with it.
 */
public final class EnginePot {
    private static final String TAG_FILLING = "LoadedPot";
    private static final String TAG_FUSE = "PotFuse";

    private final AbstractSiegeEntity engine;
    private final EntityDataAccessor<Integer> fuse;
    private PotFilling filling = PotFilling.EMPTY;

    /** {@code fuse} is the engine's own synced field for the ticks left, defined as {@link IncendiaryFuse#UNLIT}. */
    public EnginePot(AbstractSiegeEntity engine, EntityDataAccessor<Integer> fuse) {
        this.engine = engine;
        this.fuse = fuse;
    }

    /** Takes a pot in, and says what the engine has loaded: a pot with its wick or one without. */
    public String load(PotFilling loaded) {
        filling = loaded;
        setFuse(IncendiaryFuse.UNLIT);
        return loaded.wick() ? SiegeAmmo.AMMO_FIRE : SiegeAmmo.AMMO_POT;
    }

    /** Whether a crew's stores hold a pot sealed with its wick, the only kind a crew loads and lights itself. */
    public static boolean hasSealed(Container inventory) {
        return sealedSlot(inventory) >= 0;
    }

    /** Takes one sealed pot from a crew's stores, or null if there is none. */
    @Nullable
    public static PotFilling takeSealed(Container inventory) {
        int slot = sealedSlot(inventory);
        if (slot < 0) {
            return null;
        }
        ItemStack stack = inventory.getItem(slot);
        PotFilling sealed = PotFilling.of(stack);
        stack.shrink(1);
        inventory.setChanged();
        return sealed;
    }

    private static int sealedSlot(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(SiegeworksItems.FIRE_PROJECTILE.get()) && PotFilling.of(stack).canLight()) {
                return slot;
            }
        }
        return -1;
    }

    /** What the loaded pot holds; one loaded before pots were filled by hand holds the standard filling. */
    public PotFilling filling() {
        return filling.isEmpty() && SiegeAmmo.isLightablePotKey(engine.getAmmoLoaded()) ? PotFilling.standard()
                : filling;
    }

    public int fuseTicks() {
        return engine.getEntityData().get(fuse);
    }

    public boolean isLit() {
        return fuseTicks() >= 0;
    }

    private void setFuse(int ticks) {
        engine.getEntityData().set(fuse, ticks);
    }

    /** Whether a sealed pot lies ready to throw, waiting to be lit. Known on both sides, from the synced load. */
    public boolean awaitsFlame() {
        return SiegeAmmo.isLightablePotKey(engine.getAmmoLoaded()) && engine.isWindingComplete() && !isLit();
    }

    /** Lights the pot with the flint and steel a player holds, the fuse's tip being at {@code fuseTip}. */
    public boolean light(Player player, InteractionHand hand, Vec3 fuseTip) {
        if (!IncendiaryFuse.canStrike(player.getItemInHand(hand)) || !awaitsFlame()) {
            return false;
        }
        IncendiaryFuse.strike(player, hand, engine.level(), fuseTip);
        setFuse(IncendiaryFuse.fullLength());
        return true;
    }

    /** The crew lights its own pot before the throw. */
    public void lightByCrew() {
        if (awaitsFlame()) {
            setFuse(IncendiaryFuse.fullLength());
        }
    }

    /** Burns the fuse down a tick; a pot not thrown in time bursts where it lies, at {@code where}. */
    public void burn(ServerLevel level, Supplier<Vec3> where, EntityType<TrebuchetProjectile> type,
                     ResourceLocation profile) {
        int left = fuseTicks();
        if (left < 0) {
            return;
        }
        if (!SiegeAmmo.isFireAmmoKey(engine.getAmmoLoaded())) {
            setFuse(IncendiaryFuse.UNLIT);
            return;
        }
        if (left > 0) {
            setFuse(left - 1);
            return;
        }
        setFuse(IncendiaryFuse.UNLIT);
        PotFilling burning = filling();
        Vec3 at = where.get();
        engine.setAmmoLoaded("");
        filling = PotFilling.EMPTY;
        IncendiaryFuse.burst(level, at, burning, 0, type, profile, engine.getBaseDamage(), engine, null);
    }

    /** Hands the pot to the shot that throws it, still burning if it was lit. */
    public void throwWith(TrebuchetProjectile projectile) {
        projectile.setFilling(filling());
        if (isLit()) {
            projectile.lightFuse(fuseTicks());
        }
        setFuse(IncendiaryFuse.UNLIT);
        filling = PotFilling.EMPTY;
    }

    /** Sparks off a burning fuse, on the client; it runs from {@code tip} to {@code base}. */
    public void sparkle(Level level, Vec3 tip, Vec3 base) {
        if (isLit()) {
            IncendiaryFuse.sparkle(level, tip, base, IncendiaryFuse.burnt(fuseTicks()));
        }
    }

    public void save(CompoundTag tag) {
        tag.put(TAG_FILLING, filling.save());
        tag.putInt(TAG_FUSE, fuseTicks());
    }

    public void load(CompoundTag tag) {
        filling = tag.contains(TAG_FILLING) ? PotFilling.load(tag.getCompound(TAG_FILLING)) : PotFilling.EMPTY;
        setFuse(tag.contains(TAG_FUSE) ? tag.getInt(TAG_FUSE) : IncendiaryFuse.UNLIT);
    }
}
