package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.compat.Rwg1StreamPreview;
import com.chaosthedude.naturescompass.util.BiomeSearchWorker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rwg.world.ChunkManagerRealistic;

@Mixin(value = BiomeSearchWorker.class, remap = false)
public abstract class BiomeSearchWorkerMixin {
    @Shadow public BiomeGenBase biome;

    @WrapOperation(method = "doWork",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBiomeGenForCoordsBody(II)Lnet/minecraft/world/biome/BiomeGenBase;", remap = false),
            require = 1)
    private BiomeGenBase dragonfixes$searchRwg1Stream(World world, int x, int z, Operation<BiomeGenBase> original) {
        BiomeGenBase raw = original.call(world, x, z);
        if (ChromatiCraft.rainbowRiver == null || biome != ChromatiCraft.rainbowRiver
                || raw != ChromatiCraft.rainbowforest || world.isRemote
                || world.getChunkProvider().chunkExists(x >> 4, z >> 4)
                || !(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) return raw;
        if (!(world.getWorldChunkManager() instanceof Rwg1StreamPreview.Access)) return raw;
        Rwg1StreamPreview preview = ((Rwg1StreamPreview.Access) world.getWorldChunkManager())
                .dragonfixes$getStreamPreview();
        if (preview == null) return raw;
        BiomeGenBase predicted = preview.dragonfixes$previewStream(x, z);
        return predicted == ChromatiCraft.rainbowRiver ? predicted : raw;
    }
}
