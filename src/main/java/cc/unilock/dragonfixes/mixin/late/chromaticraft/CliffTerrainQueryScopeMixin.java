package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.World.BiomeGlowingCliffs;
import cc.unilock.dragonfixes.compat.CliffTerrainQueryScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BiomeGlowingCliffs.class, remap = false)
public class CliffTerrainQueryScopeMixin {
    @WrapMethod(method = "blendTerrainEdgesAndGenCliffs")
    private static void dragonfixes$scopeBiomeQueries(World world, int chunkX, int chunkZ,
            Block[] blocks, byte[] metadata, Operation<Void> original) {
        CliffTerrainQueryScope previous = CliffTerrainQueryScope.enter(world, chunkX, chunkZ);
        try {
            original.call(world, chunkX, chunkZ, blocks, metadata);
        } finally {
            CliffTerrainQueryScope.exit(previous);
        }
    }
}
