package cc.unilock.dragonfixes.mixin.late.chromaticraft.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import Reika.DragonAPI.IO.Shaders.ShaderProgram;
import Reika.DragonAPI.IO.Shaders.ShaderRegistry.ShaderTypes;
import cc.unilock.dragonfixes.DragonFixes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patch only ChromatiCraft's Unknown Artefact zone fragment shader as it is
 * loaded by DragonAPI. Preserve the source shader's edge detection, hue, and
 * animation; stop the additive glow from reaching painful peak brightness.
 *
 * This runs on both initial shader load and DragonAPI shader reloads.
 */
@Mixin(value = ShaderProgram.class, remap = false)
public abstract class ShaderProgramArtefactGlowMixin {
    @Shadow @Final public String identifier;

    @Inject(method = "getShaderData", at = @At("RETURN"), cancellable = true)
    private void dragonfixes$capArtefactZoneGlow(ShaderTypes type, CallbackInfoReturnable<InputStream> cir) {
        if (!"uazone".equals(identifier) || type != ShaderTypes.FRAGMENT) {
            return;
        }

        InputStream original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        byte[] sourceBytes;
        try (InputStream input = original; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            sourceBytes = output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read ChromatiCraft UAZONE shader", ex);
        }

        String source = new String(sourceBytes, StandardCharsets.UTF_8);
        String originalLine = "vec3 net = orig.rgb+add;";
        String uniformLine = "uniform float skyBright;";

        if (!source.contains(originalLine) || !source.contains(uniformLine)) {
            DragonFixes.LOGGER.warn("ChromatiCraft UAZONE shader layout changed; glow ceiling patch not applied");
            cir.setReturnValue(new ByteArrayInputStream(sourceBytes));
            return;
        }

        // Cap only the newly added light. Never darken pixels already brighter
        // than the chosen ceiling (e.g. snow in full daylight).
        source = source.replace(
                uniformLine,
                uniformLine + "\nuniform float dragonfixesGlowCeiling;");
        source = source.replace(
                originalLine,
                "vec3 net = max(orig.rgb, min(orig.rgb+add, vec3(dragonfixesGlowCeiling)));");

        cir.setReturnValue(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
    }
}
