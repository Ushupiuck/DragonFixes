package cc.unilock.dragonfixes.mixin.late.rwg1;

import java.util.Arrays;
import java.util.Comparator;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;
import cc.unilock.dragonfixes.compat.Rwg1ChromaBiomes;
import cc.unilock.dragonfixes.compat.RwgChromaClimate;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rwg.api.RWGBiomes;
import rwg.support.RealisticBiomeSupport;
import rwg.support.Support;
import rwg.support.Support.BiomeCategory;
import rwg.support.SupportCC;
import rwg.surface.SurfaceGrassland;
import rwg.terrain.TerrainHilly;
import rwg.terrain.TerrainHighland;

@Mixin(value = SupportCC.class, remap = false)
public abstract class SupportCCMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dragonfixes$registerNativeBiomes(CallbackInfo ci) {
        // Preserve native registration order, but never identify biomes by name or scan a
        // fixed 256-entry range. The wrappers have their own IDs, separate from Minecraft's.
        Rwg1ChromaBiomes.cliffs = null;
        Rwg1ChromaBiomes.shores = null;
        BiomeGenBase[] biomes = { ChromatiCraft.enderforest, ChromatiCraft.rainbowforest,
                ChromatiCraft.glowingcliffs, ChromatiCraft.glowingcliffsEdge };
        Arrays.sort(biomes, Comparator.nullsLast(Comparator.comparingInt(b -> b.biomeID)));
        for (BiomeGenBase biome : biomes) {
            if (biome == null) continue;
            boolean cliff = biome == ChromatiCraft.glowingcliffs || biome == ChromatiCraft.glowingcliffsEdge;
            // The river field is the native fallback, not a separate climate entry.
            // DragonAPI's river event still has the final say during chunk generation.
            BiomeGenBase river = cliff ? biome : biome == ChromatiCraft.rainbowforest
                    && ChromatiCraft.rainbowRiver != null ? ChromatiCraft.rainbowRiver : RWGBiomes.baseTemperateForest;
            RealisticBiomeSupport wrapper = new RealisticBiomeSupport(biome, river,
                    cliff ? new TerrainHilly(100f, 120f, 0f) : new TerrainHighland(6f, 120f, 65f, 150f),
                    new SurfaceGrassland(biome.topBlock, biome.fillerBlock, Blocks.stone, Blocks.cobblestone));
            if (biome == ChromatiCraft.glowingcliffs) Rwg1ChromaBiomes.cliffs = wrapper;
            if (biome == ChromatiCraft.glowingcliffsEdge) {
                Rwg1ChromaBiomes.shores = wrapper;
                // Derived coastline, never an independently selectable climate biome.
            } else {
                for (BiomeCategory category : RwgChromaClimate.categories(biome, BiomeCategory.HOT, "RWG1")) {
                    Support.addBiome(wrapper, category);
                }
            }
            if (DragonFixesConfig.enableBiomeGenerationDiagnostics) {
                DragonFixes.LOGGER.info("RWG1: registered {} (Minecraft ID {}, wrapper ID {}, river fallback {})",
                        biome.biomeName, biome.biomeID, wrapper.biomeID, river.biomeName);
            }
        }
        ci.cancel();
    }
}
