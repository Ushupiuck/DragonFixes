package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.compat.CompassBiomePrediction;
import cc.unilock.dragonfixes.compat.Rwg1StreamPreview;
import com.chaosthedude.naturescompass.util.BiomeSearchWorker;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rwg.world.ChunkManagerRealistic;

@Mixin(value = BiomeSearchWorker.class, remap = false)
public abstract class BiomeSearchWorkerMixin implements CompassBiomePrediction.Rwg1 {
    @Shadow public BiomeGenBase biome;

    @Override
    public BiomeGenBase dragonfixes$predictRwg1(World world, int x, int z, BiomeGenBase raw) {
        // The raw biome can disagree with RWG1's blended parent at biome edges.
        // The preview checks the river predicate before doing the blended lookup.
        if (ChromatiCraft.rainbowRiver == null || biome != ChromatiCraft.rainbowRiver || world.isRemote
                || !(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) return raw;
        if (!(world.getWorldChunkManager() instanceof Rwg1StreamPreview.Access)) return raw;
        Rwg1StreamPreview preview = ((Rwg1StreamPreview.Access) world.getWorldChunkManager())
                .dragonfixes$getStreamPreview();
        if (preview == null) return raw;
        BiomeGenBase predicted = preview.dragonfixes$previewStream(x, z);
        return predicted == ChromatiCraft.rainbowRiver ? predicted : raw;
    }
}
