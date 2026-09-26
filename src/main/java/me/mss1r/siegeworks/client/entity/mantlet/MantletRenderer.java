package me.mss1r.siegeworks.client.entity.mantlet;

import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MantletRenderer extends TowedSiegeRenderer<MantletEntity> {
    public MantletRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MantletModel());
    }
}
