package cc.unilock.dragonfixes.compat;

import net.minecraft.world.biome.BiomeGenBase;

/** Per-world access to the actual RWG1 provider, without constructing a second generator. */
public interface Rwg1StreamPreview {
    BiomeGenBase dragonfixes$previewStream(int x, int z);

    interface Access {
        Rwg1StreamPreview dragonfixes$getStreamPreview();
        void dragonfixes$setStreamPreview(Rwg1StreamPreview preview);
    }
}
