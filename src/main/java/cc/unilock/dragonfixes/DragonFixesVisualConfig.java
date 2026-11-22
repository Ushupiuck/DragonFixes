package cc.unilock.dragonfixes;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Visual settings live in their own category because other DragonFixes
 * options in "general" really do require restarting Minecraft.
 */
@Config(modid = "dragonfixes", category = "visual")
public class DragonFixesVisualConfig {
    @Config.Comment("ChromatiCraft Unknown Artefact zone outline shader strength: 0 = off, 0.25 = subtle, 1 = original. Client-side visual effect only. Takes effect immediately.")
    @Config.DefaultFloat(0.25F)
    @Config.RangeFloat(min = 0.0F, max = 1.0F)
    public static float artefactZoneShaderStrength;
}
