package cc.unilock.dragonfixes.mixin.late.rtg;

import cc.unilock.dragonfixes.compat.RtgMapGenScope;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtg.world.gen.ChunkLandscape;
import rtg.world.gen.ChunkProviderRTG;

@Mixin(ChunkProviderRTG.class)
public abstract class EndlessIdsMapGenProviderMixin {
    @WrapMethod(method = "provideChunk")
    private Chunk dragonfixes$scopeMapGen(int x, int z, Operation<Chunk> original) {
        RtgMapGenScope previous = RtgMapGenScope.enter((ChunkProviderRTG)(Object)this, x, z);
        try {
            return original.call(x, z);
        } finally {
            RtgMapGenScope.exit(previous);
        }
    }

    @ModifyExpressionValue(method = "provideChunk", at = @At(value = "INVOKE",
            target = "Lrtg/world/gen/LandscapeGenerator;landscape(Lrtg/world/biome/RTGBiomeProvider;II)Lrtg/world/gen/ChunkLandscape;",
            remap = false), require = 1)
    private ChunkLandscape dragonfixes$captureMapGenLandscape(ChunkLandscape landscape) {
        RtgMapGenScope scope = RtgMapGenScope.current();
        if (scope != null && (Object)scope.provider == this) scope.landscape = landscape;
        return landscape;
    }
}
