package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.ChromatiCraft;
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
    @Shadow @Final private NoiseGenerator perlin;
    @Shadow @Final private float[][] smallRender;
    @Shadow @Final private float[] testHeight;
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
        return ChromaBiomeEvents.river(biome.baseBiome, original.call(biome));
    }
}
