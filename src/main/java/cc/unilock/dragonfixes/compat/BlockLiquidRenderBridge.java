package cc.unilock.dragonfixes.compat;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import Reika.DragonAPI.Libraries.World.ReikaBlockHelper;

/**
 * Receives the neighbor block after LiquidLogged's WrapOperation has had the
 * opportunity to replace a waterlogged or lavalogged block with its fluid.
 */
public final class BlockLiquidRenderBridge {
    private BlockLiquidRenderBridge() {}

    public static boolean renderLiquidSide(IBlockAccess world, int x, int y, int z,
            int side, Block liquid, Block neighbor) {
        // LiquidLogged replaces the neighboring block with the contained fluid.
        // Hide shared faces when the neighbor is now the same fluid material.
        if (neighbor != null && neighbor.getMaterial() == liquid.getMaterial()) {
            return false;
        }

        // Preserve DragonAPI's Submergeable and remaining face-rendering rules.
        return ReikaBlockHelper.renderLiquidSide(world, x, y, z, side, liquid);
    }
}
