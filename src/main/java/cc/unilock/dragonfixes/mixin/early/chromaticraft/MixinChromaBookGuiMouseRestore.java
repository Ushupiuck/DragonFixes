package cc.unilock.dragonfixes.mixin.early.chromaticraft;

import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.launchwrapper.Launch;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import Reika.ChromatiCraft.Base.ChromaBookGui;
import Reika.ChromatiCraft.Registry.ChromaGuis;
import Reika.ChromatiCraft.Registry.ChromaResearch;

/**
 * ChromaBookGui enables cacheMouse for every newly constructed screen, even
 * when opened from the held Chromic Lexicon. The static saved mouse
 * coordinates start at (0, 0), and later retain the last book page's value.
 * initGui then unconditionally warps the physical cursor there.
 *
 * Restore the cursor only for transitions initiated by ChromaBookGui.goTo.
 * This keeps Reika's intended cross-page mouse preservation while leaving
 * the cursor alone when opening the Lexicon from gameplay or NEI.
 */
@Mixin(value = ChromaBookGui.class, remap = false)
public abstract class MixinChromaBookGuiMouseRestore {
    @Shadow(remap = false)
    private boolean cacheMouse;

    @Unique
    private static boolean dragonfixes$internalBookNavigation;

    @Inject(
            method = "goTo(LReika/ChromatiCraft/Registry/ChromaGuis;"
                    + "LReika/ChromatiCraft/Registry/ChromaResearch;"
                    + "Lnet/minecraft/entity/player/EntityPlayer;)V",
            at = @At("HEAD"),
            require = 1,
            remap = false)
    private static void dragonfixes$recordBookNavigation(
            ChromaGuis next, ChromaResearch page, EntityPlayer player,
            CallbackInfo ci) {
        // BOOKPAGES and BOOKEMPTIES are container GUIs, not ChromaBookGui.
        dragonfixes$internalBookNavigation = next != null && next.isLexiconGUI()
                && next != ChromaGuis.BOOKPAGES && next != ChromaGuis.BOOKEMPTIES;
    }

    /**
     * LWJGL3ify's Mouse.getY() is measured from the bottom of the framebuffer,
     * but Mouse.setCursorPosition delegates directly to SDL_WarpMouseInWindow,
     * whose Y is measured from the top. Reika saves getY() during goTo() and
     * restores it during initGui(), accidentally vertically mirroring the
     * cursor on every book-page transition.
     *
     * Convert the coordinate only when LWJGL3ify is present. Native LWJGL2
     * already uses bottom-origin Y for both methods.
     */
    @Redirect(
            method = "initGui()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/input/Mouse;setCursorPosition(II)V",
                    remap = false),
            require = 1,
            remap = false)
    private void dragonfixes$restoreCorrectMouseY(int x, int y) {
        if (Launch.classLoader.getResource("org/lwjglx/input/Mouse.class") != null) {
            Mouse.setCursorPosition(x, Display.getHeight() - y);
        } else {
            Mouse.setCursorPosition(x, y);
        }
    }

    @Inject(
            method = "<init>(LReika/ChromatiCraft/Registry/ChromaGuis;"
                    + "Lnet/minecraft/entity/player/EntityPlayer;II)V",
            at = @At("RETURN"),
            require = 1,
            remap = false)
    private void dragonfixes$restoreOnlyDuringBookNavigation(
            ChromaGuis type, EntityPlayer player, int x, int y, CallbackInfo ci) {
        cacheMouse = dragonfixes$internalBookNavigation;
        dragonfixes$internalBookNavigation = false;
    }
}
