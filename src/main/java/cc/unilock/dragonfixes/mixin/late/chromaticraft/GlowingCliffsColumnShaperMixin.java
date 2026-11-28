package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.World.BiomeGlowingCliffs;
import Reika.ChromatiCraft.World.GlowingCliffsColumnShaper;
import cc.unilock.dragonfixes.compat.CliffBiomeNeighborhood;
import cc.unilock.dragonfixes.compat.CliffBiomeTiles;
import cc.unilock.dragonfixes.compat.CliffTerrainQueryScope;
import cc.unilock.dragonfixes.compat.CliffHeightMemo;
import cc.unilock.dragonfixes.compat.CliffRadialAngles;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GlowingCliffsColumnShaper.class, remap = false)
public abstract class GlowingCliffsColumnShaperMixin {
    @Shadow private BiomeGenBase getBiome(World world, int x, int z) { throw new AssertionError(); }

    @Inject(method = "getDistanceFactor", at = @At("HEAD"), require = 1)
    private void dragonfixes$distanceScope(World world, int x, int z, int search, BiomeGenBase biome,
            CallbackInfoReturnable<?> cir, @Share("cliffScope") LocalRef<CliffTerrainQueryScope> scope) {
        scope.set(CliffTerrainQueryScope.current(world));
    }

    @Inject(method = "getInvertedDistanceFactor", at = @At("HEAD"), require = 1)
    private void dragonfixes$invertedScope(World world, int x, int z, int search,
            CallbackInfoReturnable<?> cir, @Share("cliffScope") LocalRef<CliffTerrainQueryScope> scope) {
        scope.set(CliffTerrainQueryScope.current(world));
    }

    @WrapOperation(method = {"getDistanceFactor", "getInvertedDistanceFactor"},
            at = @At(value = "INVOKE", target = "LReika/ChromatiCraft/World/GlowingCliffsColumnShaper;getBiome(Lnet/minecraft/world/World;II)Lnet/minecraft/world/biome/BiomeGenBase;"), require = 2)
    private BiomeGenBase dragonfixes$memoizeRadialBiome(GlowingCliffsColumnShaper self, World world, int x, int z,
            Operation<BiomeGenBase> original, @Share("cliffScope") LocalRef<CliffTerrainQueryScope> scopeRef) {
        CliffTerrainQueryScope scope = scopeRef.get();
        if (scope == null) return original.call(self, world, x, z);
        CliffBiomeNeighborhood<BiomeGenBase> cache = scope.neighborhood;
        int i = cache.index(x, z);
        return cache.isKnown(i) ? cache.get(i) : cache.remember(i, original.call(self, world, x, z));
    }

    @WrapMethod(method = "calcHval")
    private double dragonfixes$memoizeHeight(World world, int x, int z, BiomeGenBase biome, Operation<Double> original) {
        CliffTerrainQueryScope scope = CliffTerrainQueryScope.current(world);
        if (scope == null || !scope.stableManager) return original.call(world, x, z, biome);
        CliffHeightMemo cache = scope.heights;
        int i = cache.index(x, z);
        return cache.contains(this, i, biome) ? cache.get(i)
                : cache.remember(this, i, biome, original.call(world, x, z, biome));
    }

    @WrapMethod(method = "getRegion")
    private GlowingCliffsColumnShaper.GlowCliffRegion dragonfixes$scopeRegion(World world, int x, int z,
            BiomeGenBase biome, Operation<GlowingCliffsColumnShaper.GlowCliffRegion> original) {
        if (CliffTerrainQueryScope.current(world) != null) return original.call(world, x, z, biome);
        Class<?> manager = world.getWorldChunkManager().getClass();
        if (manager != net.minecraft.world.biome.WorldChunkManager.class
                && !manager.getName().equals("rtg.world.biome.WorldChunkManagerRTG")) {
            return original.call(world, x, z, biome);
        }
        CliffTerrainQueryScope previous = CliffTerrainQueryScope.enter(world, x >> 4, z >> 4);
        try { return original.call(world, x, z, biome); }
        finally { CliffTerrainQueryScope.exit(previous); }
    }

    @Redirect(method = {"getDistanceFactor", "getInvertedDistanceFactor"},
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;cos(D)D"), require = 2)
    private double dragonfixes$cachedCos(double angle) { return CliffRadialAngles.cos(angle); }

    @Redirect(method = {"getDistanceFactor", "getInvertedDistanceFactor"},
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;sin(D)D"), require = 2)
    private double dragonfixes$cachedSin(double angle) { return CliffRadialAngles.sin(angle); }

    @WrapMethod(method = "getInvertedDistanceFactor")
    private Object[] dragonfixes$skipCliffFreeNeighborhood(World world, int x, int z, int search,
            Operation<Object[]> original) {
        CliffTerrainQueryScope scope = CliffTerrainQueryScope.current(world);
        if (scope != null && search > 0 && search <= CliffBiomeNeighborhood.MAX_SEARCH
                && scope.neighborhood.isChunkColumn(x, z)) {
            boolean mayContain = scope.neighborhood.mayContainReachableCliff((qx, qz) -> getBiome(world, qx, qz),
                    (tx, tz) -> CliffBiomeTiles.getTile(world.getWorldChunkManager(), tx, tz), BiomeGlowingCliffs::isGlowingCliffs);
            if (!mayContain) return null;
        }
        // Do not change the original angular search, tie breaking, heights,
        // interpolation, biome classification or random-number consumption.
        return original.call(world, x, z, search);
    }
}
