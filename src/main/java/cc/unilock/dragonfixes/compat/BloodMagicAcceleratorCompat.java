package cc.unilock.dragonfixes.compat;

import Reika.ChromatiCraft.API.AdjacencyUpgradeAPI;
import Reika.ChromatiCraft.API.ChromatiAPI;
import cc.unilock.dragonfixes.DragonFixes;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;

/** Restores Blood Magic's two accelerator exclusions on the V33 API. */
public final class BloodMagicAcceleratorCompat {
    private BloodMagicAcceleratorCompat() {}

    public static void register() {
        if (!Loader.isModLoaded("AWWayofTime") || !Loader.isModLoaded("ChromatiCraft")
                || Launch.classLoader.getResource("Reika/ChromatiCraft/API/AcceleratorBlacklist.class") != null
                || Launch.classLoader.getResource("Reika/ChromatiCraft/API/AdjacencyUpgradeAPI.class") == null) {
            return;
        }
        try {
            ChromatiAPI core = ChromatiAPI.getAPI();
            if (core == null || core.adjacency() == null) {
                DragonFixes.LOGGER.warn("ChromatiCraft adjacency API unavailable; Blood Magic exclusions not registered");
                return;
            }
            AdjacencyUpgradeAPI api = core.adjacency();
            registerTile(api, "TEAltar", "Altar");
            registerTile(api, "TEMasterStone", "masterStone");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            DragonFixes.LOGGER.error("Unable to restore Blood Magic accelerator blacklist", e);
        }
    }

    private static void registerTile(AdjacencyUpgradeAPI api, String tile, String blockName)
            throws ClassNotFoundException {
        Class<? extends TileEntity> type = Class.forName(
                "WayofTime.alchemicalWizardry.common.tileEntity." + tile,
                false, Launch.classLoader).asSubclass(TileEntity.class);
        Block block = GameRegistry.findBlock("AWWayofTime", blockName);
        ItemStack display = block == null ? null : new ItemStack(block);
        api.addAcceleratorBlacklist(type, tile, display, AdjacencyUpgradeAPI.BlacklistReason.EXPLOIT);
        DragonFixes.LOGGER.info("Restored Blood Magic accelerator blacklist for {}", tile);
    }
}
