package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;
import cc.unilock.dragonfixes.compat.ChromaBiomeEvents;
import cc.unilock.dragonfixes.compat.Rwg1ChromaBiomes;
import cc.unilock.dragonfixes.compat.Rwg1StreamPreview;
import cc.unilock.dragonfixes.compat.Rwg1BiomeSelection;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import rwg.util.NoiseGenerator;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
    @Shadow @Final private float[][] smallRender;
    @Shadow @Final private float[] testHeight;
    @Shadow @Final private float[] riverStrength;
    @Shadow public abstract float[] getNewNoise(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes);
    @Unique private final RealisticBiomeBase[] dragonfixes$previewBiomes = new RealisticBiomeBase[256];
    @Unique private int dragonfixes$previewIndex = -1;
    @Unique private final float[] dragonfixes$oceanValues = new float[256];
    @Unique private int dragonfixes$oceanCursor;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void dragonfixes$attachPreview(World world, long seed, CallbackInfo ci) {
        if (cmr instanceof Rwg1StreamPreview.Access) {
            ((Rwg1StreamPreview.Access) cmr).dragonfixes$setStreamPreview(this);
        }
        if (DragonFixesConfig.enableBiomeGenerationDiagnostics && !world.isRemote) {
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

        // The worker runs on the server tick, like the generator it borrows.
        // Only interpolation scratch is touched; the three persistent output
        // arrays are never written during a preview.
        if (dragonfixes$previewIndex != -1) return null;
        int index = (z & 15) * 16 + (x & 15);
        dragonfixes$previewBiomes[index] = null;
        RealisticBiomeBase biome;
        dragonfixes$previewIndex = index;
        try {
            getNewNoise(cmr, x & ~15, z & ~15, dragonfixes$previewBiomes);
            biome = dragonfixes$previewBiomes[index];
        } finally {
            dragonfixes$previewIndex = -1;
        }
        if (biome == null || biome.baseBiome != ChromatiCraft.rainbowforest) return null;
        return ChromaBiomeEvents.river(biome.baseBiome, biome.riverBiome);
    }

    // The first mix4 call follows the upstream >0.95 dominant-biome check.
    // Dominant previews do not need any of the remaining interpolation stages.
    @Inject(method = "getNewNoise",
            at = @At(value = "INVOKE", target = "Lrwg/world/ChunkGeneratorRealistic;mix4([[F[F)V", ordinal = 0),
            cancellable = true, require = 1, allow = 1)
    private void dragonfixes$finishDominantPreview(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, CallbackInfoReturnable<float[]> cir,
            @Local RealisticBiomeBase dominant) {
        if (biomes != dragonfixes$previewBiomes || dragonfixes$previewIndex < 0 || dominant == null) return;
        dragonfixes$finishPreview(manager, x, z, biomes, dominant, cir);
    }

    // Mixed-biome previews still need the full upstream interpolation. Stop
    // before the terrain column loop writes any persistent generator output.
    @Inject(method = "getNewNoise", at = @At(value = "JUMP", opcode = Opcodes.IFNULL),
            cancellable = true, require = 1, allow = 1)
    private void dragonfixes$finishBiomeOnlyPreview(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, CallbackInfoReturnable<float[]> cir,
            @Local RealisticBiomeBase dominant) {
        if (biomes != dragonfixes$previewBiomes || dragonfixes$previewIndex < 0) return;
        int index = dragonfixes$previewIndex;
        int localX = index & 15;
        int localZ = index >> 4;
        RealisticBiomeBase selected = dominant;
        if (selected == null) {
            int id = Rwg1BiomeSelection.select(
                    smallRender[(localX + 4) * 25 + (localZ + 4)],
                    perlin.noise2((float) (x + localX) / 15f, (float) (z + localZ) / 15f));
            if (id >= 0) selected = RealisticBiomeBase.getBiome(id);
        }
        dragonfixes$finishPreview(manager, x, z, biomes, selected, cir);
    }

    @Unique
    private void dragonfixes$finishPreview(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, RealisticBiomeBase selected, CallbackInfoReturnable<float[]> cir) {
        int index = dragonfixes$previewIndex;
        if (Rwg1ChromaBiomes.cliffs != null && Rwg1ChromaBiomes.shores != null) {
            selected = dragonfixes$alignCliffColumn(manager.getBiomeDataAt(x + (index & 15), z + (index >> 4)), selected);
        }
        biomes[index] = selected;
        // The caller only consumes the selected biome, never this height array.
        cir.setReturnValue(testHeight);
    }

    @Unique private boolean dragonfixes$loggedCliffs;
    @Unique private boolean dragonfixes$loggedShores;
    @Unique private boolean dragonfixes$loggedStream;

    @Inject(method = "getNewNoise", at = @At("HEAD"), require = 1)
    private void dragonfixes$resetOceanSamples(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, CallbackInfoReturnable<float[]> cir) {
        if (biomes != dragonfixes$previewBiomes) dragonfixes$oceanCursor = 0;
    }

    // RWG already computes these values in X-major order. Preserve the exact
    // float returned by its own manager instead of running the noise again.
    @ModifyExpressionValue(method = "getNewNoise",
            at = @At(value = "INVOKE", target = "Lrwg/world/ChunkManagerRealistic;getOceanValue(II)F"), require = 1)
    private float dragonfixes$rememberOceanValue(float original) {
        int index = dragonfixes$oceanCursor;
        if (index >= 0 && index < dragonfixes$oceanValues.length) {
            dragonfixes$oceanValues[index] = original;
            dragonfixes$oceanCursor = index + 1;
        } else {
            dragonfixes$oceanCursor = -1;
        }
        return original;
    }

    // The native Chroma cliff shaper consults the manager, not RWG1's blended selection.
    // Keep the biome IDs consistent with that shaper without altering blended heights.
    @Inject(method = "getNewNoise", at = @At("RETURN"), require = 1)
    private void dragonfixes$alignCliffBiomes(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, CallbackInfoReturnable<float[]> cir) {
        if (biomes == dragonfixes$previewBiomes
                || Rwg1ChromaBiomes.cliffs == null || Rwg1ChromaBiomes.shores == null) return;
        boolean complete = dragonfixes$oceanCursor == dragonfixes$oceanValues.length;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int index = localZ * 16 + localX;
                RealisticBiomeBase predicted = complete
                        ? manager.getBiomeDataAt(x + localX, z + localZ, dragonfixes$oceanValues[localX * 16 + localZ])
                        : manager.getBiomeDataAt(x + localX, z + localZ);
                biomes[index] = dragonfixes$alignCliffColumn(predicted, biomes[index]);
            }
        }
    }

    @Unique
    private static RealisticBiomeBase dragonfixes$alignCliffColumn(RealisticBiomeBase predicted,
            RealisticBiomeBase blended) {
        if (predicted != null && (Rwg1ChromaBiomes.isCliff(predicted) || Rwg1ChromaBiomes.isCliff(blended))) {
            return predicted;
        }
        return blended;
    }

    @WrapOperation(method = "replaceBlocksForBiome",
            at = @At(value = "FIELD", target = "Lrwg/biomes/realistic/RealisticBiomeBase;riverBiome:Lnet/minecraft/world/biome/BiomeGenBase;",
                    opcode = Opcodes.GETFIELD), require = 1)
    private BiomeGenBase dragonfixes$forwardRiverEvent(RealisticBiomeBase biome, Operation<BiomeGenBase> original) {
        BiomeGenBase planned = original.call(biome);
        BiomeGenBase resolved = ChromaBiomeEvents.river(biome.baseBiome, planned);
        if (DragonFixesConfig.enableBiomeGenerationDiagnostics && !dragonfixes$loggedRiverEvent
                && biome.baseBiome == ChromatiCraft.rainbowforest) {
            dragonfixes$loggedRiverEvent = true;
            DragonFixes.LOGGER.info("RWG1: first actual Rainbow Forest river event: planned={}, resolved={}",
                    planned == null ? "null" : planned.biomeName, resolved == null ? "null" : resolved.biomeName);
        }
        return resolved;
    }

    @Inject(method = "replaceBlocksForBiome", at = @At("RETURN"), require = 1)
    private void dragonfixes$reportFinalBiomes(int cx, int cz, net.minecraft.block.Block[] blocks, byte[] metadata,
            RealisticBiomeBase[] biomes, BiomeGenBase[] base, float[] heights, CallbackInfo ci) {
        if (!DragonFixesConfig.enableBiomeGenerationDiagnostics) return;
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
