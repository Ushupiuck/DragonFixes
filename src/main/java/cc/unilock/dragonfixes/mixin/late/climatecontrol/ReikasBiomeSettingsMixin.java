package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import java.io.File;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.compat.ClimateControlChromaBiomes;
import climateControl.api.BiomeSettings;
import climateControl.api.ClimateControlRules;
import climateControl.biomeSettings.ReikasBiomeSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraftforge.common.config.Configuration;

@Mixin(value = ReikasBiomeSettings.class, remap = false)
public abstract class ReikasBiomeSettingsMixin {
    @Shadow @Final public BiomeSettings.Element enderForest;
    @Shadow @Final public BiomeSettings.Element rainbowForest;
    @Unique private BiomeSettings.Element dragonfixes$luminousCliffs;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void dragonfixes$addLuminousCliffs(CallbackInfo ci) {
        ReikasBiomeSettings settings = (ReikasBiomeSettings) (Object) this;
        // Element registers itself with Climate Control's ordinary config and incidence lists.
        // Match the existing ChromatiCraft defaults, while leaving the user free to change them.
        dragonfixes$luminousCliffs = settings.new Element("Luminous Cliffs", 49, 3, "MEDIUM");
    }

    @Inject(method = "setNativeBiomeIDs", at = @At("HEAD"), cancellable = true, require = 1)
    private void dragonfixes$resolveNativeBiomeIDs(File configDirectory, CallbackInfo ci) {
        // This method is also called during pre-init, before biome instances necessarily exist.
        // Read the actual current config keys as a fallback; never write the Reika config here.
        Configuration nativeConfig = new Configuration(new File(new File(configDirectory, "Reika"), "ChromatiCraft.cfg"));
        nativeConfig.load();
        enderForest.biomeID().set(nativeConfig.get("biome ids", "Ender Forest Biome ID", 47).getInt(47));
        rainbowForest.biomeID().set(nativeConfig.get("biome ids", "Rainbow Forest Biome ID", 48).getInt(48));
        dragonfixes$luminousCliffs.biomeID().set(nativeConfig.get("biome ids", "Luminous Cliffs Biome ID", 49).getInt(49));
        ClimateControlChromaBiomes.synchronize((ReikasBiomeSettings) (Object) this);
        ci.cancel();
    }

    @Inject(method = "setRules", at = @At("RETURN"), require = 1)
    private void dragonfixes$preserveCliffRiverRules(ClimateControlRules rules, CallbackInfo ci) {
        ClimateControlChromaBiomes.synchronize((ReikasBiomeSettings) (Object) this);
        if (ChromatiCraft.glowingcliffs != null) {
            rules.disallowRivers(ChromatiCraft.glowingcliffs.biomeID);
        }
        if (ChromatiCraft.glowingcliffsEdge != null) {
            rules.disallowRivers(ChromatiCraft.glowingcliffsEdge.biomeID);
        }
    }
}
