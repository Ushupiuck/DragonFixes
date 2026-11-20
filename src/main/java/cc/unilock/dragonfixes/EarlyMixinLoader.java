package cc.unilock.dragonfixes;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.Launch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1002) // DragonAPI and DragonRealmCore are 1001
public class EarlyMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {
    private static final String DRAGON_REALM_ASM = "Reika.DragonRealmCore.DragonRealmASM";
    private static final String REIKA_JVM_PARSER = "Reika.DragonAPI.Libraries.Java.ReikaJVMParser";
    private static final String DISABLE_TICK_INTERCEPT = "-DragonAPI_disable_ASM_TICKINTERCEPT";

    static {
        try {
            ConfigurationManager.registerConfig(DragonFixesConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMixinConfig() {
        return "mixins.dragonfixes.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        fixReikaJavaVersion();
        if (DragonFixesConfig.disableTickInterceptASM && loadedCoreMods.contains(DRAGON_REALM_ASM)) {
            disableTickIntercept();
        }
        List<String> mixins = new ArrayList<>();
        if (Launch.classLoader.getResource("com/colen/tempora/mixins/MixinWorldServer.class") != null
                && Launch.classLoader.getResource(
                        "Reika/DragonAPI/Instantiable/Event/BlockTickEvent.class") != null) {
            mixins.add("world.MixinWorldServerTemporaCompat");
        }

        // ChromatiCraft assumes that merely having PlayerAPI installed means it
        // renamed EntityPlayerSP's push-out method. Modern combinations do not.
        if (Launch.classLoader.getResource(
                "Reika/ChromatiCraft/Auxiliary/ChromaASMHandler.class") != null
                && Launch.classLoader.getResource("api/player/forge/PlayerAPITransformer.class") != null
                && replaceBrokenChromaPushoutPatch()) {
            mixins.add("chromaticraft.MixinEntityPlayerSPOreClip");
        }
        // AE2's optional CSV exporter enumerates creative-tab subitems from a
        // worker thread during init. ChromatiCraft's tiered plants transitively
        // initialize its research singletons and can permanently poison them.
        // Keep exporting, but start it after all mods finish initialization.
        if (Launch.classLoader.getResource("appeng/core/AppEng.class") != null
                && Launch.classLoader.getResource(
                        "Reika/ChromatiCraft/Magic/Progression/ChromaResearchManager.class") != null) {
            mixins.add("ae2.MixinAppEngDeferCSVExport");
        }

        // If LiquidLogged actually stores the fluid, don't let Reika paint a
        // second, visual-only water surface over the same block.
        if (Launch.classLoader.getResource(
                "com/gtnewhorizons/liquidlogged/api/LiquidLoggedAPI.class") != null
                && Launch.classLoader.getResource(
                        "Reika/DragonAPI/Auxiliary/DragonAPIEventWatcher.class") != null) {
            mixins.add("dragonapi.MixinDragonAPIEventWatcherLiquidLogged");
        }
        return mixins;
    }

    private static boolean replaceBrokenChromaPushoutPatch() {
        try {
            Class<?> transformer = Class.forName(
                    "Reika.ChromatiCraft.Auxiliary.ChromaASMHandler$ASMExecutor", true, Launch.classLoader);
            Field classesField = transformer.getDeclaredField("classes");
            classesField.setAccessible(true);
            Object value = classesField.get(null);
            if (!(value instanceof Map)) {
                DragonFixes.LOGGER.warn("ChromatiCraft patcher table is not a Map; leaving it alone");
                return false;
            }

            Map<?, ?> patches = (Map<?, ?>) value;
            Iterator<? extends Map.Entry<?, ?>> iterator = patches.entrySet().iterator();
            while (iterator.hasNext()) {
                Object patch = iterator.next().getValue();
                if (patch instanceof Enum && "ENTITYPUSHOUT".equals(((Enum<?>) patch).name())) {
                    iterator.remove();
                    DragonFixes.LOGGER.info("Replacing ChromatiCraft ENTITYPUSHOUT ASM "
                            + "with a targeted OreClip Mixin");
                    return true;
                }
            }
            DragonFixes.LOGGER.warn("ChromatiCraft ENTITYPUSHOUT patch was not present in the table");
        } catch (ReflectiveOperationException | SecurityException e) {
            DragonFixes.LOGGER.warn("Unable to replace ChromatiCraft's PlayerAPI-sensitive patch", e);
        }
        return false;
    }

    /**
     * ReikaJVMParser assumes all JVM versions start with "1." and drops the first
     * component. On Java 26 this turns [26, 0, 2, 1] into [0, 2, 1].
     * Correct its existing mutable cache without altering Reika's class bytecode.
     */
    private static void fixReikaJavaVersion() {
        String specification = System.getProperty("java.specification.version", "1.8");
        int major;
        try {
            major = Integer.parseInt(specification.startsWith("1.")
                    ? specification.substring(2) : specification.split("\\.")[0]);
        } catch (NumberFormatException e) {
            DragonFixes.LOGGER.warn("Unrecognized Java specification version: {}", specification, e);
            return;
        }
        if (major <= 8) {
            return; // Reika's legacy 1.x parser already handles Java 8 correctly.
        }

        try {
            String version = System.getProperty("java.version", "");
            String[] components = version.replaceAll("[^0-9.]", ".").split("\\.");
            Class<?> parser = Class.forName(REIKA_JVM_PARSER, true, Launch.classLoader);
            Field versionField = parser.getDeclaredField("version");
            versionField.setAccessible(true);
            int[] cached = (int[]) versionField.get(null);
            if (cached == null || cached.length == 0) {
                DragonFixes.LOGGER.warn("ReikaJVMParser.version is empty; skipping correction");
                return;
            }
            int[] before = cached.clone();
            cached[0] = major;
            for (int i = 1; i < cached.length && i < components.length; i++) {
                cached[i] = Integer.parseInt(components[i]);
            }
            if (!Arrays.equals(before, cached)) {
                DragonFixes.LOGGER.info("Corrected Reika JVM version {} -> {} (runtime {})",
                        Arrays.toString(before), Arrays.toString(cached), version);
            }
        } catch (ReflectiveOperationException | SecurityException | NumberFormatException e) {
            DragonFixes.LOGGER.warn("Unable to correct Reika's cached Java version", e);
        }
    }

    private static void disableTickIntercept() {
        try {
            // Resolve Reika's class from the Minecraft launch classloader rather than linking it
            // while this early loading plugin itself is being initialized.
            Class<?> parser = Class.forName(REIKA_JVM_PARSER, false, Launch.classLoader);
            Field argsField = parser.getDeclaredField("args");
            argsField.setAccessible(true);
            Object value = argsField.get(null);
            if (!(value instanceof Set)) {
                throw new IllegalStateException("ReikaJVMParser.args is not a Set");
            }

            @SuppressWarnings("unchecked")
            Set<String> args = (Set<String>) value;
            args.add(DISABLE_TICK_INTERCEPT);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("Could not disable DragonRealmCore's TickIntercept ASM", e);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
            "cc.unilock.dragonfixes.asm.dragonapi.ReikaJavaLibraryASM",
            "cc.unilock.dragonfixes.asm.dragonapi.WorldServerTemporaCompatASM",
            "cc.unilock.dragonfixes.asm.dragonapi.BlockLiquidLiquidLoggedCompatASM",
            "cc.unilock.dragonfixes.asm.dragonapi.WorldRendererLiquidLoggedCompatASM",
            "cc.unilock.dragonfixes.asm.dragonapi.ReikaOreDictionaryLogASM"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // NO-OP
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
