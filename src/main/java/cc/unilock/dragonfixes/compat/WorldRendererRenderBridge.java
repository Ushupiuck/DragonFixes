package cc.unilock.dragonfixes.compat;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.WorldRenderer;

import Reika.DragonAPI.Instantiable.Event.Client.RenderBlockAtPosEvent;

/**
 * Keep Reika's advanced render watchers while accepting LiquidLogged's
 * extended render-pass calculations from its original call-site wraps.
 */
public final class WorldRendererRenderBridge {
    private WorldRendererRenderBridge() {}

    public static int getMaxRenderPass(Block block, int liquidAware, int x, int y, int z) {
        return Math.max(liquidAware, RenderBlockAtPosEvent.getMaxRenderPass(block, x, y, z));
    }

    public static boolean checkCanRenderPass(Block block, int pass, boolean liquidAware,
            int x, int y, int z) {
        boolean reika = RenderBlockAtPosEvent.checkCanRenderPass(block, pass, x, y, z);
        return liquidAware || reika;
    }

    public static boolean fireBlockIfInPass(RenderBlocks render, Block block, int x, int y,
            int z, WorldRenderer renderer, int pass) {
        // If this pass was requested ONLY for the stored fluid, do not draw
        // the containing block in the wrong pass. Still respect advanced
        // DragonAPI render watchers on all applicable passes.
        return RenderBlockAtPosEvent.checkCanRenderPass(block, pass, x, y, z)
                && RenderBlockAtPosEvent.fire(render, block, x, y, z, renderer, pass);
    }
}
