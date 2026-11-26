package cc.unilock.dragonfixes.mixin.late.rtg;

import cc.unilock.dragonfixes.compat.RtgLandscapeAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rtg.world.gen.ChunkProviderRTG;
import rtg.world.gen.LandscapeGenerator;

@Mixin(ChunkProviderRTG.class)
public abstract class ChunkProviderRTGMixin implements RtgLandscapeAccess {
    @Shadow(remap = false) @Final private World worldObj;
    @Shadow(remap = false) @Final private LandscapeGenerator landscapeGenerator;

    @Override
    public World dragonfixes$getWorld() {
        return worldObj;
    }

    @Override
    public LandscapeGenerator dragonfixes$getLandscape() {
        return landscapeGenerator;
    }

}
