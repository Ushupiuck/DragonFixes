package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.ModInteract.ReikaClimateControl;
import cc.unilock.dragonfixes.DragonFixes;
import climateControl.api.BiomeRandomizer;
import climateControl.api.BiomeSettings;
import climateControl.api.Climate;
import climateControl.api.ClimateDistribution;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BiomeRandomizer.class, remap = false)
public abstract class BiomeRandomizerMixin {
    @Unique private static final Set<String> dragonfixes$reportedPools = ConcurrentHashMap.newKeySet();
    @Unique private Map<String, int[]> dragonfixes$weights;

    // Observe the actual weights consumed by Climate Control, not the earlier
    // registration defaults. These have already been split among climate pools.
    @Inject(method = "add", at = @At("HEAD"), require = 1)
    private void dragonfixes$recordIncidence(ClimateDistribution.Incidence incidence, CallbackInfo ci) {
        if (dragonfixes$weights == null) dragonfixes$weights = new LinkedHashMap<>();
        int[] weights = dragonfixes$weights.computeIfAbsent(incidence.climate.name, key -> new int[4]);
        weights[0] += incidence.incidence;
        BiomeGenBase biome = BiomeGenBase.getBiome(incidence.biome);
        if (biome == null) return;
        if (biome == ChromatiCraft.rainbowforest) weights[1] += incidence.incidence;
        if (biome == ChromatiCraft.enderforest) weights[2] += incidence.incidence;
        if (biome == ChromatiCraft.glowingcliffs) weights[3] += incidence.incidence;
    }

    @Inject(method = "<init>(Ljava/util/Collection;)V", at = @At("RETURN"), require = 1)
    private void dragonfixes$reportPools(Collection<BiomeSettings> settings, CallbackInfo ci) {
        if (ChromatiCraft.glowingcliffs == null || dragonfixes$weights == null) return;
        StringBuilder report = new StringBuilder();
        for (BiomeSettings setting : settings) {
            if ((Object) setting instanceof ReikaClimateControl) {
                report.append("Reika package active=").append(setting.biomesAreActive()).append("; ");
            }
        }
        for (Climate climate : new Climate[] { Climate.COOL, Climate.WARM, Climate.HOT, Climate.SNOWY }) {
            int[] weights = dragonfixes$weights.get(climate.name);
            if (weights == null) weights = new int[4];
            report.append(climate.name).append(" [total=").append(weights[0])
                    .append(", Rainbow Forest=").append(weights[1])
                    .append(", Ender Forest=").append(weights[2])
                    .append(", Luminous Cliffs=").append(weights[3]).append("]; ");
        }
        String message = report.toString();
        if (dragonfixes$reportedPools.add(message)) {
            DragonFixes.LOGGER.info("Climate Control effective biome pools: {}", message);
        }
        dragonfixes$weights = null;
    }
}
