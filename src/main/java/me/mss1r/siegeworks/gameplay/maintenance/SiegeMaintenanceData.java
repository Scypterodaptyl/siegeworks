package me.mss1r.siegeworks.gameplay.maintenance;

import dev.architectury.registry.ReloadListenerRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SiegeMaintenanceData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type BLUEPRINT_FILE_TYPE = new TypeToken<List<BlueprintRecipe>>() {}.getType();
    private static final String BLUEPRINT_DIRECTORY = "blueprints";
    private static final String BLUEPRINT_PREFIX = BLUEPRINT_DIRECTORY + "/";
    private static final int HITS_PER_RESOURCE = 4;
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

    private static volatile Map<String, BlueprintRecipe> recipesById = Map.of();
    private static volatile Map<String, BlueprintRecipe> recipesByResult = Map.of();

    private SiegeMaintenanceData() {
    }

    public static void registerReloadListener() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new Loader(),
                MinecraftVersionCompat.id(Siegeworks.MOD_ID, "maintenance_blueprints"));
    }

    public static MaintenanceRecipe forSiege(AbstractSiegeEntity siege) {
        BlueprintRecipe recipe = findRecipe(siege);
        if (recipe == null) {
            return MaintenanceRecipe.empty();
        }

        // A ladder is its base and as many sections as it has: only those stages of its build went into it.
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
    private static BlueprintRecipe findRecipe(AbstractSiegeEntity siege) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(siege.getType()).toString();
        if (siege instanceof SiegeLadderEntity) {
            BlueprintRecipe ladderRecipe = recipesById.get("siege_ladder");
            if (ladderRecipe != null) {
                return ladderRecipe;
            }
        }

        String spawnerId = ENTITY_TO_SPAWNER.get(entityId);
        return spawnerId == null ? null : recipesByResult.get(spawnerId);
    }

    private static Map<ResourceLocation, Integer> collectIngredients(BlueprintRecipe recipe, int stages) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        if (recipe.key == null) {
            return result;
        }

        if (recipe.construction != null && !recipe.construction.isEmpty()) {
            for (ConstructionStageSpec stage : recipe.construction.subList(0,
                    Math.min(stages, recipe.construction.size()))) {
                if (stage == null || stage.materials == null) {
                    continue;
                }
                for (ConstructionMaterialSpec material : stage.materials) {
                    if (material == null || material.key == null) {
                        continue;
                    }
                    IngredientSpec ingredient = recipe.key.get(material.key);
                    mergeIngredient(result, ingredient, material.count);
                }
            }
            return result;
        }

        if (recipe.pattern == null) {
            return result;
        }

        for (String row : recipe.pattern) {
            if (row == null) {
                continue;
            }
            for (int i = 0; i < row.length(); i++) {
                char symbol = row.charAt(i);
                if (symbol == ' ') {
                    continue;
                }

                IngredientSpec ingredient = recipe.key.get(String.valueOf(symbol));
                mergeIngredient(result, ingredient, ingredient == null ? 0 : ingredient.count);
            }
        }
        return result;
    }

    private static void mergeIngredient(Map<ResourceLocation, Integer> result,
                                        @Nullable IngredientSpec ingredient, int count) {
        ResourceLocation itemId = ingredient == null ? null : ResourceLocation.tryParse(ingredient.item);
        if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
            result.merge(itemId, Math.max(1, count), Integer::sum);
        }
    }

    private static int collectConstructionHits(BlueprintRecipe recipe, int stages) {
        if (recipe.construction == null) {
            return 0;
        }
        return recipe.construction.stream()
                .limit(stages)
                .filter(stage -> stage != null)
                .mapToInt(stage -> Math.max(0, stage.hits))
                .sum();
    }

    @Nullable
    private static BlueprintRecipe readBlueprint(ResourceLocation location, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || root.isJsonNull()) {
                return null;
            }
            if (root.isJsonArray()) {
                List<BlueprintRecipe> recipes = GSON.fromJson(root, BLUEPRINT_FILE_TYPE);
                return recipes == null || recipes.isEmpty() ? null : recipes.get(0);
            }
            if (root.isJsonObject()) {
                return GSON.fromJson(root, BlueprintRecipe.class);
            }
            return null;
        } catch (Exception exception) {
            LOGGER.warn("Could not read maintenance blueprint {}", location, exception);
            return null;
        }
    }

    private static String blueprintId(ResourceLocation location) {
        String path = location.getPath();
        return path.substring(BLUEPRINT_PREFIX.length(), path.length() - ".json".length());
    }

    public record MaintenanceRecipe(Map<ResourceLocation, Integer> ingredients, int requiredHits) {
        private static MaintenanceRecipe empty() {
            return new MaintenanceRecipe(Map.of(), 0);
        }

        public boolean isEmpty() {
            return ingredients.isEmpty();
        }
    }

    private static final class BlueprintRecipe {
        private String[] pattern;
        private Map<String, IngredientSpec> key = new LinkedHashMap<>();
        private List<ConstructionStageSpec> construction = List.of();
        private ResultSpec result;
    }

    private static final class IngredientSpec {
        private String item;
        private int count = 1;
    }

    private static final class ResultSpec {
        private String item;
    }

    private static final class ConstructionStageSpec {
        private int hits;
        private List<ConstructionMaterialSpec> materials = List.of();
    }

    private static final class ConstructionMaterialSpec {
        private String key;
        private int count = 1;
    }

    private record Catalog(Map<String, BlueprintRecipe> byId, Map<String, BlueprintRecipe> byResult) {
    }

    private static final class Loader extends SimplePreparableReloadListener<Catalog> {
        @Override
        protected Catalog prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, BlueprintRecipe> byId = new LinkedHashMap<>();
            Map<String, BlueprintRecipe> byResult = new LinkedHashMap<>();
            resourceManager.listResources(BLUEPRINT_DIRECTORY, location ->
                            location.getNamespace().equals("siegeworks")
                                    && location.getPath().startsWith(BLUEPRINT_PREFIX)
                                    && location.getPath().endsWith(".json")
                                    && !location.getPath().endsWith("/index.json"))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        BlueprintRecipe recipe = readBlueprint(entry.getKey(), entry.getValue());
                        if (recipe == null || recipe.result == null || recipe.result.item == null) {
                            return;
                        }
                        String id = blueprintId(entry.getKey());
                        byId.put(id, recipe);
                        byResult.putIfAbsent(recipe.result.item, recipe);
                    });
            return new Catalog(
                    Collections.unmodifiableMap(new LinkedHashMap<>(byId)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(byResult))
            );
        }

        @Override
        protected void apply(Catalog catalog, ResourceManager resourceManager, ProfilerFiller profiler) {
            recipesById = catalog.byId();
            recipesByResult = catalog.byResult();
            LOGGER.info("Loaded {} Siegeworks maintenance blueprints", recipesById.size());
        }
    }
}
