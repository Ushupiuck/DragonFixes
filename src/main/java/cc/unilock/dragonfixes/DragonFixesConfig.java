package cc.unilock.dragonfixes;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "dragonfixes")
public class DragonFixesConfig {
    @Config.Comment("Experimental RTG/EndlessIDs map-generation correction: run map generation once per distinct biome in ascending ID order, as RTG originally did. May change volcano/structure terrain relative to chunks made by the old repeated loop. Test on new worlds.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean optimizeRtgEndlessIdsMapGen;

    @Config.Comment("Opt-in wall-clock stack sampling of the integrated/dedicated server during startup. Reports every five seconds and on startup completion. No per-chunk hooks or timings when disabled.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableWorldStartupProfiler;

    @Config.Comment("Enables troubleshooting logs and biome generation diagnostics for RWG1, RWG2, RTG and Climate Control. Does not change biome placement or compatibility fixes.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableBiomeGenerationDiagnostics;

    @Config.Comment("Disables the hardcoded checks for Void Monster spawning in certain dimensions, instead always respecting the config")
    @Config.DefaultBoolean(false)
    public static boolean disableVoidMonsterHardcode;

    @Config.Comment("Disables the hardcoded check for spawning the Void Monster on the Nether roof")
    @Config.DefaultBoolean(false)
    public static boolean disableVoidMonsterNetherRoof;

    @Config.Comment("Suppresses Reika's native fluid tank tooltip lines in ChromatiCraft, RotaryCraft and ReactorCraft to avoid duplicate information when WAILAPlugins' IFluidHandler provider is enabled.")
    @Config.DefaultBoolean(false)
    public static boolean disableWailaPlugins;

    @Config.Comment("Disables DragonRealmCore's TickIntercept ASM (pauses the server when no players are online; disabled by default in DragonRealmCore config)")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean disableTickInterceptASM;

    @Config.Comment("Disables the TerritoryZone-based Stronghold generation system from DragonRealmCore")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean disableTerritoryStrongholdSystem;

    @Config.Comment("Prevents crashes in `EnergizationManager#getEnergizationLevel`; breaks DragonRealmCore's \"Atmospheric Permeability Mediation\" mechanic")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean energizationManagerFix;

    @Config.Comment("Replaces \"rubber\" with \"gemLatex\" in `RecipesDryingBed#addPostLoadRecipes` - avoids a progression skip in HBM's NTM")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean replaceRubberWithGemLatex;

    @Config.Comment("Uses DragonAPI's native ChromatiCraft biome registration in Climate Control, refreshes stale biome IDs, and forwards custom shoreline and river placement to DragonAPI events. Changing this can cause borders between old and new chunks.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"climatecontrol", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static boolean fixClimateControlChromaBiomeIntegration;

    @Config.Comment("Fixes ChromatiCraft biome discovery, Shores and Rainbow Stream support, and biome repair in RTG. Changing this can cause borders between old and new chunks.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"RTG", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static boolean fixRtgChromaBiomeIntegration;

    @Config.Comment("Registers native ChromatiCraft biomes in RWG1, derives cliff shores from adjacent water, and forwards river placement to DragonAPI. Changes biome lists; use new worlds or expect chunk borders.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"RWG", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static boolean fixRwg1ChromaBiomeIntegration;

    @Config.Comment("Restores ChromatiCraft's parent-dependent Luminous Cliffs Shores and custom river biome rules in RWG2. Changing this can cause borders between old and new chunks.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"rwg2", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static boolean fixChromaBiomeIntegration;

    @Config.Comment("Overrides RWG1/RWG2's climate pools for ChromatiCraft biomes. Changing these settings can cause borders between old and new chunks. Disable to retain the original HOT climate category.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean overrideChromaClimate;

    @Config.Comment("RWG1/RWG2 climate pools for Rainbow Forest. Valid values: SNOW, COLD (includes temperate), HOT, WET. Multiple values are allowed. Empty or invalid lists retain RWG1/RWG2's original placement.")
    @Config.DefaultStringList({"COLD"})
    @Config.RequiresMcRestart
    public static String[] rainbowForestClimate;

    @Config.Comment("RWG1/RWG2 climate pools for Ender Forest. Valid values: SNOW, COLD (includes temperate), HOT, WET. Multiple values are allowed. Empty or invalid lists retain RWG1/RWG2's original placement.")
    @Config.DefaultStringList({"COLD"})
    @Config.RequiresMcRestart
    public static String[] enderForestClimate;

    @Config.Comment("RWG1/RWG2 climate pools for Luminous Cliffs. Shores follow their parent when the corresponding RWG integration fix is enabled. In RWG1, the climate override also requires fixRwg1ChromaBiomeIntegration. Valid values: SNOW, COLD (includes temperate), HOT, WET. Multiple values are allowed. Empty or invalid lists retain RWG1/RWG2's original placement.")
    @Config.DefaultStringList({"COLD"})
    @Config.RequiresMcRestart
    public static String[] luminousCliffsClimate;
}
