package me.mss1r.siegeworks.gameplay.maintenance;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Repair and dismantle costs, derived from the blueprint as loaded by Axiomata. Tag materials count as the tag's first
 * item.
 */
public final class SiegeMaintenanceData {
    private static final int HITS_PER_RESOURCE = 4;
    private static final String LADDER_BLUEPRINT = "siegeworks:siege_ladder";
    private static final Map<String, String> ENTITY_TO_SPAWNER = Map.ofEntries(
            Map.entry("siegeworks:serpentine", "siegeworks:serpentine_spawner"),
            Map.entry("siegeworks:culverin", "siegeworks:culverin_spawner"),
            Map.entry("siegeworks:battering_ram", "siegeworks:battering_ram_spawner"),
            Map.entry("siegeworks:mangonel", "siegeworks:mangonel_spawner"),
            Map.entry("siegeworks:trebuchet", "siegeworks:trebuchet_spawner"),
            Map.entry("siegeworks:tower_crossbow", "siegeworks:tower_crossbow_spawner"),
            Map.entry("siegeworks:arcballista", "siegeworks:arcballista_spawner"),
            Map.entry("siegeworks:mantlet", "siegeworks:mantlet_spawner"),
            Map.entry("siegeworks:mons_meg", "siegeworks:mons_meg_spawner"),
            Map.entry("siegeworks:siege_tower", "siegeworks:siege_tower_spawner"),
            Map.entry("siegeworks:siege_ladder", "siegeworks:siege_ladder_spawner")
    );

    private SiegeMaintenanceData() {
    }

    public static MaintenanceRecipe forSiege(AbstractSiegeEntity siege) {
        BlueprintDefinition recipe = findRecipe(siege);
        if (recipe == null) {
            return MaintenanceRecipe.empty();
        }

        // A ladder only cost its base and the sections it has.
        int stages = siege instanceof SiegeLadderEntity ladder ? 1 + ladder.getSections() : Integer.MAX_VALUE;
        Map<ResourceLocation, Integer> ingredients = collectIngredients(recipe, stages);
        int authoredHits = collectConstructionHits(recipe, stages);
        int totalHits = authoredHits > 0
                ? authoredHits
                : Math.max(1, ingredients.values().stream().mapToInt(Integer::intValue).sum()
                        * HITS_PER_RESOURCE);
        if (siege instanceof SiegeTowerEntity tower) {
            tower.getLeatherMaterials().forEach(
                    (item, count) -> ingredients.merge(item, count, Integer::sum));
        }
        return new MaintenanceRecipe(ingredients, totalHits);
    }

    public static Map<ResourceLocation, Integer> repairCost(AbstractSiegeEntity siege) {
        MaintenanceRecipe recipe = forSiege(siege);
        if (recipe.isEmpty()) {
            return Map.of();
        }

        float missingRatio = 1.0F - Math.max(0.0F, Math.min(1.0F, siege.getHealth() / siege.getMaxHealth()));
        if (missingRatio <= 0.001F) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        recipe.ingredients().forEach((item, count) -> result.put(item, Math.max(1, (int) Math.ceil(count * missingRatio))));
        return result;
    }

    public static Map<ResourceLocation, Integer> dismantleRefund(AbstractSiegeEntity siege) {
        MaintenanceRecipe recipe = forSiege(siege);
        if (recipe.isEmpty()) {
            return Map.of();
        }

        float healthRatio = Math.max(0.0F, Math.min(1.0F, siege.getHealth() / siege.getMaxHealth()));
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        recipe.ingredients().forEach((item, count) -> {
            int refund = (int) Math.floor(count * healthRatio * 0.5F);
            if (refund > 0) {
                result.put(item, refund);
            }
        });
        return result;
    }

    public static String formatItems(Map<ResourceLocation, Integer> items) {
        if (items.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        items.forEach((id, count) -> {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            String name = item == null ? id.toString() : item.getDefaultInstance().getHoverName().getString();
            builder.append(name).append(" x").append(count);
        });
        return builder.toString();
    }

    public static int countResources(AbstractSiegeEntity siege) {
        return forSiege(siege).ingredients().values().stream().mapToInt(Integer::intValue).sum();
    }

    public static int dismantleRequiredHits(AbstractSiegeEntity siege) {
        MaintenanceRecipe recipe = forSiege(siege);
        return recipe.isEmpty() ? 0 : Math.max(1, (int) Math.ceil(recipe.requiredHits() * 0.5D));
    }

    @Nullable
    private static BlueprintDefinition findRecipe(AbstractSiegeEntity siege) {
        if (siege instanceof SiegeLadderEntity) {
            BlueprintDefinition ladder = BlueprintDefinitions.get(LADDER_BLUEPRINT);
            if (ladder != null) {
                return ladder;
            }
        }
        String spawnerId = ENTITY_TO_SPAWNER.get(BuiltInRegistries.ENTITY_TYPE.getKey(siege.getType()).toString());
        if (spawnerId == null) {
            return null;
        }
        // Use the first matching blueprint by id so the result doesn't depend on load order.
        for (Map.Entry<String, BlueprintDefinition> entry : new TreeMap<>(BlueprintDefinitions.allById()).entrySet()) {
            if (spawnerId.equals(entry.getValue().result().item().toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<ResourceLocation, Integer> collectIngredients(BlueprintDefinition recipe, int stages) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        for (BlueprintDefinition.Stage stage : recipe.stages().subList(0, Math.min(stages, recipe.stageCount()))) {
            for (BlueprintDefinition.Material material : stage.materials()) {
                Item shown = material.displayStack().getItem();
                result.merge(BuiltInRegistries.ITEM.getKey(shown), material.count(), Integer::sum);
            }
        }
        return result;
    }

    private static int collectConstructionHits(BlueprintDefinition recipe, int stages) {
        return recipe.stages().stream()
                .limit(stages)
                .mapToInt(stage -> Math.max(0, stage.hits()))
                .sum();
    }

    public record MaintenanceRecipe(Map<ResourceLocation, Integer> ingredients, int requiredHits) {
        private static MaintenanceRecipe empty() {
            return new MaintenanceRecipe(Map.of(), 0);
        }

        public boolean isEmpty() {
            return ingredients.isEmpty();
        }
    }
}
