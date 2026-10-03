package me.mss1r.siegeworks.network;

import com.mojang.serialization.Codec;
import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if neoforge {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.Map;

/** The server's projectile profiles, so clients fly predicted projectiles the way the server does. */
//? if forge {
/*
public record ProjectileProfilesS2CPayload(Map<ResourceLocation, ProjectilePhysicsProfile> profiles) {
*///?} else {
public record ProjectileProfilesS2CPayload(Map<ResourceLocation, ProjectilePhysicsProfile> profiles)
        implements CustomPacketPayload {
    public static final Type<ProjectileProfilesS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "projectile_profiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectileProfilesS2CPayload> STREAM_CODEC =
            StreamCodec.ofMember(ProjectileProfilesS2CPayload::write, ProjectileProfilesS2CPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<ProjectileProfilesS2CPayload> type() { return TYPE; }
    //?}
    private static final Codec<Map<ResourceLocation, ProjectilePhysicsProfile>> PROFILES =
            Codec.unboundedMap(ResourceLocation.CODEC, ProjectilePhysicsProfile.CODEC);

    public static ProjectileProfilesS2CPayload current() {
        return new ProjectileProfilesS2CPayload(SiegeProfileCatalogs.PROJECTILES.snapshot());
    }

    public static void encode(ProjectileProfilesS2CPayload packet, FriendlyByteBuf buffer) {
        buffer.writeNbt((CompoundTag) PROFILES.encodeStart(NbtOps.INSTANCE, packet.profiles).result()
                .orElseThrow(() -> new IllegalStateException("Projectile profiles could not be encoded")));
    }

    public static ProjectileProfilesS2CPayload decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        Map<ResourceLocation, ProjectilePhysicsProfile> profiles = tag == null
                ? Map.of()
                : PROFILES.parse(NbtOps.INSTANCE, tag).result().orElse(Map.of());
        return new ProjectileProfilesS2CPayload(profiles);
    }

    public static void handle(ProjectileProfilesS2CPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> SiegeProfileCatalogs.PROJECTILES.acceptFromServer(packet.profiles));
    }
}
