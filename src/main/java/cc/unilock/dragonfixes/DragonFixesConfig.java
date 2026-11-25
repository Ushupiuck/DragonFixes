package cc.unilock.dragonfixes;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "dragonfixes")
public class DragonFixesConfig {
    @Config.Comment("Disables the hardcoded checks for Void Monster spawning in certain dimensions, instead always respecting the config")
    @Config.DefaultBoolean(false)
    public static boolean disableVoidMonsterHardcode;

    @Config.Comment("Disables the hardcoded check for spawning the Void Monster on the Nether roof")
    @Config.DefaultBoolean(false)
    public static boolean disableVoidMonsterNetherRoof;

    @Config.Comment("Disables IFluidHandler WAILA plugins from Reika's mods, in favor of something better")
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
    @Config.Comment("Overrides RWG2's climate pools for ChromatiCraft biomes. Changing these settings can cause borders between old and new chunks. Disable to retain RWG2's original placement.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"rwg2", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static boolean overrideChromaClimate;

    @Config.Comment("RWG2 climate pools for Rainbow Forest. Valid values: SNOW, COLD (includes temperate), HOT, WET. Multiple values are allowed. Empty or invalid lists retain RWG2's original placement.")
    @Config.DefaultStringList({"COLD"})
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"rwg2", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static String[] rainbowForestClimate;

    @Config.Comment("RWG2 climate pools for Ender Forest. Valid values: SNOW, COLD (includes temperate), HOT, WET. Multiple values are allowed. Empty or invalid lists retain RWG2's original placement.")
    @Config.DefaultStringList({"COLD"})
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"rwg2", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static String[] enderForestClimate;

    @Config.Comment("RWG2 climate pools for Luminous Cliffs and Luminous Cliffs Shores. Valid values: SNOW, COLD (includes temperate), HOT, WET. Multiple values are allowed. Empty or invalid lists retain RWG2's original placement.")
    @Config.DefaultStringList({"COLD"})
    @Config.RequiresMcRestart
    @Config.RequiresMod(value = {"rwg2", "ChromatiCraft"}, mode = Config.RequiresMod.Mode.AND)
    public static String[] luminousCliffsClimate;
}
