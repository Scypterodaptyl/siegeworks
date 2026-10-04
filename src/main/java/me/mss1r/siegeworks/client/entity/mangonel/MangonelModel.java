package me.mss1r.siegeworks.client.entity.mangonel;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class MangonelModel extends TowedSiegeModel<MangonelEntity> {
    @Override
    public ResourceLocation getModelResource(MangonelEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/mangonel.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MangonelEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/mangonel.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MangonelEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/mangonel.animation.json");
    }

    @Override
    public void setCustomAnimations(MangonelEntity animatable, long instanceId, AnimationState<MangonelEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        String ammo = animatable.getAmmoLoaded();
        getBone("load").ifPresent(geoBone -> geoBone.setHidden(
                !SiegeAmmo.isStoneAmmoKey(ammo) && !SiegeAmmo.isGrapeshotAmmoKey(ammo)));
        // The pot is drawn as its own block on the load bone; this bone of the mangonel's has no pot texture.
        getBone("load_fire").ifPresent(geoBone -> geoBone.setHidden(true));
    }
}
