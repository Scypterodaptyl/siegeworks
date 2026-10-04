package me.mss1r.siegeworks.gameplay.construction;

import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionHitTesting;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class SiegeConstructionController {
    private static final String TAG_BUILD = "Construction";

    public interface Host {
        AbstractSiegeEntity siege();

        void syncProgress(int builtStages, String blueprintId, int hits, int stageHits);

        int syncedBuildHits();

        int syncedBuildStageHits();

        int syncedBuiltSections();

        String syncedBlueprintId();
    }

    private final Host host;
    private final BuildProgress progress = BuildProgress.finished();

    public SiegeConstructionController(Host host) {
        this.host = host;
    }

    public BuildProgress progress() {
        return progress;
    }

    public void save(CompoundTag root) {
        CompoundTag construction = new CompoundTag();
        progress.save(construction);
        root.put(TAG_BUILD, construction);
    }

    public void load(CompoundTag root) {
        if (!root.contains(TAG_BUILD)) {
            return;
        }
        progress.load(root.getCompound(TAG_BUILD));
        syncProgress();
    }

    public void orientForPlacement(float yaw) {
        siege().setTrackedYaw(yaw);
        holdOrientation();
    }

    public void holdOrientation() {
        AbstractSiegeEntity siege = siege();
        float yaw = siege.getTrackedYaw();
        siege.setYRot(yaw);
        siege.yRotO = yaw;
        siege.setYHeadRot(yaw);
        siege.yHeadRotO = yaw;
        siege.setYBodyRot(yaw);
        siege.yBodyRotO = yaw;
        siege.lastRiderYaw = yaw;
    }

    public void syncProgress() {
        BlueprintConstructionPlan.Stage stage = progress.currentStage();
        host.syncProgress(progress.builtStages(), progress.blueprintId(), progress.hits(),
                stage == null ? 0 : stage.hits());
    }

    public boolean acceptsBlow(Player builder, String section) {
        return ConstructionHitTesting.accepts(builder, siege(), modelIdentifier(), section);
    }

    public int buildHits() {
        return host.syncedBuildHits();
    }

    public int buildStageHits() {
        return host.syncedBuildStageHits();
    }

    public int builtSections() {
        return host.syncedBuiltSections();
    }

    public String blueprintId() {
        return host.syncedBlueprintId();
    }

    public boolean fullyBuilt() {
        return siege().level().isClientSide() ? builtSections() == Integer.MAX_VALUE : progress.complete();
    }

    private ResourceLocation modelIdentifier() {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(siege().getType());
        return type == null ? MinecraftVersionCompat.id("siegeworks", "unknown") : type;
    }

    private AbstractSiegeEntity siege() {
        return host.siege();
    }
}
