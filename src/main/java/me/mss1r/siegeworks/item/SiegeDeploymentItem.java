package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.client.item.SiegeDeploymentItemRenderer;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
//? if forge {
/*import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*///?} else {
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
//?}

public class SiegeDeploymentItem extends Item {
    private final Supplier<? extends EntityType<?>> typeSupplier;

    public SiegeDeploymentItem(Supplier<? extends EntityType<?>> typeSupplier, Properties properties) {
        super(properties);
        this.typeSupplier = Objects.requireNonNull(typeSupplier, "typeSupplier");
    }

    @Override
    public final InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        EntityType<?> type = entityType();
        InteractionResult customValidation = validatePlacement(serverLevel, context, type);
        if (customValidation != InteractionResult.SUCCESS) {
            return customValidation;
        }

        SiegeDeploymentLimits.Deployment deployment = SiegeDeploymentLimits.resolve(
                serverLevel, context.getItemInHand(), context.getPlayer());
        if (!SiegeDeploymentLimits.canDeployWithFeedback(
                serverLevel, type, deployment, context.getPlayer())) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos().above();
        Entity entity = type.create(serverLevel);
        if (entity == null) {
            return InteractionResult.PASS;
        }

        float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        applySpawnRotation(entity, yaw);
        if (entity instanceof AbstractSiegeEntity siege && deployment != null) {
            siege.setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
        }
        configureSpawnedEntity(entity, context);
        if (!serverLevel.addFreshEntity(entity)) {
            return InteractionResult.FAIL;
        }
        if (entity instanceof AbstractSiegeEntity siege) {
            SiegeDeploymentLimits.register(siege);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    public final EntityType<?> entityType() {
        return Objects.requireNonNull(typeSupplier.get(), "Supplied siege entity type");
    }

    protected InteractionResult validatePlacement(ServerLevel level, UseOnContext context,
                                                  EntityType<?> type) {
        return InteractionResult.SUCCESS;
    }

    protected void configureSpawnedEntity(Entity entity, UseOnContext context) {
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SiegeDeploymentItemRenderer.clientExtensions());
    }

    private static void applySpawnRotation(Entity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.yRotO = yaw;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;

        if (entity instanceof AbstractSiegeEntity siege) {
            siege.setTrackedYaw(yaw);
            siege.setTrackedPitch(0.0F);
        }
    }
}
