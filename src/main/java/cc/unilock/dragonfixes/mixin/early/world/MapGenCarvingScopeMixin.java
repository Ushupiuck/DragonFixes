package cc.unilock.dragonfixes.mixin.early.world;

import cc.unilock.dragonfixes.compat.CarverBiomeScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MapGenBase.class)
public abstract class MapGenCarvingScopeMixin {
    @WrapMethod(method = "func_151539_a")
    private void dragonfixes$scopeCarving(IChunkProvider provider, World world, int x, int z,
            Block[] blocks, Operation<Void> original) {
        // Limit caching to the four implementations inspected for this patch.
        // Subclasses and other map generators retain their original behavior.
        String type = getClass().getName();
        if (!type.equals("net.minecraft.world.gen.MapGenCaves")
                && !type.equals("net.minecraft.world.gen.MapGenRavine")
                && !type.equals("rtg.world.gen.MapGenCavesRTG")
                && !type.equals("rtg.world.gen.MapGenRavineRTG")) {
            original.call(provider, world, x, z, blocks);
            return;
        }
        CarverBiomeScope previous = CarverBiomeScope.enter(this, world, x, z);
        try {
            original.call(provider, world, x, z, blocks);
        } finally {
            CarverBiomeScope.exit(previous);
        }
    }
}
