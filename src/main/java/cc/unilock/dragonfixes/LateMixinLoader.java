package cc.unilock.dragonfixes;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@LateMixin
public class LateMixinLoader implements ILateMixinLoader {
    @Override
    public String getMixinConfig() {
        return "mixins.dragonfixes.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        final boolean cavecontrol = loadedMods.contains("CaveControl");
        final boolean chromaticraft = loadedMods.contains("ChromatiCraft");
        final boolean dragonapi = loadedMods.contains("DragonAPI");
        final boolean rwg1 = loadedMods.contains("RWG");
        final boolean rwg2 = loadedMods.contains("rwg2");
        final boolean rtg = loadedMods.contains("RTG");
        final boolean climateControl = loadedMods.contains("climatecontrol");
        final boolean reactorcraft = loadedMods.contains("ReactorCraft");
        final boolean rotarycraft = loadedMods.contains("RotaryCraft");
        final boolean satisforestry = loadedMods.contains("Satisforestry");
        final boolean voidmonster = loadedMods.contains("VoidMonster");
        final boolean dragonrealmcore = loadedMods.contains("DragonRealmCore");

        List<String> mixins = new ArrayList<>();

        if (FMLLaunchHandler.side().isClient()) {
            if (chromaticraft) {
                mixins.add("chromaticraft.client.accessor.WarpPointAccessor");
                mixins.add("chromaticraft.client.ChromaClientEventControllerMixin");
                mixins.add("chromaticraft.client.ChromaShadersMixin");
                mixins.add("chromaticraft.client.ShaderProgramArtefactGlowMixin");
                mixins.add("chromaticraft.client.ChromaDimensionManagerMixin");
                mixins.add("chromaticraft.client.CrystalFurnaceHandlerMixin");
                mixins.add("chromaticraft.client.CrystalGlowRendererMixin");
                mixins.add("chromaticraft.client.EnchantDecompHandlerMixin");
                mixins.add("chromaticraft.client.FabricatorHandlerMixin");
                mixins.add("chromaticraft.client.BlockLightedLeafMixin");
                mixins.add("chromaticraft.client.WarpPointDataMixin");
            }
        }

        if (loadedMods.contains("arsmagica2")) {
            mixins.add("am2.ShrinkHandlerMixin");
            if (FMLLaunchHandler.side().isClient()) {
                mixins.add("am2.client.PotionHudMixin");
            }
        }
        if (cavecontrol) {
            mixins.add("cavecontrol.CaveLoaderMixin");
        }
        if (chromaticraft) {
            mixins.add("chromaticraft.accessor.CrystalBlockAccessor");
            mixins.add("chromaticraft.accessor.TileEntityCrystalBroadcasterAccessor");
            mixins.add("chromaticraft.accessor.RayTracerAccessor");
            mixins.add("chromaticraft.BiomeGlowingCliffsMixin");
            mixins.add("chromaticraft.CliffTerrainQueryScopeMixin");
            mixins.add("chromaticraft.GlowingCliffsColumnShaperMixin");
            mixins.add("chromaticraft.BlockChromaTileMixin");
            mixins.add("chromaticraft.BlockDecoFlowerMixin");
            mixins.add("chromaticraft.BlockTieredPlantMixin");
            mixins.add("chromaticraft.ChromaBlocksMixin");
            mixins.add("chromaticraft.CrystalTypeBlockMixin");
            mixins.add("chromaticraft.DungeonGeneratorMixin");
            mixins.add("chromaticraft.ItemBlockCrystalGlowMixin");
            mixins.add("chromaticraft.ProgressionManagerMixin");
            mixins.add("chromaticraft.PylonFinderMixin");
            mixins.add("chromaticraft.PylonGeneratorMixin");
            mixins.add("chromaticraft.TileEntityCrystalBroadcasterMixin");
            mixins.add("chromaticraft.TileEntityLumenAlvearyHumidityMatchingEffectMixin");
            mixins.add("chromaticraft.TileEntityLumenAlvearyTemperatureMatchingEffectMixin");
            mixins.add("chromaticraft.TileEntityPlayerDelegateMixin");
            mixins.add("chromaticraft.TileEntityWirelessPoweredMixin");
        }
        if (chromaticraft && loadedMods.contains("naturescompass")
                && ((rwg1 && DragonFixesConfig.fixRwg1ChromaBiomeIntegration)
                || (rwg2 && DragonFixesConfig.fixChromaBiomeIntegration)
                || (rtg && DragonFixesConfig.fixRtgChromaBiomeIntegration)
                || (climateControl && DragonFixesConfig.fixClimateControlChromaBiomeIntegration))) {
            mixins.add("naturescompass.StoredBiomeSearchMixin");
        }
        if (rwg1 && chromaticraft && DragonFixesConfig.fixRwg1ChromaBiomeIntegration) {
            mixins.add("rwg1.SupportCCMixin");
            mixins.add("rwg1.ChunkManagerRealisticMixin");
            mixins.add("rwg1.ChunkGeneratorRealisticMixin");
            mixins.add("rwg1.RealisticBiomeSupportMixin");
            if (loadedMods.contains("naturescompass")) {
                mixins.add("rwg1.BiomeSearchWorkerMixin");
            }
        }
        if (rwg2 && chromaticraft) {
            if (DragonFixesConfig.overrideChromaClimate || DragonFixesConfig.fixChromaBiomeIntegration) {
                mixins.add("rwg2.SupportCCMixin");
            }
            if (DragonFixesConfig.fixChromaBiomeIntegration) {
                mixins.add("rwg2.ChunkManagerRealisticMixin");
                mixins.add("rwg2.ChunkGeneratorRealisticMixin");
            }
        }
        if (rtg && chromaticraft) {
            mixins.add("rtg.RtgBiomeCacheAccessor");
        }
        if (rtg) {
            mixins.add("rtg.RtgCarverBiomeMixin");
        }
        if (rtg && loadedMods.contains("endlessids") && DragonFixesConfig.optimizeRtgEndlessIdsMapGen) {
            mixins.add("rtg.EndlessIdsMapGenProviderMixin");
            mixins.add("rtg.EndlessIdsMapGenBiomeMixin");
        }
        if (rtg && chromaticraft && DragonFixesConfig.fixRtgChromaBiomeIntegration) {
            mixins.add("rtg.RealisticBiomeCCBaseMixin");
            mixins.add("rtg.BiomeAnalyzerMixin");
            mixins.add("rtg.RealisticBiomePresenceTesterMixin");
            mixins.add("rtg.WorldChunkManagerRTGMixin");
            mixins.add("rtg.ChunkProviderRTGMixin");
            if (loadedMods.contains("naturescompass")) {
                mixins.add("rtg.BiomeSearchWorkerMixin");
            }
        }
        if (climateControl && chromaticraft && DragonFixesConfig.fixClimateControlChromaBiomeIntegration) {
            mixins.add("climatecontrol.BiomeSettingsMixin");
            mixins.add("climatecontrol.GenLayerShoreMixin");
            mixins.add("climatecontrol.GenLayerLowlandRiverMixMixin");
        }
        if (dragonapi) {
            mixins.add("dragonapi.AbstractSearchFoundPathMixin");
            mixins.add("dragonapi.accessor.BlockSetDataInvoker");
            mixins.add("dragonapi.WorldGenInterceptionRegistryMixin");
        }
        if (reactorcraft) {
            mixins.add("reactorcraft.BlockReactorTileMixin");
        }
        if (rotarycraft) {
            mixins.add("rotarycraft.BlockBasicMultiTEMixin");
            mixins.add("rotarycraft.RecipesDryingBedMixin");
            mixins.add("rotarycraft.TileEntityBedrockBreakerMixin");
        }
        if (satisforestry) {
            mixins.add("satisforestry.SFEventsMixin");
            mixins.add("satisforestry.TileShaftConnectionMixin");
        }
        if (voidmonster) {
            mixins.add("voidmonster.AmbientSoundGeneratorMixin");
            mixins.add("voidmonster.EntityVoidMonsterMixin");
            mixins.add("voidmonster.MonsterGeneratorMixin");
        }
        if (dragonrealmcore) {
            mixins.add("dragonrealmcore.BlockT2HexGeneratorMixin");
            mixins.add("dragonrealmcore.BlockT3HexGeneratorMixin");
            if (DragonFixesConfig.disableTerritoryStrongholdSystem) {
                mixins.add("cavecontrol.stronghold.ControllableStrongholdGenMixin");
                mixins.add("dragonrealmcore.stronghold.DREventsMixin");
                mixins.add("dragonrealmcore.stronghold.StrongholdSeedMixCommandMixin");
            }
            if (DragonFixesConfig.energizationManagerFix) {
                mixins.add("dragonrealmcore.EnergizationManagerMixin");
            }
        }

        return mixins;
    }
}
