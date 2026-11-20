package cc.unilock.dragonfixes.mixin.early.dragonapi;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.WorldRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import Reika.DragonAPI.Auxiliary.DragonAPIEventWatcher;
import Reika.DragonAPI.Interfaces.Block.Submergeable;

import cc.unilock.dragonfixes.compat.SubmergeableLiquidLoggedCompat;

/** Avoid drawing Reika's simulated water surface over a real stored fluid. */
@Mixin(value = DragonAPIEventWatcher.class, remap = false)
public abstract class MixinDragonAPIEventWatcherLiquidLogged {
    @Inject(method = "onBlockTriedRender", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void dragonfixes$skipSimulatedWaterWhenStored(
            Block block, int x, int y, int z, WorldRenderer renderer, RenderBlocks render,
            int pass, CallbackInfoReturnable<Boolean> cir) {
        if (block instanceof Submergeable
                && SubmergeableLiquidLoggedCompat.hasStoredFluid(render.blockAccess, x, y, z)) {
            // Reika's event watcher only renders the optional visual water surface
            // here. It doesn't need to render it when the cell has real stored fluid.
            cir.setReturnValue(false);
        }
    }
}
