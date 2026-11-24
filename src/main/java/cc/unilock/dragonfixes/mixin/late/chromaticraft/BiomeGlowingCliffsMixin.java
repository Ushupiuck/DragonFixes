package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Registry.ChromaBlocks;
import Reika.ChromatiCraft.World.BiomeGlowingCliffs;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockKey;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BiomeGlowingCliffs.class, remap = false)
public class BiomeGlowingCliffsMixin {
    @ModifyReturnValue(method = "selectGlowingTreeLeaf", at = @At("RETURN"), require = 1)
    private static BlockKey dragonfixes$normalizeGlowLeafMetadata(BlockKey leaf) {
        if (leaf != null && leaf.blockID == ChromaBlocks.GLOWLEAF.getBlockInstance()
                && (leaf.metadata < 0 || leaf.metadata > 15)) {
            // Wildcard -1 was truncated to 15 by vanilla's four-bit metadata storage.
            // Do not let extended metadata storage persist that wildcard as 65535.
            return new BlockKey(leaf.blockID, leaf.metadata & 15);
        }
        return leaf;
    }
}
