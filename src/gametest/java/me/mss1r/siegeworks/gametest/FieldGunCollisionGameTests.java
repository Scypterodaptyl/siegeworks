package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.siege.CulverinEntity;
import me.mss1r.siegeworks.entity.siege.SerpentineEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(FieldGunCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FieldGunCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-7D;

    private FieldGunCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void fieldGunsUseCarriagesAndCannonsWithoutWheels(GameTestHelper helper) {
        SerpentineEntity serpentine = createSerpentine(helper);
        CulverinEntity culverin = createCulverin(helper);

        helper.assertTrue(groupNames(serpentine).equals(List.of("body", "cannon")),
                "Serpentine collision does not separate carriage and cannon");
        helper.assertTrue(groupNames(culverin).equals(List.of("body", "cannon")),
                "Culverin collision does not separate carriage and cannon");
        helper.assertTrue(GeneratedCollisionShapes.SERPENTINE_BODY.parts().size() == 19
                        && GeneratedCollisionShapes.SERPENTINE_CANNON.parts().size() == 7,
                "Serpentine collision does not match its structural model bones");
        helper.assertTrue(GeneratedCollisionShapes.CULVERIN_BODY.parts().size() == 14
                        && GeneratedCollisionShapes.CULVERIN_CANNON.parts().size() == 10,
                "Culverin collision does not match its structural model bones");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fieldGunAimRaisesCannonAroundAuthoredPivot(GameTestHelper helper) {
        assertAimRaisesCannon(helper, createSerpentine(helper));
        assertAimRaisesCannon(helper, createCulverin(helper));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towingTiltsCompleteFieldGunCarriage(GameTestHelper helper) {
        SerpentineEntity serpentine = createSerpentine(helper);
        CollisionGroup parkedBody = serpentine.collisionGroups().get(0);
        Vec3 trail = new Vec3(0.0D, 0.45D, -2.5D);
        Vec3 parkedTrail = parkedBody.fromGroup(trail);

        Horse horse = EntityType.HORSE.create(helper.getLevel());
        helper.assertTrue(horse != null && horse.startRiding(serpentine, true),
                "Serpentine rejected its draft mount");
        CollisionGroup towedBody = serpentine.collisionGroups().get(0);
        CollisionGroup towedCannon = serpentine.collisionGroups().get(1);
        Vec3 towedTrail = towedBody.fromGroup(trail);
        Vec3 cannonPivot = GeneratedCollisionShapes.SERPENTINE_CANNON.pivot();

        helper.assertTrue(towedTrail.y > parkedTrail.y + 0.3D,
                "Towing did not lift the Serpentine trail");
        helper.assertTrue(towedBody.fromGroup(cannonPivot).distanceToSqr(
                        towedCannon.fromGroup(cannonPivot)) < EPSILON,
                "Towed carriage and cannon disagree at their shared aim pivot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fieldGunBoundsIncludeFullTrail(GameTestHelper helper) {
        CulverinEntity culverin = createCulverin(helper);
        AABB modelBounds = StructureCollisionResolver.worldBounds(culverin);

        helper.assertTrue(modelBounds != null, "Culverin has no model-derived bounds");
        helper.assertTrue(modelBounds.getZsize() > 3.9D,
                "Culverin model bounds do not reach both its trail and muzzle");
        helper.assertTrue(contains(culverin.getBoundingBoxForCulling(), modelBounds),
                "Culverin culling bounds do not contain its complete model");
        helper.succeed();
    }

    private static boolean contains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private static void assertAimRaisesCannon(GameTestHelper helper, AbstractFieldGunEntity gun) {
        CollisionGroup level = gun.collisionGroups().get(1);
        CollisionPart barrel = level.parts().get(level.parts().size() - 1);
        AABB barrelBox = barrel.box();
        Vec3 muzzle = new Vec3(barrelBox.getCenter().x, barrelBox.getCenter().y, barrelBox.maxZ);
        Vec3 levelMuzzle = level.fromPart(barrel, muzzle);

        gun.setTrackedPitch(gun.getMinAimPitch());
        CollisionGroup raised = gun.collisionGroups().get(1);
        Vec3 raisedMuzzle = raised.fromPart(barrel, muzzle);

        helper.assertTrue(raisedMuzzle.y > levelMuzzle.y + 0.2D,
                gun.getType() + " aim tilted its cannon opposite to the rendered model");
        helper.assertTrue(level.fromGroup(level.shape().pivot()).distanceToSqr(
                        raised.fromGroup(raised.shape().pivot())) < EPSILON,
                gun.getType() + " aim moved its authored cannon pivot");
    }

    private static List<String> groupNames(AbstractFieldGunEntity gun) {
        return gun.collisionGroups().stream().map(CollisionGroup::name).toList();
    }

    private static SerpentineEntity createSerpentine(GameTestHelper helper) {
        SerpentineEntity gun = SiegeworksEntities.SERPENTINE_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(gun != null, "Failed to create Serpentine");
        placeAtTestCenter(helper, gun);
        return gun;
    }

    private static CulverinEntity createCulverin(GameTestHelper helper) {
        CulverinEntity gun = SiegeworksEntities.CULVERIN_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(gun != null, "Failed to create Culverin");
        placeAtTestCenter(helper, gun);
        return gun;
    }

    private static void placeAtTestCenter(GameTestHelper helper, AbstractFieldGunEntity gun) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        gun.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        level.addFreshEntity(gun);
    }
}
