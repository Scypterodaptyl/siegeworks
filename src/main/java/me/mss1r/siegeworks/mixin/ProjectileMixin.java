package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.api.ProjectilePassThroughControl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void siegeworks$usePassThroughGeometryForVehicle(Entity target,
                                                              CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof ProjectilePassThroughControl && target.canBeHitByProjectile()) {
            callback.setReturnValue(true);
        }
    }
}
