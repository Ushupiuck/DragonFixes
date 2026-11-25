package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.Instantiable.Event.GenLayerRiverEvent;
import climateControl.customGenLayer.GenLayerLowlandRiverMix;
import com.llamalad7.mixinextras.sugar.Local;
import cpw.mods.fml.common.eventhandler.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;

@Mixin(GenLayerLowlandRiverMix.class)
public abstract class GenLayerLowlandRiverMixMixin {
    @Inject(method = "getInts", at = @At("RETURN"), require = 1)
    private void dragonfixes$forwardChromaRivers(int x, int z, int width, int height,
            CallbackInfoReturnable<int[]> cir, @Local(ordinal = 0) int[] parentBiomes) {
        if (ChromatiCraft.glowingcliffs == null || ChromatiCraft.glowingcliffsEdge == null
                || ChromatiCraft.rainbowforest == null || ChromatiCraft.rainbowRiver == null) return;
        int[] result = cir.getReturnValue();
        int cliffs = ChromatiCraft.glowingcliffs.biomeID;
        int shores = ChromatiCraft.glowingcliffsEdge.biomeID;
        int forest = ChromatiCraft.rainbowforest.biomeID;

        for (int i = 0; i < width * height; i++) {
            // Only forward rivers Climate Control actually elected to place.
            // Do not re-enable suppressed rivers, including in RTG worlds.
            int planned = result[i];
            if (planned != BiomeGenBase.river.biomeID && planned != BiomeGenBase.frozenRiver.biomeID) continue;
            int parent = parentBiomes[i];
            if (parent != cliffs && parent != shores && parent != forest) continue;

            // Reuse ChromatiCraft's native listener, not a second copy of its rules.
            // Construct the event directly so IDs are not truncated by vanilla masking.
            GenLayerRiverEvent event = new GenLayerRiverEvent(parent, true);
            event.riverBiomeID = planned;
            MinecraftForge.EVENT_BUS.post(event);
            result[i] = event.getResult() == Event.Result.DENY ? parent : event.riverBiomeID;
        }
    }
}
