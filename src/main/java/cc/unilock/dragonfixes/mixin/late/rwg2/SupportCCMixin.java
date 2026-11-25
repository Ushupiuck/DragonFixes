package cc.unilock.dragonfixes.mixin.late.rwg2;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;
import cc.unilock.dragonfixes.compat.RwgChromaBiomes;
import cc.unilock.dragonfixes.compat.RwgChromaClimate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.biome.BiomeGenBase;
import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.support.Support.BiomeCategory;
import rwg2.support.SupportCC;

@Mixin(value = SupportCC.class, remap = false)
public class SupportCCMixin {
    @WrapOperation(
            method = "init",
            at = @At(value = "INVOKE", target = "Lrwg2/support/Support;addBiome(Lrwg2/biomes/realistic/RealisticBiomeBase;Lrwg2/support/Support$BiomeCategory;)V"),
            require = 2)
    private static void dragonfixes$overrideChromaClimate(RealisticBiomeBase biome, BiomeCategory originalCategory,
            Operation<Void> original) {
        BiomeGenBase base = biome.baseBiome;
        if (DragonFixesConfig.fixChromaBiomeIntegration) {
            if (base == ChromatiCraft.glowingcliffs) {
                RwgChromaBiomes.cliffs = biome;
            } else if (base == ChromatiCraft.glowingcliffsEdge) {
                // Keep the existing realistic wrapper, but do not put it in any independent climate pool.
                RwgChromaBiomes.shores = biome;
                if (DragonFixesConfig.enableBiomeGenerationDiagnostics) {
                    DragonFixes.LOGGER.info("RWG2: {} derives from Luminous Cliffs coastlines", base.biomeName);
                }
                return;
            }
        }
        for (BiomeCategory category : RwgChromaClimate.categories(base, originalCategory, "RWG2")) {
            original.call(biome, category);
        }
    }
}
