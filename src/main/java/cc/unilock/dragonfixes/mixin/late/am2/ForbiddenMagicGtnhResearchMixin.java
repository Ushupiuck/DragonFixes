package cc.unilock.dragonfixes.mixin.late.am2;

import cc.unilock.dragonfixes.DragonFixes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

@Pseudo
@Mixin(targets = "com.dreammaster.scripts.ScriptForbiddenMagic", remap = false)
public abstract class ForbiddenMagicGtnhResearchMixin {
    // Forbidden Magic's AM2 integration registers ROD_witchwood at (-3, 1).
    // GTNH needs that slot for JOURNEY, a prerequisite of VINTEUM.
    // Remove only the original AM2 registrations when GTNH actually runs its
    // replacement script; do not disable the integration or touch the GUI.
    @Inject(method = "loadRecipes()V", at = @At("HEAD"), require = 1)
    private void dragonfixes$replaceOriginalAm2Research(CallbackInfo ci) {
        dragonfixes$removeOriginal("ROD_witchwood", -3, 1);
        dragonfixes$removeOriginal("CAP_vinteum", -4, -1);
        dragonfixes$removeOriginal("ROD_witchwood_staff", -3, -1);
    }

    @Unique
    private static void dragonfixes$removeOriginal(String key, int x, int y) {
        ResearchItem research = ResearchCategories.getResearch(key);
        if (research == null || !"FORBIDDEN".equals(research.category)
                || research.displayColumn != x || research.displayRow != y
                || !"fox.spiteful.forbidden.DarkResearchItem".equals(research.getClass().getName())) {
            return;
        }
        ResearchCategoryList category = ResearchCategories.getResearchList(research.category);
        if (category == null || category.research.get(key) != research) {
            return;
        }

        // The old recipe and its research page share the same recipe object.
        // Remove by identity so we cannot accidentally remove another mod's
        // recipe merely because it produces the same wand component.
        ResearchPage[] pages = research.getPages();
        if (pages != null) {
            for (ResearchPage page : pages) {
                if (page != null && page.recipe != null) {
                    final Object originalRecipe = page.recipe;
                    ThaumcraftApi.getCraftingRecipes().removeIf(recipe -> recipe == originalRecipe);
                }
            }
        }
        category.research.remove(key);
        DragonFixes.LOGGER.info("Removed original Forbidden Magic AM2 research {} for GTNH replacement", key);
    }
}
