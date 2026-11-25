package cc.unilock.dragonfixes.compat;

import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.Instantiable.Event.GenLayerBeachEvent;
import Reika.DragonAPI.Instantiable.Event.GenLayerRiverEvent;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;

/** Adapt generators that bypass DragonAPI's vanilla GenLayer ASM hooks. */
public final class ChromaBiomeEvents {
    private ChromaBiomeEvents() {}

    public static boolean isCliff(BiomeGenBase biome) {
        return biome != null && (biome == ChromatiCraft.glowingcliffs || biome == ChromatiCraft.glowingcliffsEdge);
    }

    public static boolean isRiverParent(BiomeGenBase biome) {
        return biome != null && (isCliff(biome) || biome == ChromatiCraft.rainbowforest);
    }

    public static boolean isVanillaBeach(int id) {
        return id == BiomeGenBase.beach.biomeID || id == BiomeGenBase.stoneBeach.biomeID
                || id == BiomeGenBase.coldBeach.biomeID;
    }

    public static BiomeGenBase river(BiomeGenBase parent, BiomeGenBase planned) {
        if (!isRiverParent(parent) || planned == null) return planned;
        GenLayerRiverEvent event = new GenLayerRiverEvent(parent.biomeID, true);
        event.riverBiomeID = planned.biomeID;
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return parent;
        return registeredOrDefault(event.riverBiomeID, planned);
    }

    public static BiomeGenBase beach(BiomeGenBase parent, BiomeGenBase planned) {
        if (parent == null || parent != ChromatiCraft.glowingcliffs || planned == null) return planned;
        return registeredOrDefault(GenLayerBeachEvent.fire(parent, planned.biomeID), planned);
    }

    private static BiomeGenBase registeredOrDefault(int id, BiomeGenBase fallback) {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        return id >= 0 && id < biomes.length && biomes[id] != null ? biomes[id] : fallback;
    }
}
