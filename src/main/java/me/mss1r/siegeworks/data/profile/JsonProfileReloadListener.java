package me.mss1r.siegeworks.data.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class JsonProfileReloadListener<T>
        extends SimplePreparableReloadListener<JsonProfileReloadListener.PreparedProfiles<T>> {
    private final String directory;
    private final String label;
    private final Codec<T> codec;
    private final Function<T, Optional<String>> validator;
    private final ProfileCatalog<T> destination;

    public JsonProfileReloadListener(String directory, String label, Codec<T> codec,
                                     Function<T, Optional<String>> validator,
                                     ProfileCatalog<T> destination) {
        this.directory = directory;
        this.label = label;
        this.codec = codec;
        this.validator = validator;
        this.destination = destination;
    }

    @Override
    protected PreparedProfiles<T> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> profiles = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        Map<ResourceLocation, Resource> resources =
                resourceManager.listResources(directory, id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            ResourceLocation profileId = profileId(resourceId);
            if (profileId == null) {
                errors.add("Invalid " + label + " resource path: " + resourceId);
                continue;
            }

            try (Reader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                List<String> decodeErrors = new ArrayList<>();
                DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                Optional<T> decoded = result.resultOrPartial(decodeErrors::add);

                if (!decodeErrors.isEmpty()) {
                    decodeErrors.forEach(message -> errors.add(resourceId + ": " + message));
                    continue;
                }
                if (decoded.isEmpty()) {
                    errors.add(resourceId + ": codec returned no profile");
                    continue;
                }

                T profile = decoded.get();
                Optional<String> validationError = validator.apply(profile);
                if (validationError.isPresent()) {
                    errors.add(resourceId + ": " + validationError.get());
                    continue;
                }
                profiles.put(profileId, profile);
            } catch (Exception exception) {
                errors.add(resourceId + ": " + exception.getMessage());
                Siegeworks.LOG.debug("Failed to prepare {} from {}", label, resourceId, exception);
            }
        }

        return new PreparedProfiles<>(Map.copyOf(profiles), List.copyOf(errors));
    }

    @Override
    protected void apply(PreparedProfiles<T> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (!prepared.errors().isEmpty()) {
            prepared.errors().forEach(message -> Siegeworks.LOG.error("Invalid {}: {}", label, message));
            Siegeworks.LOG.error("Rejected {} reload with {} error(s); keeping the previous snapshot",
                    label, prepared.errors().size());
            return;
        }

        destination.publish(prepared.profiles());
        Siegeworks.LOG.info("Loaded {} {}", prepared.profiles().size(), label);
    }

    private ResourceLocation profileId(ResourceLocation resourceId) {
        String prefix = directory + "/";
        String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        return ResourceLocation.tryBuild(
                resourceId.getNamespace(),
                path.substring(prefix.length(), path.length() - ".json".length())
        );
    }

    public record PreparedProfiles<T>(Map<ResourceLocation, T> profiles, List<String> errors) {
    }
}
