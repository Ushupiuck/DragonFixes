package cc.unilock.dragonfixes.mixin.late.rwg2;

import java.util.EnumSet;
import java.util.Locale;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;
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
        String[] configured;
        if (base == ChromatiCraft.rainbowforest) {
            configured = DragonFixesConfig.rainbowForestClimate;
        } else if (base == ChromatiCraft.enderforest) {
            configured = DragonFixesConfig.enderForestClimate;
        } else if (base == ChromatiCraft.glowingcliffs || base == ChromatiCraft.glowingcliffsEdge) {
            configured = DragonFixesConfig.luminousCliffsClimate;
        } else {
            original.call(biome, originalCategory);
            return;
        }

        EnumSet<BiomeCategory> categories = EnumSet.noneOf(BiomeCategory.class);
        if (configured != null) {
            for (String value : configured) {
                String name = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
                switch (name) {
                    case "SNOW":
                    case "COLD":
                    case "HOT":
                    case "WET":
                        categories.add(BiomeCategory.valueOf(name));
                        break;
                    default:
                        DragonFixes.LOGGER.warn("Invalid RWG2 climate '{}' for {}; retaining {}", value,
                                base.biomeName, originalCategory);
                        original.call(biome, originalCategory);
                        return;
                }
            }
        }

        if (categories.isEmpty()) {
            DragonFixes.LOGGER.warn("Empty RWG2 climate list for {}; retaining {}", base.biomeName, originalCategory);
            original.call(biome, originalCategory);
            return;
        }

        // Reuse the same realistic biome (and internal ID) in every selected pool.
        for (BiomeCategory category : categories) {
            original.call(biome, category);
        }
        DragonFixes.LOGGER.info("RWG2 climate override: {} -> {}", base.biomeName, categories);
    }
}
