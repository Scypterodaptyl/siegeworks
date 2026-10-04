package me.mss1r.siegeworks.client.entity.batteringram;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;

public class BatteringRamModel extends TowedSiegeModel<BatteringRamEntity> {
    @Override
    public ResourceLocation getModelResource(BatteringRamEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/battering_ram.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BatteringRamEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/battering_ram.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BatteringRamEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/battering_ram.animation.json");
    }

    @Override
    protected String leftWheelBone() {
        return "rotate";
    }

    @Override
    protected String rightWheelBone() {
        return "rotate2";
    }

    @Override
    protected boolean shouldRotateWheels(BatteringRamEntity animatable) {
        return animatable.getCooldown() == 0;
    }
}
