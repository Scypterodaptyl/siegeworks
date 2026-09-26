package me.mss1r.siegeworks.client.entity.fieldgun;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public final class FieldGunModel<T extends AbstractFieldGunEntity> extends TowedSiegeModel<T> {
    private final ResourceLocation modelResource;
    private final ResourceLocation textureResource;
    private final ResourceLocation animationResource;

    public FieldGunModel(String assetName) {
        this.modelResource = ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "geo/" + assetName + ".geo.json");
        this.textureResource = ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "textures/entity/" + assetName + ".png");
        this.animationResource = ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "animations/" + assetName + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return modelResource;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animationResource;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        animatable.updateRenderedAim(animationState.getPartialTick());

        getBone("base").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedTowBasePitch())));
        getBone("cannon_aim").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedModelPitch())));
    }
}
