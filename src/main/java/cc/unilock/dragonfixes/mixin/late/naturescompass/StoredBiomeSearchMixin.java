package cc.unilock.dragonfixes.mixin.late.naturescompass;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.compat.StoredBiomeLookup;
import cc.unilock.dragonfixes.compat.CompassBiomePrediction;
import com.chaosthedude.naturescompass.util.BiomeSearchWorker;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BiomeSearchWorker.class, remap = false)
public abstract class StoredBiomeSearchMixin {
    @Shadow public World world;
    @Shadow public BiomeGenBase biome;
    @Shadow public int x;
    @Shadow public int z;
    @Shadow public boolean finished;
    @Shadow private void finish(boolean found) { throw new AssertionError(); }

    @Unique private boolean dragonfixes$tentative;
    @Unique private boolean dragonfixes$sampleTentative;
    @Unique private int dragonfixes$retries;
    @Unique private int dragonfixes$previousX;
    @Unique private int dragonfixes$previousZ;
    @Unique private BiomeGenBase dragonfixes$forcedBiome;

    @Unique
    private boolean dragonfixes$enabled() {
        return !world.isRemote && biome != null
                && (biome == ChromatiCraft.rainbowforest || biome == ChromatiCraft.rainbowRiver
                || biome == ChromatiCraft.enderforest || biome == ChromatiCraft.glowingcliffs
                || biome == ChromatiCraft.glowingcliffsEdge);
    }

    @WrapMethod(method = "hasWork")
    private boolean dragonfixes$keepTentativeWorker(Operation<Boolean> original) {
        return !finished && (dragonfixes$tentative || original.call());
    }

    @WrapMethod(method = "doWork")
    private boolean dragonfixes$revalidate(Operation<Boolean> original) {
        if (!dragonfixes$enabled()) return original.call();
        if (dragonfixes$tentative) {
            BiomeGenBase actual = null;
            int stored = StoredBiomeLookup.UNKNOWN;
            if (world.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
                actual = world.getBiomeGenForCoordsBody(x, z);
            } else {
                stored = StoredBiomeLookup.read(world, x, z);
                if (StoredBiomeLookup.isKnown(stored)) actual = BiomeGenBase.getBiome(StoredBiomeLookup.id(stored));
            }
            if (actual != null && actual != biome) {
                // Vanilla returned before advancing its spiral counters. Replay
                // that same sample with the authoritative answer to advance once.
                dragonfixes$tentative = false;
                x = dragonfixes$previousX;
                z = dragonfixes$previousZ;
                dragonfixes$forcedBiome = actual;
            } else if (actual != null && !StoredBiomeLookup.isPending(stored)
                    || stored == StoredBiomeLookup.ABSENT || ++dragonfixes$retries >= 3) {
                dragonfixes$tentative = false;
                finish(true);
                return false;
            } else {
                return false; // Yield until the next server tick, not a busy loop.
            }
        }
        dragonfixes$previousX = x;
        dragonfixes$previousZ = z;
        dragonfixes$sampleTentative = false;
        try {
            return original.call();
        } finally {
            dragonfixes$forcedBiome = null;
        }
    }

    // Own the lookup in one place. Saved/live data must bypass both the raw
    // biome query and generator-specific prediction, including RTG's forest filter.
    @WrapOperation(method = "doWork",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBiomeGenForCoordsBody(II)Lnet/minecraft/world/biome/BiomeGenBase;", remap = false),
            require = 1)
    private BiomeGenBase dragonfixes$resolveSample(World sampleWorld, int sampleX, int sampleZ,
            Operation<BiomeGenBase> original) {
        if (!dragonfixes$enabled()) return original.call(sampleWorld, sampleX, sampleZ);
        if (dragonfixes$forcedBiome != null) return dragonfixes$forcedBiome;
        if (sampleWorld.getChunkProvider().chunkExists(sampleX >> 4, sampleZ >> 4)) {
            return original.call(sampleWorld, sampleX, sampleZ);
        }

        int stored = StoredBiomeLookup.read(sampleWorld, sampleX, sampleZ);
        if (StoredBiomeLookup.isKnown(stored)) {
            BiomeGenBase actual = BiomeGenBase.getBiome(StoredBiomeLookup.id(stored));
            if (actual != null) {
                dragonfixes$sampleTentative = StoredBiomeLookup.isPending(stored);
                return actual;
            }
        }

        dragonfixes$sampleTentative = true;
        BiomeGenBase raw = original.call(sampleWorld, sampleX, sampleZ);
        Object worker = this;
        // Optional generator mixins implement separate interfaces. This avoids
        // relying on the ordering of multiple WrapOperations on the same call.
        if (worker instanceof CompassBiomePrediction.Rwg1) {
            raw = ((CompassBiomePrediction.Rwg1) worker).dragonfixes$predictRwg1(sampleWorld, sampleX, sampleZ, raw);
        }
        if (worker instanceof CompassBiomePrediction.Rtg) {
            raw = ((CompassBiomePrediction.Rtg) worker).dragonfixes$predictRtg(sampleWorld, sampleX, sampleZ, raw);
        }
        return raw;
    }

    @WrapOperation(method = "doWork",
            at = @At(value = "INVOKE", target = "Lcom/chaosthedude/naturescompass/util/BiomeSearchWorker;finish(Z)V"),
            require = 2)
    private void dragonfixes$deferMatch(BiomeSearchWorker worker, boolean found, Operation<Void> original) {
        if (found && dragonfixes$sampleTentative) {
            dragonfixes$tentative = true;
            dragonfixes$retries = 0;
            return;
        }
        original.call(worker, found);
    }
}
