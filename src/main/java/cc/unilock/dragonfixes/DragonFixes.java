package cc.unilock.dragonfixes;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cc.unilock.dragonfixes.debug.WorldStartupProfiler;

import cc.unilock.dragonfixes.compat.SubmergeableLiquidLoggedCompat;
import cc.unilock.dragonfixes.compat.AE2CSVExportBridge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = DragonFixes.MOD_ID, version = Tags.VERSION, name = "DragonFixes", acceptedMinecraftVersions = "[1.7.10]", acceptableRemoteVersions = "*", dependencies = "after:CaveControl;"+"after:ChromatiCraft;"+"after:DragonRealmCore;"+"after:gtnhlib;"+"after:liquidlogged;")
public class DragonFixes {
    public static final String MOD_ID = "dragonfixes";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        if (DragonFixesConfig.enableWorldStartupProfiler) {
            WorldStartupProfiler.start(Thread.currentThread());
        }
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        WorldStartupProfiler.stop("server started");
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        WorldStartupProfiler.stop("server stopped");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        SubmergeableLiquidLoggedCompat.register();
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        // All mods have finished init/postInit. It is now safe for AE2 to
        // enumerate other mods' creative-tab items on its CSV export thread.
        cc.unilock.dragonfixes.compat.am2.EtheriumCompat.initialize();
        AE2CSVExportBridge.startDeferred();
    }
}
