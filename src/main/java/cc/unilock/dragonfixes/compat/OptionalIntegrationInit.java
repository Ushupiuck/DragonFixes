package cc.unilock.dragonfixes.compat;

import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import cc.unilock.dragonfixes.DragonFixes;
import cpw.mods.fml.common.Loader;
import net.minecraft.launchwrapper.Launch;

public final class OptionalIntegrationInit {
    private OptionalIntegrationInit() {}

    public static void initializeIfLoaded(String className, String modId) {
        if (!Loader.isModLoaded(modId)) {
            DragonFixes.LOGGER.info("Skipping DragonAPI optional integration {} ({} not installed)", className, modId);
            return;
        }
        try {
            ReikaJavaLibrary.initClass(Class.forName(className, false, Launch.classLoader), true);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Installed " + modId + " integration class missing: " + className, e);
        }
    }
}
