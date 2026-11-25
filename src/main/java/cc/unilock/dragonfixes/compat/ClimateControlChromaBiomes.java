package cc.unilock.dragonfixes.compat;

import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.ModInteract.ReikaClimateControl;
import cc.unilock.dragonfixes.DragonFixes;
import climateControl.api.BiomeSettings;

import net.minecraft.world.biome.BiomeGenBase;

/** Restore native IDs if a saved Climate Control config overwrites them. */
public final class ClimateControlChromaBiomes {
    private ClimateControlChromaBiomes() {}

    public static void synchronize(BiomeSettings settings) {
        // DragonAPI replaces ReikasPackage.freshBiomeSetting() with its own
        // implementation. Do not create new entries, change weights, or modify
        // the activation and climate settings managed by that package.
        if (!((Object) settings instanceof ReikaClimateControl)) return;

        for (BiomeSettings.Element element : settings.elements()) {
            switch (element.name) {
                case "Ender Forest":
                    setRegisteredID(element, ChromatiCraft.enderforest);
                    break;
                case "Rainbow Forest":
                    setRegisteredID(element, ChromatiCraft.rainbowforest);
                    break;
                case "Luminous Cliffs":
                    setRegisteredID(element, ChromatiCraft.glowingcliffs);
                    break;
                default:
                    break;
            }
        }
    }

    private static void setRegisteredID(BiomeSettings.Element element, BiomeGenBase biome) {
        if (biome == null || element.biomeID().value() == biome.biomeID) return;
        int previous = element.biomeID().value();
        element.setIDFrom(biome);
        DragonFixes.LOGGER.info("Climate Control: restored native ID for {} ({} -> {})",
                element.name, previous, element.biomeID().value());
    }

}
