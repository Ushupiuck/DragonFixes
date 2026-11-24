package cc.unilock.dragonfixes.mixin.late.chromaticraft.client;

import Reika.ChromatiCraft.Block.Dimension.BlockLightedLeaf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BlockLightedLeaf.class, remap = false)
public class BlockLightedLeafMixin {
    @ModifyVariable(method = "getOverlay", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private int dragonfixes$normalizeOverlayMetadata(int metadata) {
        // Existing worlds may already contain 0xFFFF from wildcard leaf metadata.
        // There are only sixteen overlay textures, regardless of storage width.
        return metadata & 15;
    }
}
