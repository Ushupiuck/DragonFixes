package cc.unilock.dragonfixes.mixin.late.rtg;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import rtg.api.biome.BiomeConfig;
import rtg.api.biome.chromaticraft.config.BiomeConfigCC;
import rtg.world.biome.deco.DecoBaseBiomeDecorations;
import rtg.world.biome.realistic.RealisticBiomeBase;
import rtg.world.biome.realistic.chromaticraft.RealisticBiomeCCBase;
import rtg.world.biome.realistic.chromaticraft.RealisticBiomeCCEnderForest;
import rtg.world.biome.realistic.chromaticraft.RealisticBiomeCCLuminousCliffs;
import rtg.world.biome.realistic.chromaticraft.RealisticBiomeCCRainbowForest;
import rtg.world.gen.surface.vanilla.SurfaceVanillaRiver;
import rtg.world.gen.terrain.vanilla.TerrainVanillaRiver;

@Mixin(value = RealisticBiomeCCBase.class, remap = false)
public abstract class RealisticBiomeCCBaseMixin {
    @Shadow public static RealisticBiomeBase ccEnderForest;
    @Shadow public static RealisticBiomeBase ccLuminousCliffs;
    @Shadow public static RealisticBiomeBase ccRainbowForest;

    // Use ChromatiCraft's registered instances instead of scanning 256 entries and comparing strings by identity.
    // The original RTG constructors register their wrappers using the actual biome ID.
    @Inject(method = "addBiomes", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dragonfixes$registerChromaBiomes(CallbackInfo ci) {
        if (ChromatiCraft.enderforest == null || ChromatiCraft.glowingcliffs == null
                || ChromatiCraft.rainbowforest == null || ChromatiCraft.glowingcliffsEdge == null
                || ChromatiCraft.rainbowRiver == null) {
            DragonFixes.LOGGER.warn("RTG: ChromatiCraft biome instances are incomplete; retaining original registration");
            return;
        }

        ccEnderForest = new RealisticBiomeCCEnderForest(
                ChromatiCraft.enderforest, BiomeConfigCC.biomeConfigCCEnderForest);
        ccLuminousCliffs = new RealisticBiomeCCLuminousCliffs(
                ChromatiCraft.glowingcliffs, BiomeConfigCC.biomeConfigCCLuminousCliffs);
        ccRainbowForest = new RealisticBiomeCCRainbowForest(
                ChromatiCraft.rainbowforest, BiomeConfigCC.biomeConfigCCRainbowForest);

        // Shores use the same realistic terrain/surface implementation as their parent.
        // ChromatiCraft's own column shaper handles the lower shoreline terrain.
        new RealisticBiomeCCLuminousCliffs(
                ChromatiCraft.glowingcliffsEdge, BiomeConfigCC.biomeConfigCCLuminousCliffs);

        // Rainbow Stream uses river terrain, with its own native ChromatiCraft decorator.
        BiomeConfig streamConfig = BiomeConfigCC.biomeConfigCCRainbowForest;
        RealisticBiomeCCBase stream = new RealisticBiomeCCBase(
                streamConfig, ChromatiCraft.rainbowRiver, ChromatiCraft.rainbowRiver,
                new TerrainVanillaRiver(), new SurfaceVanillaRiver(streamConfig));
        stream.addDeco(new DecoBaseBiomeDecorations());

        ci.cancel();
    }

}
