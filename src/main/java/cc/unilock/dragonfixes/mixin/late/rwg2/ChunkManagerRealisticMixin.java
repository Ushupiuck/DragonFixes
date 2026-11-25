package cc.unilock.dragonfixes.mixin.late.rwg2;

import java.util.ArrayList;

import cc.unilock.dragonfixes.compat.RwgChromaBiomes;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import rwg2.biomes.realistic.RealisticBiomeBase;
import rwg2.world.ChunkManagerRealistic;

@Mixin(value = ChunkManagerRealistic.class, remap = false)
public abstract class ChunkManagerRealisticMixin {
    @Shadow private boolean continental;
    @Shadow private ArrayList<RealisticBiomeBase>[] littoralBiomes;
    @Shadow public abstract float getContinentValue(int x, int z);

    @Shadow
    private int getClimateAt(int x, int z) {
        throw new AssertionError();
    }

    @Shadow
    private RealisticBiomeBase getLandBiomeAt(int x, int z, int climate) {
        throw new AssertionError();
    }

    // Immediately before the cache-size check: the selected biome is available, and both caches
    // are still populated by RWG2 itself. The first biomeDataMap access is the cache lookup.
    @Inject(
            method = "getBiomeDataAt(II)Lrwg2/biomes/realistic/RealisticBiomeBase;",
            at = @At(value = "FIELD", target = "Lrwg2/world/ChunkManagerRealistic;biomeDataMap:Lgnu/trove/map/hash/TLongObjectHashMap;", opcode = Opcodes.GETFIELD, ordinal = 1),
            require = 1)
    private void dragonfixes$deriveCliffShores(int x, int z, CallbackInfoReturnable<RealisticBiomeBase> cir,
            @Local(ordinal = 0) LocalRef<RealisticBiomeBase> output) {
        RealisticBiomeBase cliffs = RwgChromaBiomes.cliffs;
        RealisticBiomeBase shores = RwgChromaBiomes.shores;
        if (!continental || cliffs == null || shores == null) return;

        // Match RWG2's existing coastal transition, not its much wider independent littoral pool.
        float continent = getContinentValue(x, z);
        if (continent < 0f || continent >= 24f) return;

        RealisticBiomeBase selected = output.get();
        if (selected == cliffs) {
            output.set(shores);
            return;
        }

        int climate = getClimateAt(x, z);
        // Only replace generic coast or climate littoral assignments. Preserve other selected
        // biomes, including ocean and volcanic landmarks.
        if (selected != RealisticBiomeBase.coastDunes && selected != RealisticBiomeBase.coastIce
                && !littoralBiomes[climate - 1].contains(selected)) return;
        if (getLandBiomeAt(x, z, climate) == cliffs) {
            output.set(shores);
        }
    }
}
