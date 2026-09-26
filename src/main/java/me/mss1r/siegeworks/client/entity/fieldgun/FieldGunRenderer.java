package me.mss1r.siegeworks.client.entity.fieldgun;

import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class FieldGunRenderer<T extends AbstractFieldGunEntity> extends TowedSiegeRenderer<T> {
    public FieldGunRenderer(EntityRendererProvider.Context context, String assetName) {
        super(context, new FieldGunModel<>(assetName));
    }
}
