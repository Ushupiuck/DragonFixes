package cc.unilock.dragonfixes.mixin.late.rtg;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.biome.BiomeGenBase;
import rtg.util.RealisticBiomePresenceTester;

@Mixin(value = RealisticBiomePresenceTester.class, remap = false)
public class RealisticBiomePresenceTesterMixin {
    @ModifyConstant(method = "doBiomeCheck", constant = @Constant(intValue = 256), require = 1)
    private static int dragonfixes$checkFullRegistry(int original) {
        return BiomeGenBase.getBiomeGenArray().length;
    }
}
