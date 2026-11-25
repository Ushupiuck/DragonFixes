package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.Registry.ChromaOptions;
import cc.unilock.dragonfixes.compat.ChromaBiomeEvents;
import cc.unilock.dragonfixes.compat.Rwg1ChromaBiomes;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gnu.trove.map.hash.TLongObjectHashMap;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.ChunkManagerRealistic;

@Mixin(value = ChunkManagerRealistic.class, remap = false)
public abstract class ChunkManagerRealisticMixin {
    @Shadow private NoiseGenerator perlin;
    @Shadow private CellNoise cell;
    @Shadow public abstract float getOceanValue(int x, int z);
    @Shadow public abstract RealisticBiomeBase getBiomeDataAt(int x, int z, float ocean);

    @Unique private final ThreadLocal<Boolean> dragonfixes$rawLookup = new ThreadLocal<>();
    @Unique private final TLongObjectHashMap<RealisticBiomeBase> dragonfixes$cliffEdges = new TLongObjectHashMap<>();

    // RWG1's active selector does not have the continental shore pass found in RWG2.
    // Derive an edge only on the parent side of an adjoining water/lowland boundary.
    // Cache separately so raw neighboring lookups never poison RWG's own biome cache.
    @Inject(method = "getBiomeDataAt(IIF)Lrwg/biomes/realistic/RealisticBiomeBase;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void dragonfixes$deriveShores(int x, int z, float ocean,
            CallbackInfoReturnable<RealisticBiomeBase> cir) {
        RealisticBiomeBase cliffs = Rwg1ChromaBiomes.cliffs;
        RealisticBiomeBase shores = Rwg1ChromaBiomes.shores;
        if (cliffs == null || shores == null || cir.getReturnValue() != cliffs
                || Boolean.TRUE.equals(dragonfixes$rawLookup.get())) return;
        long key = ChunkCoordIntPair.chunkXZ2Int(x, z);
        RealisticBiomeBase cached = dragonfixes$cliffEdges.get(key);
        if (cached != null) {
            cir.setReturnValue(cached);
            return;
        }
        RealisticBiomeBase result = cliffs;
        dragonfixes$rawLookup.set(true);
        try {
            // Sample cardinal neighbors within the cliff shaper's native blend radius.
            // Uncarved heights exclude ordinary river channels from the shoreline test.
            outer:
            for (int distance = 8; distance <= 16; distance += 8) {
                for (int direction = 0; direction < 4; direction++) {
                    int nx = x + (direction == 0 ? distance : direction == 1 ? -distance : 0);
                    int nz = z + (direction == 2 ? distance : direction == 3 ? -distance : 0);
                    float neighborOcean = getOceanValue(nx, nz);
                    RealisticBiomeBase neighbor = getBiomeDataAt(nx, nz, neighborOcean);
                    if (neighbor == null || Rwg1ChromaBiomes.isCliff(neighbor)) continue;
                    if (!ChromaOptions.BIOMEBLEND.getState()
                            || BiomeDictionary.isBiomeOfType(neighbor.baseBiome, BiomeDictionary.Type.OCEAN)
                            || neighbor.rNoise(perlin, cell, nx, nz, neighborOcean, 1f, 1f) < 62f) {
                        if (ChromaBiomeEvents.beach(cliffs.baseBiome, BiomeGenBase.beach) == shores.baseBiome) {
                            result = shores;
                        }
                        break outer;
                    }
                }
            }
        } finally {
            dragonfixes$rawLookup.remove();
        }
        if (dragonfixes$cliffEdges.size() >= 16384) dragonfixes$cliffEdges.clear();
        dragonfixes$cliffEdges.put(key, result);
        cir.setReturnValue(result);
    }
}
