package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeAmmunitionControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.MangonelPassengerProjectile;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.gameplay.ballistics.ScattershotVolley;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.ScalarAnimationCurve;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.gameplay.loading.AutomatedLoadingSession;
import me.mss1r.siegeworks.gameplay.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
//? if forge {
/*import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
*///?} else {
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimatableManager;
*///?} else {
import software.bernie.geckolib.animation.AnimatableManager;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.Animation;
*///?} else {
import software.bernie.geckolib.animation.Animation;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationController;
*///?} else {
import software.bernie.geckolib.animation.AnimationController;
//?}
//? if forge {
/*import software.bernie.geckolib.core.object.PlayState;
*///?} else {
import software.bernie.geckolib.animation.PlayState;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.RawAnimation;
*///?} else {
import software.bernie.geckolib.animation.RawAnimation;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MangonelEntity extends AbstractSiegeEntity implements GeoEntity, SiegeAmmunitionControl,
        MountedSiegeItemControl {
    private static final String TAG_LAUNCH_PAYLOAD = "LaunchPayload";
    private static final String TAG_SHOOT_ANIMATION_TICK = "ShootAnimationTick";
    private static final int PROJECTILE_RELEASE_TICK = 3;
    private static final int SHOOT_ANIMATION_TICKS = 18;
    private static final float RELOAD_ANIMATION_TICKS = 100.0F;
    private static final float OPERATOR_VIEW_LIMIT = 35.0F;
    private static final float PAYLOAD_VIEW_LIMIT = 80.0F;
    private static final double UNLOADED_ARM_ANGLE = 0.0D;
    private static final double LOADED_ARM_ANGLE = -60.0D;
    private static final TagKey<Item> PASSENGER_LOADING_ITEMS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, "mangonel_passenger_loading_items"));
    private static final Vec3 ARM_PIVOT = new Vec3(0.0D, 9.0D / 16.0D, -7.0D / 16.0D);
    private static final Vec3 LOAD_CENTER = new Vec3(0.0D, 44.0D / 16.0D, -6.0D / 16.0D);
    private static final Vec3 ROOT_COLLISION_PIVOT = new Vec3(0.0D, 6.0D / 16.0D, 0.0D);
    private static final double LAUNCH_SLOPE = 0.60D;
    private static final double PROJECTILE_GRAVITY = 0.05D;
    private static final Set<SiegeAmmunitionMode> AUTOMATED_AMMUNITION_MODES = Set.of(
            SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD, SiegeAmmunitionMode.INCENDIARY);
    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withReloadSound(SiegeworksSounds.MANGONEL_RELOAD.get())
            .withFiringSound(SiegeworksSounds.MANGONEL_SHOOT.get())
            .withMovementInterval(150)
            .withMovementRange(30.0)
            .withReloadRange(15.0)
            .withReloadVolume(0.6F)
            .withFiringRange(180.0D)
            .withFiringVolume(0.9F);

    private static final LoadingRequirement[] AMMO_LOADS = {
            LoadingRequirement.consume(Items.STONE).timedBy("stone"),
            LoadingRequirement.consume(SiegeworksItems.FIRE_PROJECTILE.get()).timedBy("fireProjectile"),
            LoadingRequirement.consume(SiegeworksItems.GRAPESHOT.get()).timedBy("stone")
    };
    private static final EntityDataAccessor<Optional<UUID>> LAUNCH_PAYLOAD =
            SynchedEntityData.defineId(MangonelEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SHOOT_ANIMATION_TICK =
            SynchedEntityData.defineId(MangonelEntity.class, EntityDataSerializers.INT);
    private static final ScalarAnimationCurve RELOAD_ARM = ScalarAnimationCurve.of(
            key(0.0F, UNLOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.LINEAR),
            key(RELOAD_ANIMATION_TICKS, LOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.LINEAR));
    private static final ScalarAnimationCurve SHOOT_ARM = ScalarAnimationCurve.of(
            key(0.0F, LOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.LINEAR),
            key(5.0F, UNLOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.EASE_IN_ELASTIC));
    private static final ScalarAnimationCurve SHOOT_ROOT = ScalarAnimationCurve.of(
            key(0.0F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR),
            key(2.5F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR),
            key(4.166F, -1.0D, ScalarAnimationCurve.Interpolation.EASE_IN_ELASTIC),
            key(5.834F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR),
            key(17.5F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR));

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AutomatedLoadingSession automatedLoading = new AutomatedLoadingSession();
    private boolean transferringLaunchPayload;
    private final RawAnimation shootAnim = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final RawAnimation reloadingAnim = RawAnimation.begin().thenPlayAndHold("reloading");
    private final RawAnimation loadedAnim = RawAnimation.begin().thenPlayAndHold("loaded");
    private final RawAnimation unloadedAnim = RawAnimation.begin().thenPlayAndHold("unloaded");
    private double previousCollisionRootAngle;
    private double previousCollisionArmAngle = UNLOADED_ARM_ANGLE;

    public MangonelEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.075)
                .add(Attributes.KNOCKBACK_RESISTANCE, 265);
    }

    @Override
    //? if forge {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(LAUNCH_PAYLOAD, Optional.empty());
        data.define(SHOOT_ANIMATION_TICK, -1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(LAUNCH_PAYLOAD).ifPresent(uuid -> tag.putUUID(TAG_LAUNCH_PAYLOAD, uuid));
        tag.putInt(TAG_SHOOT_ANIMATION_TICK, getShootAnimationTick());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(LAUNCH_PAYLOAD,
                tag.hasUUID(TAG_LAUNCH_PAYLOAD) ? Optional.of(tag.getUUID(TAG_LAUNCH_PAYLOAD)) : Optional.empty());
        setShootAnimationTick(tag.contains(TAG_SHOOT_ANIMATION_TICK)
                ? Mth.clamp(tag.getInt(TAG_SHOOT_ANIMATION_TICK), -1, SHOOT_ANIMATION_TICKS - 1)
                : -1);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() { return SOUND_PROFILE; }

    @Override
    protected int getMoveSoundIntervalTicks() {
        return 28;
    }

    @Override
    public Set<SiegeAmmunitionMode> getSupportedAmmunitionModes() {
        return AUTOMATED_AMMUNITION_MODES;
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (isLaunchPayload(player)) return InteractionResult.FAIL;
        if (continueLoadingAction(player)) return showLoadingProgress(player);
        if (getCooldown() > 0) return showCooldownProgress(player);

        ItemStack itemStack = player.getItemInHand(hand);
        boolean hasLaunchLoad = hasLaunchLoad();

        LivingEntity operator = getControllingPassenger();
        if (operator != null && operator != player) return InteractionResult.FAIL;

        if (itemStack.isEmpty() && !player.isShiftKeyDown() && hasLaunchPayload()
                && operator == null && canAddPassenger(player)) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (itemStack.isEmpty() && player.isShiftKeyDown() && hasLaunchLoad && isWindingComplete()) {
            return cycleShotPower(player);
        }

        if (itemStack.isEmpty() && !hasLaunchLoad && canAddPassenger(player) && !player.isShiftKeyDown()) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (!hasLaunchLoad) {
            if (itemStack.is(PASSENGER_LOADING_ITEMS)) {
                LoadingRequirement passengerStage = LoadingRequirement.consume(itemStack.getItem()).timedBy("stone");
                return beginLoadingAction(player, hand, serverLevel, 0, passengerStage);
            }

            LoadingRequirement match = findMatchingAmmo(itemStack);

            if (match == null) {
                showAmmoListMessage(player);
                return InteractionResult.FAIL;
            }

            if (!canBeginLoadingRequirement(player, hand, match)) return InteractionResult.FAIL;
            return beginLoadingAction(player, hand, serverLevel, 0, match);
        }

        if (!isWindingComplete()) {
            return showWindingProgress(player);
        }

        return beginShot(serverLevel, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    public boolean requestRiderFire(ServerPlayer player) {
        boolean allowedOperator = isOperator(player);
        boolean allowedPayload = isLaunchPayload(player) && player.isCreative();
        return (allowedOperator || allowedPayload) && beginShot(player.serverLevel(), player);
    }

    private boolean beginShot(ServerLevel serverLevel, LivingEntity operator) {
        if (!hasLaunchLoad() || !isWindingComplete() || getCooldown() > 0) return false;
        startShot(serverLevel, operator);
        return true;
    }

    private void startShot(ServerLevel serverLevel, LivingEntity operator) {
        triggerAnimation("shoot");
        setShootAnimationTick(0);
        startRecovery();
        playShootSound(serverLevel);
        setOperator(operator);
    }

    @Override
    public boolean acceptsMountedItem(Player player, InteractionHand hand) {
        if (!isOperator(player)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (hasLoadingAction()) {
            return stack.is(getActiveLoadingItem());
        }
        return !hasAmmoLoaded() && findMatchingAmmo(stack) != null;
    }

    @Override
    public InteractionResult handleMountedItem(Player player, InteractionHand hand, ServerLevel serverLevel) {
        return handleSiegeInteraction(player, hand, serverLevel);
    }

    @Override
    public SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        if (!(level() instanceof ServerLevel serverLevel) || !isOperator(operator)) {
            automatedLoading.reset();
            return SiegeActionResult.DENIED;
        }
        if (hasLoadingAction() || getCooldown() > 0 || !isWindingComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (hasLaunchLoad()) {
            beginShot(serverLevel, operator);
            return SiegeActionResult.FIRED;
        }

        AutomatedAmmo ammo = findAutomatedAmmo(inventory);
        if (ammo == null || !AutomatedLoadingSession.hasRequiredItem(inventory, ammo.stage(), ammo.item())) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }
        if (automatedLoading.needsStart(operator, 0)) {
            automatedLoading.start(operator, 0, getLoadingRequirementTicks(ammo.stage().timingKey()));
            onLoadingStart(serverLevel);
        }
        if (!automatedLoading.tickComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (!AutomatedLoadingSession.finishStage(inventory, ammo.stage(), ammo.item(), operator)) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        setAmmoLoaded(ammo.ammoKey());
        setWindingTime(getLoadingRequirementTicks("winding"));
        setOperator(operator);
        automatedLoading.reset();
        return SiegeActionResult.LOADED;
    }

    @Override
    public void cancelPrimaryAction(LivingEntity operator) {
        automatedLoading.cancel(operator);
    }

    @Override
    public SiegeOperationState getOperationState() {
        if (automatedLoading.isActive()) return SiegeOperationState.LOADING;
        if (!hasLaunchPayload()) return super.getOperationState();
        if (getCooldown() > 0) return SiegeOperationState.COOLDOWN;
        return isWindingComplete() ? SiegeOperationState.READY : SiegeOperationState.WINDING;
    }

    private AutomatedAmmo findAutomatedAmmo(Container inventory) {
        if (getAutomatedAmmunitionMode() == SiegeAmmunitionMode.INCENDIARY) {
            return findAutomatedFireAmmo(inventory);
        }
        AutomatedAmmo stoneAmmo = findAutomatedStoneAmmo(inventory);
        if (stoneAmmo != null || getAutomatedAmmunitionMode() == SiegeAmmunitionMode.STANDARD) {
            return stoneAmmo;
        }
        return findAutomatedFireAmmo(inventory);
    }

    private AutomatedAmmo findAutomatedStoneAmmo(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (SiegeAmmo.isStoneProjectile(stack)) {
                LoadingRequirement stage = LoadingRequirement.consume(stack.getItem()).timedBy("stone");
                return new AutomatedAmmo(stage, stack.getItem(), SiegeAmmo.stoneAmmoKey(stack.getItem()));
            }
            if (SiegeAmmo.isGrapeshot(stack)) {
                return new AutomatedAmmo(AMMO_LOADS[2], stack.getItem(), SiegeAmmo.AMMO_GRAPESHOT);
            }
        }
        return null;
    }

    private AutomatedAmmo findAutomatedFireAmmo(Container inventory) {
        Item fireProjectile = SiegeworksItems.FIRE_PROJECTILE.get();
        if (AutomatedLoadingSession.hasRequiredItem(inventory, AMMO_LOADS[1], fireProjectile)) {
            return new AutomatedAmmo(AMMO_LOADS[1], fireProjectile, SiegeAmmo.AMMO_FIRE);
        }
        return null;
    }

    @Override
    protected boolean shouldPlayLoadingSoundOnStart() {
        return false;
    }

    @Override
    protected boolean shouldLoopReloadSoundDuringVisualReload() {
        return true;
    }

    @Override
    protected int getVisualReloadSoundIntervalTicks() {
        return getLoadingRequirementTicks("winding");
    }

    @Override
    protected void playReloadSound(ServerLevel serverLevel) {
        playSoundToNearbyPlayers(serverLevel, getReloadSound(), getSoundProfile().reload().range(),
                getSoundProfile().reload().volume(), 1.0F, 0.0F);
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.is(PASSENGER_LOADING_ITEMS) && itemStack.is(getActiveLoadingItem())) {
            if (!boardLaunchPayload(player)) {
                player.displayClientMessage(Component.translatable("siege.loading.cancelled"), true);
            }
            return;
        }

        LoadingRequirement match = findMatchingAmmo(itemStack);

        if (match == null || !itemStack.is(getActiveLoadingItem())) {
            player.displayClientMessage(Component.translatable("siege.loading.cancelled"), true);
            return;
        }

        if (!consumeLoadingRequirement(player, hand, match)) return;

        if (SiegeAmmo.isStoneProjectile(match.item())) {
            setAmmoLoaded(SiegeAmmo.stoneAmmoKey(match.item()));
        } else if (match.item() == SiegeworksItems.GRAPESHOT.get()) {
            setAmmoLoaded(SiegeAmmo.AMMO_GRAPESHOT);
        } else {
            setAmmoLoaded(SiegeAmmo.AMMO_FIRE);
        }
        setWindingTime(getLoadingRequirementTicks("winding"));
        setOperator(player);
    }

    private LoadingRequirement findMatchingAmmo(ItemStack stack) {
        if (SiegeAmmo.isStoneProjectile(stack)) {
            return LoadingRequirement.consume(stack.getItem()).timedBy("stone");
        }
        for (LoadingRequirement stage : AMMO_LOADS) {
            if (stack.is(stage.item())) return stage;
        }
        return null;
    }

    private void showAmmoListMessage(Player player) {
        Component ammoList = SiegeAmmo.stoneAmmoDescription()
                .copy().append(", ").append(AMMO_LOADS[2].item().getDefaultInstance().getHoverName())
                .append(", ").append(AMMO_LOADS[1].item().getDefaultInstance().getHoverName());
        player.displayClientMessage(Component.translatable("siege.loading.need_one_of", ammoList), true);
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        int animationTick = getShootAnimationTick();
        if (animationTick < 0) {
            return;
        }
        animationTick++;
        if (hasLaunchLoad() && animationTick == PROJECTILE_RELEASE_TICK) {
            fireMangonel(serverLevel);
        }
        setShootAnimationTick(animationTick >= SHOOT_ANIMATION_TICKS ? -1 : animationTick);
    }

    @Override
    protected String getCooldownStatusKey() {
        return hasLaunchLoad() ? "siege.loading.state.firing" : super.getCooldownStatusKey();
    }

    private void fireMangonel(ServerLevel serverLevel) {
        Vec3 launchVelocity = createLaunchVelocity();
        Entity launchPayload = getLaunchPayload();
        if (launchPayload instanceof LivingEntity livingPayload) {
            setAmmoLoaded("");
            launchPassenger(serverLevel, livingPayload, launchVelocity);
            return;
        }

        Vec3 releasePosition = position().add(rotateModelOffset(getLoadOffsetAtShootTick(PROJECTILE_RELEASE_TICK)));
        if (SiegeAmmo.isGrapeshotAmmoKey(getAmmoLoaded())) {
            var scattershot = getScattershotProfile();
            int pelletCount = scattershot.minPellets()
                    + random.nextInt(scattershot.maxPellets() - scattershot.minPellets() + 1);
            ScattershotVolley.spawn(serverLevel, this, releasePosition, launchVelocity,
                    pelletCount, scattershot.spreadDegrees(), scattershot.baseDamagePerPellet(), true);
            setAmmoLoaded("");
            return;
        }

        TrebuchetProjectile projectile = new TrebuchetProjectile(SiegeworksEntities.MANGONEL_PROJECTILE.get(), this, serverLevel);
        projectile.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
        projectile.setDeltaMovement(launchVelocity);

        projectile.setBaseDamage(getBaseDamage());
        projectile.setOwner(this);

        String ammo = getAmmoLoaded();
        if (SiegeAmmo.isStoneAmmoKey(ammo)) {
            projectile.setImpactMode(TrebuchetProjectile.ImpactMode.BREAK_BLOCKS);
            projectile.setTextureName(ammo);
        } else if (SiegeAmmo.isFireAmmoKey(ammo)) {
            projectile.setImpactMode(TrebuchetProjectile.ImpactMode.SPREAD_FIRE);
            projectile.setTextureName(SiegeAmmo.AMMO_FIRE);
        }

        serverLevel.addFreshEntity(projectile);
        setAmmoLoaded("");
    }

    private void launchPassenger(ServerLevel serverLevel, LivingEntity passenger, Vec3 launchVelocity) {
        MangonelPassengerProjectile carrier = new MangonelPassengerProjectile(
                SiegeworksEntities.MANGONEL_PASSENGER_PROJECTILE.get(), this, serverLevel);
        Vec3 releasePosition = position().add(rotateModelOffset(getLoadOffsetAtShootTick(PROJECTILE_RELEASE_TICK)));
        carrier.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
        carrier.setDeltaMovement(launchVelocity);
        carrier.setOwner(this);
        serverLevel.addFreshEntity(carrier);

        transferringLaunchPayload = true;
        passenger.stopRiding();
        transferringLaunchPayload = false;
        entityData.set(LAUNCH_PAYLOAD, Optional.empty());

        if (!passenger.startRiding(carrier, true)) {
            passenger.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
            passenger.setDeltaMovement(launchVelocity);
            passenger.hurtMarked = true;
            carrier.discard();
        }
    }

    private Vec3 createLaunchVelocity() {
        double blocksPerTick = getProjectileSpeed() / 20.0D;
        float yawOffset = (random.nextFloat() - 0.5F) * 4.0F * getAccuracyMultiplier();
        float yawRad = (getVisualRotationYInDegrees() + yawOffset) * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(-Mth.sin(yawRad), LAUNCH_SLOPE, Mth.cos(yawRad)).normalize();
        return direction.scale(blocksPerTick * getShotPower());
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        startShot(serverLevel, operator);
        return true;
    }

    @Override
    public Vec3 getAutomatedAimOrigin() {
        return position().add(rotateModelOffset(getLoadOffsetAtShootTick(PROJECTILE_RELEASE_TICK)));
    }

    @Override
    public float calculateAutomatedAimPitch(Vec3 target) {
        return 0.0F;
    }

    @Override
    public void prepareAutomatedShot(Vec3 target) {
        double power = getRequiredShotPower(target);
        if (Double.isFinite(power)) {
            setShotPower((float) power);
        }
    }

    @Override
    public boolean canReachAutomatedTarget(Vec3 target) {
        double power = getRequiredShotPower(target);
        return Double.isFinite(power) && power >= getMinShotPower() && power <= getMaxShotPower();
    }

    private double getRequiredShotPower(Vec3 target) {
        return SiegeBallistics.calculateFixedArcPower(getAutomatedAimOrigin(), target,
                getProjectileSpeed() / 20.0D, LAUNCH_SLOPE, PROJECTILE_GRAVITY,
                SiegeProfileCatalogs.PROJECTILES.forEntity(SiegeworksEntities.MANGONEL_PROJECTILE.get()).drag(),
                getMinShotPower(), getMaxShotPower());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "anim_controller", state -> {
            if (hasLaunchLoad() && getWindingTime() > 0) {
                state.setAnimation(reloadingAnim);
            } else if (hasLaunchLoad()) {
                state.setAnimation(loadedAnim);
            } else {
                state.setAnimation(unloadedAnim);
            }
            return PlayState.CONTINUE;
        })
                .triggerableAnim("shoot", shootAnim)
                .setAnimationSpeedHandler(animatable -> animatable.hasLaunchLoad() && animatable.getWindingTime() > 0
                        ? 100.0D / Math.max(1, animatable.getWindingTotal())
                        : 1.0D)
                .triggerableAnim("loaded", loadedAnim)
                .triggerableAnim("unloaded", unloadedAnim));
    }

    @Override
    public void triggerAnimation(String name) {
        switch (name) {
            case "shoot" -> triggerAnim("anim_controller", "shoot");
            case "reloading" -> triggerAnim("anim_controller", "reloading");
            case "loaded" -> triggerAnim("anim_controller", "loaded");
            case "unloaded" -> triggerAnim("anim_controller", "unloaded");
        }
    }

    @Override
    public void stopAnimation(String name) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(4.25D).turningAbout(18.0D / 16.0D);
    }

    @Override
    public void tick() {
        previousCollisionRootAngle = getCurrentRootAngle();
        previousCollisionArmAngle = getCurrentArmAngle();
        super.tick();
        StructureMotionSystem.tickStructure(this);
    }

    public int getShootAnimationTick() {
        return entityData.get(SHOOT_ANIMATION_TICK);
    }

    private void setShootAnimationTick(int tick) {
        entityData.set(SHOOT_ANIMATION_TICK, tick);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(getCurrentRootAngle(), getCurrentArmAngle());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionRootAngle, previousCollisionArmAngle);
    }

    private static List<CollisionGroup> collisionGroups(double rootAngle, double armAngle) {
        CollisionPose rootPose = authoredRotation(ROOT_COLLISION_PIVOT, rootAngle);
        CollisionPose armPose = authoredRotation(GeneratedCollisionShapes.MANGONEL_ARM.pivot(), armAngle)
                .then(rootPose);
        return List.of(
                new CollisionGroup("body", GeneratedCollisionShapes.MANGONEL_BODY, rootPose),
                new CollisionGroup("arm", GeneratedCollisionShapes.MANGONEL_ARM, armPose));
    }

    private double getCurrentRootAngle() {
        int shootTick = getShootAnimationTick();
        return shootTick >= 0 ? SHOOT_ROOT.sample(shootTick) : 0.0D;
    }

    private double getCurrentArmAngle() {
        int shootTick = getShootAnimationTick();
        if (shootTick >= 0) {
            return getShootArmAngle(shootTick);
        }
        if (!hasLaunchLoad()) {
            return UNLOADED_ARM_ANGLE;
        }
        return getWindingTime() > 0 ? RELOAD_ARM.sample(getReloadAnimationTick()) : LOADED_ARM_ANGLE;
    }

    private static double getShootArmAngle(float tick) {
        return SHOOT_ARM.sample(tick);
    }

    private float getReloadAnimationTick() {
        int total = getWindingTotal();
        if (total <= 0) {
            return RELOAD_ANIMATION_TICKS;
        }
        double progress = 1.0D - Mth.clamp((double) getWindingTime() / total, 0.0D, 1.0D);
        return (float) (progress * RELOAD_ANIMATION_TICKS);
    }

    private static CollisionPose authoredRotation(Vec3 pivot, double degrees) {
        return CollisionPose.fromGeckoBoneX(pivot, (float) Math.toRadians(-degrees));
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        if (isLaunchPayload(entity)) {
            return applyRootAngle(getLoadOffsetForAuthoredAngle(getCurrentArmAngle()), getCurrentRootAngle())
                    .add(0.0D, MangonelPassengerProjectile.PASSENGER_Y_OFFSET, 0.0D);
        }
        return new Vec3(0.0D, 0.0D, 2.625D);
    }

    @Override
    public float getPassengerViewYawLimit(Entity passenger) {
        return isLaunchPayload(passenger) ? PAYLOAD_VIEW_LIMIT : OPERATOR_VIEW_LIMIT;
    }

    @Override
    public float getPassengerViewPitchLimit(Entity passenger) {
        return isLaunchPayload(passenger) ? PAYLOAD_VIEW_LIMIT : OPERATOR_VIEW_LIMIT;
    }

    @Override
    public Vec3 getTowPivotOffset() {
        return isTowed() ? new Vec3(0.0D, 0.0D, 18.0D / 16.0D) : Vec3.ZERO;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        if (isLaunchPayload(entity)) return getLaunchPayload() == null;
        return isSupportedDirectOperator(entity) && getControllingPassenger() == null;
    }

    @Override
    public boolean canOperate(LivingEntity operator) {
        return !isLaunchPayload(operator)
                && isSupportedDirectOperator(operator)
                && (getControllingPassenger() == null || hasPassenger(operator));
    }

    @Override
    protected boolean operatorControlsRotation(Entity passenger) {
        return !isLaunchPayload(passenger) && super.operatorControlsRotation(passenger);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        boolean launchPayload = isLaunchPayload(passenger);
        super.removePassenger(passenger);
        if (launchPayload && !transferringLaunchPayload) {
            entityData.set(LAUNCH_PAYLOAD, Optional.empty());
            setWindingTime(0);
        }
    }

    public boolean isLaunchPayload(Entity entity) {
        return entity != null && entityData.get(LAUNCH_PAYLOAD)
                .map(entity.getUUID()::equals)
                .orElse(false);
    }

    public boolean hasLaunchPayload() {
        return getLaunchPayload() != null;
    }

    private boolean hasLaunchLoad() {
        return hasAmmoLoaded() || hasLaunchPayload();
    }

    private Entity getLaunchPayload() {
        Optional<UUID> payloadUuid = entityData.get(LAUNCH_PAYLOAD);
        if (payloadUuid.isEmpty()) return null;
        for (Entity passenger : getPassengers()) {
            if (payloadUuid.get().equals(passenger.getUUID())) return passenger;
        }
        return null;
    }

    private boolean boardLaunchPayload(Player player) {
        if (hasLaunchLoad() || player.getVehicle() != null) return false;
        entityData.set(LAUNCH_PAYLOAD, Optional.of(player.getUUID()));
        if (!player.startRiding(this)) {
            entityData.set(LAUNCH_PAYLOAD, Optional.empty());
            return false;
        }
        setWindingTime(getLoadingRequirementTicks("winding"));
        setOperator(player);
        return true;
    }

    private static Vec3 getLoadOffsetForAuthoredAngle(double armAngleDegrees) {
        double angle = -armAngleDegrees * Mth.DEG_TO_RAD;
        double relativeY = LOAD_CENTER.y - ARM_PIVOT.y;
        double relativeZ = LOAD_CENTER.z - ARM_PIVOT.z;
        return new Vec3(
                LOAD_CENTER.x,
                ARM_PIVOT.y + relativeY * Math.cos(angle) - relativeZ * Math.sin(angle),
                ARM_PIVOT.z + relativeY * Math.sin(angle) + relativeZ * Math.cos(angle));
    }

    private static Vec3 getLoadOffsetAtShootTick(float tick) {
        return applyRootAngle(getLoadOffsetForAuthoredAngle(getShootArmAngle(tick)), SHOOT_ROOT.sample(tick));
    }

    private static Vec3 applyRootAngle(Vec3 offset, double rootAngleDegrees) {
        return authoredRotation(ROOT_COLLISION_PIVOT, rootAngleDegrees).toStructure(offset);
    }

    private static ScalarAnimationCurve.Keyframe key(float tick, double value,
                                                      ScalarAnimationCurve.Interpolation interpolation) {
        return ScalarAnimationCurve.key(tick, value, interpolation);
    }

    private Vec3 rotateModelOffset(Vec3 offset) {
        float yawRad = getVisualRotationYInDegrees() * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double rightX = Mth.cos(yawRad);
        double rightZ = Mth.sin(yawRad);
        return new Vec3(
                rightX * offset.x - forwardX * offset.z,
                offset.y,
                rightZ * offset.x - forwardZ * offset.z);
    }

    @Override
    public Vec3 getPlayerPOV() {
        return new Vec3(0.0, -0.7f, 0.0);
    }

    private record AutomatedAmmo(LoadingRequirement stage, Item item, String ammoKey) {
    }
}
