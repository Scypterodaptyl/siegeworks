package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class RecruitsSupplyInventory {
    private static final double SUPPLY_RANGE_SQR = 16.0D * 16.0D;

    private RecruitsSupplyInventory() {
    }

    static Container resolve(SiegeEngineerEntity engineer, Vec3 workPosition) {
        Container engineerInventory = engineer.getInventory();
        Container supply = findSupply(engineer, workPosition);
        return supply == null ? engineerInventory : new CompoundContainer(engineerInventory, supply);
    }

    static Container resolveForDeposit(SiegeEngineerEntity engineer, Vec3 workPosition) {
        Container engineerInventory = engineer.getInventory();
        Container supply = findSupply(engineer, workPosition);
        return supply == null ? engineerInventory : new CompoundContainer(supply, engineerInventory);
    }

    /** Rechecked on every use: the upkeep point can also be set through Recruits' own command, which checks nothing. */
    private static Container findSupply(SiegeEngineerEntity engineer, Vec3 workPosition) {
        BlockPos supplyPos = engineer.getUpkeepPos();
        if (supplyPos == null || workPosition.distanceToSqr(Vec3.atCenterOf(supplyPos)) > SUPPLY_RANGE_SQR
                || !engineer.level().hasChunkAt(supplyPos)
                || !RecruitsCompat.claimOpensContainerTo(supplyPos, engineer.getTeam())) {
            return null;
        }

        BlockState state = engineer.level().getBlockState(supplyPos);
        Container supply = null;
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            supply = ChestBlock.getContainer(chestBlock, state, engineer.level(), supplyPos, false);
        } else {
            BlockEntity blockEntity = engineer.level().getBlockEntity(supplyPos);
            if (blockEntity instanceof Container container) {
                supply = container;
            }
        }

        return supply;
    }
}
