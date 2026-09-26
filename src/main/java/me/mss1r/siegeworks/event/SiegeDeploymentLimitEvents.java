package me.mss1r.siegeworks.event;

import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.api.event.BlueprintRecipeCheckEvent;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class SiegeDeploymentLimitEvents {
    private SiegeDeploymentLimitEvents() {
    }

    public static void register() {
        BlueprintEvents.RECIPE_CHECK.register(SiegeDeploymentLimitEvents::checkBlueprintDeployment);
    }

    public static void checkBlueprintDeployment(BlueprintRecipeCheckEvent event) {
        if (event.recipe == null || !event.recipe.buildsInWorld()
                || event.recipe.result == null || event.recipe.result.item == null) {
            return;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(event.recipe.result.item);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return;
        }
        Item resultItem = BuiltInRegistries.ITEM.get(itemId);
        if (!(resultItem instanceof SiegeDeploymentItem spawnerItem)) {
            return;
        }

        EntityType<?> type = spawnerItem.entityType();
        SiegeDeploymentLimits.Deployment deployment =
                SiegeDeploymentLimits.forOwner(event.player.serverLevel(), event.player.getUUID());
        if (!SiegeDeploymentLimits.canDeployWithFeedback(
                event.player.serverLevel(), type, deployment, event.player)) {
            event.deny();
        }
    }
}
