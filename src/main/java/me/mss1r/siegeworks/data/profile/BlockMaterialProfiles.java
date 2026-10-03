package me.mss1r.siegeworks.data.profile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BlockMaterialProfiles {
    public static final ProfileCatalog<BlockMaterialProfile> CATALOG = new ProfileCatalog<>(
            new BlockMaterialProfile(Optional.empty(), Optional.empty(), 0,
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    private static volatile List<Rule> rules = List.of();

    static {
        CATALOG.onPublish(() -> rules = CATALOG.snapshot().entrySet().stream()
                .map(entry -> new Rule(entry.getKey(), entry.getValue()))
                .sorted(Comparator.<Rule>comparingInt(rule -> rule.profile().block().isPresent() ? 0 : 1)
                        .thenComparing(Comparator.comparingInt((Rule rule) -> rule.profile().priority()).reversed())
                        .thenComparing(rule -> rule.id().toString()))
                .toList());
    }

    private BlockMaterialProfiles() {
    }

    public static Optional<BlockMaterialProfile> forState(BlockState state) {
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for (Rule rule : rules) {
            BlockMaterialProfile profile = rule.profile();
            if (profile.block().filter(block::equals).isPresent()
                    || profile.tag().filter(tag -> state.is(TagKey.create(Registries.BLOCK, tag))).isPresent()) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public static double resistance(BlockState state) {
        return forState(state).flatMap(BlockMaterialProfile::projectileResistance)
                .orElseGet(() -> (double) state.getBlock().getExplosionResistance());
    }

    public static void reset() {
        rules = List.of();
        CATALOG.reset();
    }

    private record Rule(ResourceLocation id, BlockMaterialProfile profile) {
    }
}
