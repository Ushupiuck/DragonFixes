package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.ChromaBiomeEvents;
import cc.unilock.dragonfixes.compat.Rwg1ChromaBiomes;
import cc.unilock.dragonfixes.compat.Rwg1StreamPreview;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import rwg.util.NoiseGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.BiomeGenBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.world.ChunkGeneratorRealistic;
import rwg.world.ChunkManagerRealistic;

@Mixin(value = ChunkGeneratorRealistic.class, remap = false)
public abstract class ChunkGeneratorRealisticMixin implements Rwg1StreamPreview {
    @Shadow @Final private ChunkManagerRealistic cmr;
    @Unique private boolean dragonfixes$loggedRainbowForest;
    @Unique private boolean dragonfixes$loggedRiverEvent;
    @Unique private boolean dragonfixes$loggedDryCandidate;
    @Shadow @Final private NoiseGenerator perlin;
    @Shadow @Final private float[] mapGenBiomes;
    @Shadow @Final private float[] testHeight;
    @Shadow @Final private float[] riverStrength;
    @Shadow public abstract float[] getNewNoise(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes);

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void dragonfixes$attachPreview(World world, long seed, CallbackInfo ci) {
        if (cmr instanceof Rwg1StreamPreview.Access) {
            ((Rwg1StreamPreview.Access) cmr).dragonfixes$setStreamPreview(this);
        }
        if (!world.isRemote) {
            DragonFixes.LOGGER.info("RWG1: active chunk generator in dimension {} (seed {})",
                    world.provider.dimensionId, seed);
        }
    }

    // Exact predicate used by replaceBlocksForBiome, including its short-scale noise.
    @Unique
    private boolean dragonfixes$isRiverColumn(int x, int z, float river) {
        return river > 0.05f && river + perlin.noise2(x / 10f, z / 10f) * 0.15f > 0.8f;
    }

    @Override
    public BiomeGenBase dragonfixes$previewStream(int x, int z) {
        if (!dragonfixes$isRiverColumn(x, z, -cmr.getRiverStrength(x, z))) return null;

        // Nature's Compass executes its worker on the server tick. getNewNoise uses
        // reusable interpolation scratch buffers; its persistent output arrays must
        // not be left with the preview chunk's heights or map-generation weights.
        float[] oldWeights = mapGenBiomes.clone();
        float[] oldHeights = testHeight.clone();
        float[] oldRivers = riverStrength.clone();
        RealisticBiomeBase[] predicted = new RealisticBiomeBase[256];
        try {
            getNewNoise(cmr, x & ~15, z & ~15, predicted);
            RealisticBiomeBase biome = predicted[(z & 15) * 16 + (x & 15)];
            if (biome == null || biome.baseBiome != ChromatiCraft.rainbowforest) return null;
            return ChromaBiomeEvents.river(biome.baseBiome, biome.riverBiome);
        } finally {
            System.arraycopy(oldWeights, 0, mapGenBiomes, 0, oldWeights.length);
            System.arraycopy(oldHeights, 0, testHeight, 0, oldHeights.length);
            System.arraycopy(oldRivers, 0, riverStrength, 0, oldRivers.length);
        }
    }

    @Unique private boolean dragonfixes$loggedCliffs;
    @Unique private boolean dragonfixes$loggedShores;
    @Unique private boolean dragonfixes$loggedStream;

    // The native Chroma cliff shaper consults the manager, not RWG1's blended selection.
    // Keep the biome IDs consistent with that shaper without altering blended heights.
    @Inject(method = "getNewNoise", at = @At("RETURN"), require = 1)
    private void dragonfixes$alignCliffBiomes(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, CallbackInfoReturnable<float[]> cir) {
        if (Rwg1ChromaBiomes.cliffs == null || Rwg1ChromaBiomes.shores == null) return;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int index = localZ * 16 + localX;
                RealisticBiomeBase predicted = manager.getBiomeDataAt(x + localX, z + localZ);
                if (Rwg1ChromaBiomes.isCliff(predicted) || Rwg1ChromaBiomes.isCliff(biomes[index])) {
                    if (predicted != null) biomes[index] = predicted;
                }
            }
        }
    }

    @WrapOperation(method = "replaceBlocksForBiome",
            at = @At(value = "FIELD", target = "Lrwg/biomes/realistic/RealisticBiomeBase;riverBiome:Lnet/minecraft/world/biome/BiomeGenBase;",
                    opcode = Opcodes.GETFIELD), require = 1)
    private BiomeGenBase dragonfixes$forwardRiverEvent(RealisticBiomeBase biome, Operation<BiomeGenBase> original) {
        BiomeGenBase planned = original.call(biome);
        BiomeGenBase resolved = ChromaBiomeEvents.river(biome.baseBiome, planned);
        if (!dragonfixes$loggedRiverEvent && biome.baseBiome == ChromatiCraft.rainbowforest) {
            dragonfixes$loggedRiverEvent = true;
            DragonFixes.LOGGER.info("RWG1: first actual Rainbow Forest river event: planned={}, resolved={}",
                    planned == null ? "null" : planned.biomeName, resolved == null ? "null" : resolved.biomeName);
        }
        return resolved;
    }

    @Inject(method = "replaceBlocksForBiome", at = @At("RETURN"), require = 1)
    private void dragonfixes$reportFinalBiomes(int cx, int cz, net.minecraft.block.Block[] blocks, byte[] metadata,
            RealisticBiomeBase[] biomes, BiomeGenBase[] base, float[] heights, CallbackInfo ci) {
        if (dragonfixes$loggedCliffs && dragonfixes$loggedShores && dragonfixes$loggedStream
                && dragonfixes$loggedRainbowForest) return;
        int forest = 0;
        int candidates = 0;
        int streams = 0;
        int wetStreams = 0;
        float strongest = 0f;
        for (int i = 0; i < 256; i++) {
            RealisticBiomeBase parent = biomes[i];
            int lx = i & 15;
            int lz = i >> 4;
            float strength = -riverStrength[lx * 16 + lz];
            if (parent != null && parent.baseBiome == ChromatiCraft.rainbowforest) {
                forest++;
                strongest = Math.max(strongest, strength);
                if (dragonfixes$isRiverColumn(cx * 16 + lx, cz * 16 + lz, strength)) candidates++;
            }
            BiomeGenBase biome = base[i];
            if (biome != null && biome == ChromatiCraft.rainbowRiver) {
                streams++;
                if (blocks[(lx * 16 + lz) * 256 + 62] == net.minecraft.init.Blocks.water) wetStreams++;
            }
            if (biome == null) continue;
            if (!dragonfixes$loggedCliffs && biome == ChromatiCraft.glowingcliffs) {
                dragonfixes$loggedCliffs = true;
                DragonFixes.LOGGER.info("RWG1: generating Luminous Cliffs at chunk {}, {}", cx, cz);
            }
            if (!dragonfixes$loggedShores && biome == ChromatiCraft.glowingcliffsEdge) {
                dragonfixes$loggedShores = true;
                DragonFixes.LOGGER.info("RWG1: generating Luminous Cliffs Shores at chunk {}, {}", cx, cz);
            }
        }
        if (forest > 0 && !dragonfixes$loggedRainbowForest) {
            dragonfixes$loggedRainbowForest = true;
            DragonFixes.LOGGER.info("RWG1: first generated Rainbow Forest parent at chunk {}, {} ({} columns, strongest river={}, candidates={})",
                    cx, cz, forest, strongest, candidates);
        }
        if (streams > 0 && !dragonfixes$loggedStream) {
            dragonfixes$loggedStream = true;
            DragonFixes.LOGGER.info("RWG1: generating Rainbow Stream at chunk {}, {} ({} columns, {} water at y=62)",
                    cx, cz, streams, wetStreams);
        }
        if (candidates > 0 && streams == 0 && !dragonfixes$loggedDryCandidate) {
            dragonfixes$loggedDryCandidate = true;
            DragonFixes.LOGGER.warn("RWG1: {} eligible Rainbow Forest river columns at chunk {}, {} but no Rainbow Stream in final array",
                    candidates, cx, cz);
        }
    }
}
