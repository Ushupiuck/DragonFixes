package cc.unilock.dragonfixes.mixin.early.world;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import Reika.DragonAPI.Instantiable.Event.BlockTickEvent;
import Reika.DragonAPI.Instantiable.Event.BlockTickEvent.UpdateFlags;

/**
 * MixinExtras wrapping is composable: apply this at higher priority so
 * Tempora's later wrapper can surround DragonAPI's event dispatch, including
 * its world-gen tracking, before the event eventually calls Block.updateTick.
 */
@Mixin(value = WorldServer.class, priority = 1100)
public abstract class MixinWorldServerTemporaCompat {

    @WrapOperation(
            method = "tickUpdates",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
            require = 1)
    private void dragonfixes$dispatchScheduledBlockEvent(
            Block block, World world, int x, int y, int z, Random random, Operation<Void> original) {
        // Tempora's outer WrapOperation executes first, then invokes this wrapper.
        // DragonAPI's fire() performs Forge event dispatch and the actual block tick.
        BlockTickEvent.fire(block, world, x, y, z, random, UpdateFlags.SCHEDULED);
    }
}
