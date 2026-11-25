package cc.unilock.dragonfixes.compat;

import Reika.ChromatiCraft.ChromatiCraft;
import climateControl.api.BiomeSettings;
import climateControl.biomeSettings.ReikasBiomeSettings;

import net.minecraft.world.biome.BiomeGenBase;

/** Synchronize IDs after Climate Control reads a world-specific configuration. */
public final class ClimateControlChromaBiomes {
    private ClimateControlChromaBiomes() {}

    public static void synchronize(BiomeSettings settings) {
        if (!(settings instanceof ReikasBiomeSettings)) return;

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
                case "Island":
                case "Plains":
                case "Skyland":
                    // These no longer exist in ChromatiCraft. Never select an unrelated
                    // mod's biome just because it occupies one of their old numeric IDs.
                    element.biomeID().set(-1);
                    element.biomeIncidences().set(0);
                    break;
                default:
                    break;
            }
        }
    }

    private static void setRegisteredID(BiomeSettings.Element element, BiomeGenBase biome) {
        // Early config loading may precede ChromatiCraft's biome registration.
        if (biome != null) element.setIDFrom(biome);
    }

    public static boolean isVanillaBeach(int id) {
        return id == BiomeGenBase.beach.biomeID
                || id == BiomeGenBase.stoneBeach.biomeID
                || id == BiomeGenBase.coldBeach.biomeID;
    }
}
