package cc.unilock.dragonfixes.mixin.late.chromaticraft.client;

import Reika.ChromatiCraft.Registry.ChromaShaders;
import cc.unilock.dragonfixes.DragonFixesVisualConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Keeps Unknown Artefact zones recognizable without forcing the full-strength,
 * animated edge highlights over an entire scene.
 *
 * Scale the render-time intensity, not ChromaShaders' internal intensity:
 * modifying the latter would interfere with ChromatiCraft's fade logic.
 */
@Mixin(value = ChromaShaders.class, remap = false)
public abstract class ChromaShadersMixin {
    @ModifyArg(
            method = "onPreRender",
            at = @At(value = "INVOKE", target = "LReika/DragonAPI/IO/Shaders/ShaderProgram;setIntensity(F)V", remap = false),
            index = 0)
    private float dragonfixes$scaleArtefactZoneIntensity(float original) {
        return (Object) this == ChromaShaders.UAZONE
                ? original * dragonfixes$getArtefactZoneStrength()
                : original;
    }

    // DragonAPI checks isEnabled before onPreRender; updateEnabled must also honor "off".
    @ModifyArg(
            method = {"onPreRender", "updateEnabled"},
            at = @At(value = "INVOKE", target = "LReika/DragonAPI/IO/Shaders/ShaderProgram;setEnabled(Z)V", remap = false),
            index = 0)
    private boolean dragonfixes$enableArtefactZoneShader(boolean original) {
        return (Object) this != ChromaShaders.UAZONE
                ? original
                : original && dragonfixes$getArtefactZoneStrength() > 0.0F;
    }

    @Unique
    private static float dragonfixes$getArtefactZoneStrength() {
        return Math.max(0.0F, Math.min(1.0F, DragonFixesVisualConfig.artefactZoneShaderStrength));
    }
}
