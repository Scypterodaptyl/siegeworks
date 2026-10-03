package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.item.SiegeAmmo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(MangonelCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MangonelCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-6D;

    private MangonelCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void mangonelUsesOnlyPhysicalModelGroups(GameTestHelper helper) {
        MangonelEntity mangonel = createMangonel(helper);
        List<String> names = mangonel.collisionGroups().stream().map(CollisionGroup::name).toList();

        helper.assertTrue(names.equals(List.of("body", "arm")),
                "Mangonel collision contains visual-only groups: " + names);
        helper.assertTrue(GeneratedCollisionShapes.MANGONEL_BODY.parts().size() == 12,
                "Mangonel frame collision does not match the authored base bone");
        helper.assertTrue(GeneratedCollisionShapes.MANGONEL_ARM.parts().size() == 4,
                "Mangonel arm collision does not match the authored catapult bone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mangonelReloadFollowsAuthoredArmTrack(GameTestHelper helper) {
        MangonelEntity mangonel = createMangonel(helper);
        Vec3 unloadedCenter = armCenter(mangonel);

        mangonel.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        Vec3 loadedCenter = armCenter(mangonel);
        Vec3 expectedLoadedCenter = authoredArmCenter(-60.0D);
        helper.assertTrue(loadedCenter.distanceToSqr(expectedLoadedCenter) < EPSILON,
                "Loading the mangonel did not apply the exported -60 degree arm pose");
        helper.assertTrue(loadedCenter.distanceToSqr(unloadedCenter) > 0.1D,
                "Loading the mangonel did not move its arm collision");

        mangonel.setWindingTime(100);
        Vec3 reloadStart = armCenter(mangonel);
        mangonel.setWindingTime(50);
        Vec3 reloadHalf = armCenter(mangonel);
        helper.assertTrue(reloadStart.distanceToSqr(unloadedCenter) < EPSILON,
                "Mangonel reload does not start at the authored unloaded pose");
        double fullTravel = reloadStart.distanceTo(loadedCenter);
        helper.assertTrue(reloadHalf.distanceTo(reloadStart) < fullTravel
                        && reloadHalf.distanceTo(loadedCenter) < fullTravel,
                "Mangonel reload collision skipped or reversed the authored arm travel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mangonelShotMovesCollisionAndReleasesAlongTheLaunchLine(GameTestHelper helper) {
        MangonelEntity mangonel = createMangonel(helper);
        ServerLevel level = helper.getLevel();
        level.addFreshEntity(mangonel);
        mangonel.setAmmoLoaded(SiegeAmmo.AMMO_STONE);

        mangonel.handleSiegeInteraction(
                SiegeGameTestPlayers.create(level), InteractionHand.MAIN_HAND, level);
        helper.assertTrue(mangonel.getShootAnimationTick() == 0,
                "Mangonel did not start its synchronized shot track");

        Vec3 bodyAtRest = bodyCenter(mangonel);
        // The cup runs along the launch line a third of the way into the second tick of its swing.
        mangonel.onSiegeTick(level);
        helper.assertTrue(mangonel.hasAmmoLoaded(),
                "Mangonel released its projectile before its cup ran along the launch line");
        mangonel.onSiegeTick(level);
        helper.assertTrue(mangonel.getShootAnimationTick() == 2 && !mangonel.hasAmmoLoaded(),
                "Mangonel did not release its projectile on animation tick 2");
        mangonel.onSiegeTick(level);

        mangonel.onSiegeTick(level);
        helper.assertTrue(bodyCenter(mangonel).distanceToSqr(bodyAtRest) > 1.0E-5D,
                "Mangonel frame collision ignored the authored recoil track");
        mangonel.onSiegeTick(level);
        mangonel.onSiegeTick(level);
        helper.assertTrue(armCenter(mangonel).distanceToSqr(unloadedArmCenter(mangonel)) < EPSILON,
                "Mangonel arm collision did not finish at the authored unloaded pose");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mangonelShotStateSurvivesSaveAndLoad(GameTestHelper helper) {
        MangonelEntity source = createMangonel(helper);
        ServerLevel level = helper.getLevel();
        source.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        source.handleSiegeInteraction(SiegeGameTestPlayers.create(level), InteractionHand.MAIN_HAND, level);
        source.onSiegeTick(level);

        CompoundTag saved = new CompoundTag();
        source.addAdditionalSaveData(saved);
        MangonelEntity restored = createMangonel(helper);
        restored.readAdditionalSaveData(saved);

        helper.assertTrue(restored.getShootAnimationTick() == 1 && restored.hasAmmoLoaded(),
                "Mangonel lost its in-progress shot while loading NBT");
        restored.onSiegeTick(level);
        restored.onSiegeTick(level);
        helper.assertTrue(!restored.hasAmmoLoaded(),
                "Restored mangonel did not release its projectile on the authored tick");
        helper.succeed();
    }

    private static Vec3 bodyCenter(MangonelEntity mangonel) {
        CollisionGroup body = mangonel.collisionGroups().get(0);
        return body.fromPart(body.parts().get(0), Vec3.ZERO);
    }

    private static Vec3 armCenter(MangonelEntity mangonel) {
        CollisionGroup arm = mangonel.collisionGroups().get(1);
        return arm.fromPart(arm.parts().get(1), Vec3.ZERO);
    }

    private static Vec3 unloadedArmCenter(MangonelEntity mangonel) {
        CollisionGroup body = mangonel.collisionGroups().get(0);
        CollisionGroup arm = new CollisionGroup("arm", GeneratedCollisionShapes.MANGONEL_ARM, body.pose());
        return arm.fromPart(arm.parts().get(1), Vec3.ZERO);
    }

    private static Vec3 authoredArmCenter(double degrees) {
        CollisionPart beam = GeneratedCollisionShapes.MANGONEL_ARM.parts().get(1);
        Vec3 center = beam.pose().toStructure(Vec3.ZERO);
        return CollisionPose.aroundX(
                GeneratedCollisionShapes.MANGONEL_ARM.pivot(), (float) Math.toRadians(degrees))
                .toStructure(center);
    }

    private static MangonelEntity createMangonel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        helper.assertTrue(mangonel != null, "Failed to create mangonel");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        mangonel.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        return mangonel;
    }
}
