package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.World.BiomeGlowingCliffs;
import Reika.ChromatiCraft.World.GlowingCliffsColumnShaper;
import cc.unilock.dragonfixes.compat.CliffBiomeNeighborhood;
import cc.unilock.dragonfixes.compat.CliffBiomeTiles;
import cc.unilock.dragonfixes.compat.CliffTerrainQueryScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GlowingCliffsColumnShaper.class, remap = false)
public abstract class GlowingCliffsColumnShaperMixin {
    @Shadow private BiomeGenBase getBiome(World world, int x, int z) { throw new AssertionError(); }

    @WrapMethod(method = "getBiome")
    private BiomeGenBase dragonfixes$memoizeBiome(World world, int x, int z, Operation<BiomeGenBase> original) {
        CliffTerrainQueryScope scope = CliffTerrainQueryScope.current(world);
        if (scope == null) return original.call(world, x, z);
        CliffBiomeNeighborhood<BiomeGenBase> cache = scope.neighborhood;
        int index = cache.index(x, z);
        if (cache.isKnown(index)) return cache.get(index);
        return cache.remember(index, original.call(world, x, z));
    }

    @WrapMethod(method = "getInvertedDistanceFactor")
    private Object[] dragonfixes$skipCliffFreeNeighborhood(World world, int x, int z, int search,
            Operation<Object[]> original) {
        CliffTerrainQueryScope scope = CliffTerrainQueryScope.current(world);
        if (scope != null && search > 0 && search <= CliffBiomeNeighborhood.MAX_SEARCH
                && scope.neighborhood.isChunkColumn(x, z)) {
            boolean mayContain = scope.neighborhood.mayContainReachableCliff((qx, qz) -> getBiome(world, qx, qz),
                    (tx, tz) -> CliffBiomeTiles.getTile(world.getWorldChunkManager(), tx, tz), BiomeGlowingCliffs::isGlowingCliffs);
            if (!mayContain) return null;
        }
        // Do not change the original angular search, tie breaking, heights,
        // interpolation, biome classification or random-number consumption.
        return original.call(world, x, z, search);
    }
}
