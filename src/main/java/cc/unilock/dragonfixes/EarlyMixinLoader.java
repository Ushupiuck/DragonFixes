package cc.unilock.dragonfixes;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.Launch;

import java.lang.reflect.Field;
import java.util.Collections;
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
        if (DragonFixesConfig.disableTickInterceptASM && loadedCoreMods.contains(DRAGON_REALM_ASM)) {
            disableTickIntercept();
        }
        return Collections.emptyList();
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
            "cc.unilock.dragonfixes.asm.dragonapi.ReikaJavaLibraryASM"
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
