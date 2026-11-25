package cc.unilock.dragonfixes.mixin.late.rwg2;

import cc.unilock.dragonfixes.compat.ChromaBiomeEvents;
import cc.unilock.dragonfixes.compat.RwgChromaBiomes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.biome.BiomeGenBase;
import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.biomes.realistic.ocean.RealisticBiomeIslandVolcano;
import rwg2.world.ChunkGeneratorRealistic;
import rwg2.world.ChunkManagerRealistic;

@Mixin(value = ChunkGeneratorRealistic.class, remap = false)
public class ChunkGeneratorRealisticMixin {
    @Shadow @Final private boolean[] activeBiomeFlags;

    // ChromatiCraft's column shaper uses the manager's biome rather than the blended biome.
    // Keep the final cliff-family assignment consistent with it, without changing blended heights.
    // Only inspect columns in chunks whose existing 8-block sampling grid contains this family.
    @Inject(
            method = "getNewNoise(Lrwg2/world/ChunkManagerRealistic;II[Lrwg2/biomes/realistic/RealisticBiomeBase;I)[F",
            at = @At("RETURN"),
            require = 1)
    private void dragonfixes$alignCliffBiomes(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, int stride, CallbackInfoReturnable<float[]> cir) {
        RealisticBiomeBase cliffs = RwgChromaBiomes.cliffs;
        RealisticBiomeBase shores = RwgChromaBiomes.shores;
        if (cliffs == null || shores == null) return;
        if (!activeBiomeFlags[cliffs.biomeID] && !activeBiomeFlags[shores.biomeID]) return;

        for (int localZ = 0; localZ < 16; localZ += stride) {
            for (int localX = 0; localX < 16; localX += stride) {
                int index = localZ * 16 + localX;
                RealisticBiomeBase blended = biomes[index];
                RealisticBiomeBase predicted = manager.getBiomeDataAt(x + localX, z + localZ);
                if (predicted == blended || predicted instanceof RealisticBiomeIslandVolcano
                        || blended instanceof RealisticBiomeIslandVolcano) continue;
                if (RwgChromaBiomes.isCliff(predicted) || RwgChromaBiomes.isCliff(blended)) {
                    biomes[index] = predicted;
                }
            }
        }
    }

    // RWG2 bypasses DragonAPI's GenLayerRiverEvent; forward actual river placements.
    @WrapOperation(
            method = "replaceBlocksForBiome",
            at = @At(value = "FIELD", target = "Lrwg2/biomes/realistic/RealisticBiomeBase;riverBiome:Lnet/minecraft/world/biome/BiomeGenBase;", opcode = Opcodes.GETFIELD),
            require = 1)
    private BiomeGenBase dragonfixes$preserveChromaRiverBiomes(RealisticBiomeBase biome,
            Operation<BiomeGenBase> original) {
        return ChromaBiomeEvents.river(biome.baseBiome, original.call(biome));
    }
}
