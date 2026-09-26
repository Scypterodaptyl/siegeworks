package me.mss1r.siegeworks.client.harness;

import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public enum MountHarness {
    HORSE("horse_harness", 16.0F, 7.0F, 3.0F, -8.0F, 2.0F, -7.5F,
            10.0F, 6.0F, -5.0F, 3.0F, 7.0D, 17.0D, -6.0D),
    CAMEL("camel_harness", 21.0F, 7.0F, 3.0F, -10.5F, -9.0F, -12.5F,
            15.0F, 6.0F, -7.5F, -8.0F, 9.5D, 28.0D, -11.0D),
    LLAMA("llama_harness", 18.0F, 7.0F, 3.0F, -9.0F, 1.0F, -5.5F,
            12.0F, 6.0F, -6.0F, 2.0F, 8.0D, 18.0D, -4.0D);

    private final ResourceLocation texture;
    private final float outerWidth, outerHeight, depth, outerX, outerY, z;
    private final float innerWidth, innerHeight, innerX, innerY;
    private final Vec3 leftAnchor, rightAnchor;
    private ModelPart baked;

    MountHarness(String name, float outerWidth, float outerHeight, float depth,
                 float outerX, float outerY, float z,
                 float innerWidth, float innerHeight, float innerX, float innerY,
                 double anchorX, double anchorY, double anchorZ) {
        this.texture = ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "textures/entity/" + name + ".png");
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.depth = depth;
        this.outerX = outerX;
        this.outerY = outerY;
        this.z = z;
        this.innerWidth = innerWidth;
        this.innerHeight = innerHeight;
        this.innerX = innerX;
        this.innerY = innerY;
        this.leftAnchor = new Vec3(-anchorX / 16.0D, anchorY / 16.0D, anchorZ / 16.0D);
        this.rightAnchor = new Vec3(anchorX / 16.0D, anchorY / 16.0D, anchorZ / 16.0D);
    }

    public static MountHarness forMount(LivingEntity mount) {
        EntityType<?> type = mount.getType();
        if (type == EntityType.CAMEL) {
            return CAMEL;
        }
        if (type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA) {
            return LLAMA;
        }
        return HORSE;
    }

    public static float mountScale(LivingEntity mount) {
        EntityType<?> type = mount.getType();
        float breed = type == EntityType.DONKEY ? 0.87F : type == EntityType.MULE ? 0.92F : 1.0F;
        return breed * mount.getScale();
    }

    public ResourceLocation texture() {
        return texture;
    }

    public Vec3 leftAnchor() {
        return leftAnchor;
    }

    public Vec3 rightAnchor() {
        return rightAnchor;
    }

    public ModelPart model() {
        if (baked == null) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("harness", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(outerX, outerY, z, outerWidth, outerHeight, depth)
                            .texOffs(0, 10).addBox(innerX, innerY, z, innerWidth, innerHeight, depth),
                    PartPose.ZERO);
            baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }
        return baked;
    }
}
