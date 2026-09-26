package me.mss1r.siegeworks.client.entity.batteringram;

import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class BatteringRamRenderer extends TowedSiegeRenderer<BatteringRamEntity> {
    public BatteringRamRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BatteringRamModel());
    }
}
