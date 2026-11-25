package cc.unilock.dragonfixes.compat;

import java.util.EnumSet;
import java.util.Locale;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;
import net.minecraft.world.biome.BiomeGenBase;

/** Both RWG versions have the same four main climate categories, but different enum types. */
public final class RwgChromaClimate {
    private RwgChromaClimate() {}

    public static <E extends Enum<E>> EnumSet<E> categories(BiomeGenBase biome, E fallback, String generator) {
        if (!DragonFixesConfig.overrideChromaClimate) return EnumSet.of(fallback);
        String[] configured;
        if (biome == ChromatiCraft.rainbowforest) configured = DragonFixesConfig.rainbowForestClimate;
        else if (biome == ChromatiCraft.enderforest) configured = DragonFixesConfig.enderForestClimate;
        else if (ChromaBiomeEvents.isCliff(biome)) configured = DragonFixesConfig.luminousCliffsClimate;
        else return EnumSet.of(fallback);

        EnumSet<E> result = EnumSet.noneOf(fallback.getDeclaringClass());
        if (configured != null) {
            for (String value : configured) {
                String name = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
                switch (name) {
                    case "SNOW":
                    case "COLD":
                    case "HOT":
                    case "WET":
                        result.add(Enum.valueOf(fallback.getDeclaringClass(), name));
                        break;
                    default:
                        DragonFixes.LOGGER.warn("{}: invalid climate '{}' for {}; retaining {}", generator, value,
                                biome.biomeName, fallback);
                        return EnumSet.of(fallback);
                }
            }
        }
        if (result.isEmpty()) {
            DragonFixes.LOGGER.warn("{}: empty climate list for {}; retaining {}", generator, biome.biomeName, fallback);
            return EnumSet.of(fallback);
        }
        DragonFixes.LOGGER.info("{} climate override: {} -> {}", generator, biome.biomeName, result);
        return result;
    }
}
