package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import java.util.ArrayList;

import cc.unilock.dragonfixes.compat.ClimateControlChromaBiomes;
import climateControl.api.BiomeSettings;
import climateControl.api.ClimateDistribution;
import climateControl.api.ClimateControlRules;
import climateControl.generator.SubBiomeChooser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeSettings.class, remap = false)
public abstract class BiomeSettingsMixin {
    // A saved dimension's config may overwrite the IDs after setNativeBiomeIDs.
    // Refresh only the Reika package immediately before Climate Control consumes them.
    @Inject(method = "incidences", at = @At("HEAD"), require = 1)
    private void dragonfixes$syncIncidences(CallbackInfoReturnable<ArrayList<ClimateDistribution.Incidence>> cir) {
        ClimateControlChromaBiomes.synchronize((BiomeSettings) (Object) this);
    }

    @Inject(method = "update", at = @At("HEAD"), require = 1)
    private void dragonfixes$syncSubBiomes(SubBiomeChooser chooser, CallbackInfo ci) {
        ClimateControlChromaBiomes.synchronize((BiomeSettings) (Object) this);
    }

    @Inject(method = "setVillages", at = @At("HEAD"), require = 1)
    private void dragonfixes$syncVillages(ClimateControlRules rules, CallbackInfo ci) {
        ClimateControlChromaBiomes.synchronize((BiomeSettings) (Object) this);
    }
}
