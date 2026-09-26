package me.mss1r.siegeworks.client;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.aim.SiegeAimOverlay;
import me.mss1r.siegeworks.client.particle.ImpactSmokePlumeParticle;
import me.mss1r.siegeworks.client.particle.MuzzlePlumeParticle;
import me.mss1r.siegeworks.client.particle.SiegeSmokeParticle;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
*///?} else {
import net.neoforged.api.distmarker.Dist;
//?}
//? if forge {
/*import net.minecraftforge.eventbus.api.SubscribeEvent;
*///?} else {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if forge {
/*import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
*///?} else {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
import java.util.List;
import me.mss1r.siegeworks.client.harness.HarnessLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? if forge {
/*import net.minecraftforge.client.event.EntityRenderersEvent;
*///?} else {
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//?}
//? if forge {
/*import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
*///?} else {
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
//?}
//? if forge {
/*import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
*///?} else {
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
//?}

@SuppressWarnings("removal")
@EventBusSubscriber(
   modid = Siegeworks.MOD_ID,
   bus = EventBusSubscriber.Bus.MOD,
   value = {Dist.CLIENT}
)
public final class SiegeworksClientModEvents {
   private SiegeworksClientModEvents() {
   }

   @SubscribeEvent
   public static void addHarnessLayer(EntityRenderersEvent.AddLayers event) {
      for (EntityType<? extends AbstractHorse> type : List.of(
              EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
              EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE,
              EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.CAMEL)) {
         LivingEntityRenderer<AbstractHorse, EntityModel<AbstractHorse>> renderer = event.getRenderer(type);
         if (renderer instanceof LivingEntityRenderer<?, ?>) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            LivingEntityRenderer raw = renderer;
            raw.addLayer(new HarnessLayer<>(raw));
         }
      }
   }

   @SubscribeEvent
   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      Siegeworks.LOG.info("Registering Siegeworks particle providers");
      event.registerSpriteSet(SiegeworksParticles.SIEGE_SMOKE.get(),
              sprites -> new SiegeSmokeParticle.Provider(sprites, false));
      event.registerSpriteSet(SiegeworksParticles.HEAVY_SIEGE_SMOKE.get(),
              sprites -> new SiegeSmokeParticle.Provider(sprites, true));
      event.registerSpecial(SiegeworksParticles.MUZZLE_PLUME.get(),
              new MuzzlePlumeParticle.Provider());
      event.registerSpecial(SiegeworksParticles.IMPACT_SMOKE_PLUME.get(),
              new ImpactSmokePlumeParticle.Provider());
   }

   @SubscribeEvent
   //? if forge {
   /*public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "siege_aim",
              (forgeGui, graphics, partialTick, width, height) ->
                      SiegeAimOverlay.render(graphics, partialTick, width, height));
   }
   *///?} else {
   public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
      event.registerAbove(VanillaGuiLayers.CROSSHAIR,
              ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "siege_aim"),
              (graphics, deltaTracker) -> SiegeAimOverlay.render(
                      graphics,
                      deltaTracker.getGameTimeDeltaPartialTick(false),
                      graphics.guiWidth(),
                      graphics.guiHeight()));
   }
   //?}

}
