package cc.unilock.dragonfixes.debug;

/** Exclusive, innermost recognized subsystem per sample. Generic provider
 * frames are deliberately deferred so nested features receive attribution. */
public final class StartupSubsystems {
    private StartupSubsystems() {}

    public static String classify(StackTraceElement[] trace) {
        String provider = null;
        for (StackTraceElement frame : trace) {
            String c = frame.getClassName(), m = frame.getMethodName();
            if (c.contains("GlowingCliffsColumnShaper") || c.contains("CliffBiomeNeighborhood")
                    || m.contains("blendTerrainEdgesAndGenCliffs")) return "ChromatiCraft cliff shaping";
            if (m.contains("injectTheFunny") || m.contains("deduplicateEndlessIdsMapGen")
                    || (c.startsWith("rtg.") && (m.equals("generateMapGen") || m.equals("rMapVolcanoes")
                    || c.contains("WorldGenVolcano")))) return "RTG map/volcano generation";
            if (c.contains("MapGenCaves") || c.contains("MapGenRavine")) return "Caves/ravines";
            if (c.contains("PylonGenerator") || c.contains("PylonFinder")) return "ChromatiCraft pylons";
            if (c.contains("AnvilChunkLoader") || c.contains("RegionFile")) return "Chunk storage";
            if (c.contains("LoadingScreenChunkColorSampler") || c.contains("LoadingScreenWorldGenTracker")) return "Loading preview";
            if (c.contains("GenLayer") && (c.startsWith("climateControl.") || c.startsWith("net.minecraft."))) {
                // Preserve attribution to a more specific caller, e.g. cliff shaping.
                if (provider == null) provider = "Biome layers";
            }
            if (c.startsWith("rtg.") && (c.contains("LandscapeGenerator") || c.contains("BiomeAnalyzer"))) {
                if (provider == null) provider = "RTG landscape/biome repair";
            }
            if (c.startsWith("rtg.") && c.contains("ChunkProvider") && provider == null) provider = "RTG other terrain";
            if ((c.startsWith("rwg.") || c.startsWith("rwg2.")) && c.contains("Chunk") && provider == null) provider = "RWG terrain/biomes";
            if ((c.startsWith("micdoodle8.") || c.startsWith("bwcrossmod.galacticraft.")
                    || c.startsWith("galaxyspace.")) && provider == null) provider = "Galacticraft/planet generation";
            if (c.equals("cpw.mods.fml.common.registry.GameRegistry") && m.equals("generateWorld")
                    && provider == null) provider = "Forge mod world generators";
            if (c.contains("ChunkProvider") && provider == null) provider = "Other chunk generation/loading";
        }
        return provider == null ? "Other startup work" : provider;
    }
}
