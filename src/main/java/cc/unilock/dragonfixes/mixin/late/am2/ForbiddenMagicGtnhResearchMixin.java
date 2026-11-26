package cc.unilock.dragonfixes.mixin.late.am2;

import cc.unilock.dragonfixes.DragonFixes;
import com.gtnewhorizon.gtnhlib.api.thaumcraft.FormattedResearchPage;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

@Pseudo
@Mixin(targets = "com.dreammaster.scripts.ScriptForbiddenMagic", remap = false)
public abstract class ForbiddenMagicGtnhResearchMixin {
    @Unique private Object[] dragonfixes$rodTextArgs;
    @Unique private Object[] dragonfixes$staffTextArgs;
    @Unique private Object[] dragonfixes$capTextArgs;
    @Unique private boolean dragonfixes$hybridRecipe;
    // Forbidden Magic's AM2 integration registers ROD_witchwood at (-3, 1).
    // GTNH needs that slot for JOURNEY, a prerequisite of VINTEUM.
    // Remove only the original AM2 registrations when GTNH actually runs its
    // replacement script; do not disable the integration or touch the GUI.
    @Inject(method = "loadRecipes()V", at = @At("HEAD"), require = 1)
    private void dragonfixes$replaceOriginalAm2Research(CallbackInfo ci) {
        dragonfixes$hybridRecipe = false;
        dragonfixes$rodTextArgs = dragonfixes$removeOriginal("ROD_witchwood", -3, 1);
        dragonfixes$capTextArgs = dragonfixes$removeOriginal("CAP_vinteum", -4, -1);
        dragonfixes$staffTextArgs = dragonfixes$removeOriginal("ROD_witchwood_staff", -3, -1);
    }

    // Preserve GTNH's cost, central Ent Twig, Vinteum, Witchery distillates
    // and opposed pedestal pairs. Only replace the three sapling pairs.
    @ModifyArgs(method = "loadRecipes()V", at = @At(value = "INVOKE",
            target = "Lcom/dreammaster/thaumcraft/TCHelper;addInfusionCraftingRecipe(Ljava/lang/String;"
                    + "Lnet/minecraft/item/ItemStack;ILthaumcraft/api/aspects/AspectList;"
                    + "Ljava/lang/Object;[Ljava/lang/Object;)Lthaumcraft/api/crafting/InfusionRecipe;"),
            require = 1)
    private void dragonfixes$useRealWitchwood(Args args) {
        if (!"ROD_witchwood".equals(args.get(0)) || dragonfixes$rodTextArgs == null) {
            return;
        }
        Object[] ingredients = args.get(5);
        Block wood = GameRegistry.findBlock("arsmagica2", "WitchwoodLog");
        Item essence = GameRegistry.findItem("arsmagica2", "essence");
        Item sapling = GameRegistry.findItem("witchery", "witchsapling");
        if (wood == null || Item.getItemFromBlock(wood) == null || essence == null
                || sapling == null || ingredients == null || ingredients.length != 12) {
            DragonFixes.LOGGER.warn("Unable to resolve AM2 Witchwood infusion ingredients; retaining GTNH recipe");
            return;
        }
        for (int i = 0; i < 3; i++) {
            int slot = 1 + 2 * i;
            if (!dragonfixes$isSapling(ingredients[slot], sapling, i)
                    || !dragonfixes$isSapling(ingredients[slot + 6], sapling, i)) {
                DragonFixes.LOGGER.warn("GTNH Witchwood infusion layout changed; retaining its recipe");
                return;
            }
        }
        Object[] hybrid = ingredients.clone();
        for (int offset = 0; offset < 12; offset += 6) {
            hybrid[offset + 1] = new ItemStack(wood);
            hybrid[offset + 3] = new ItemStack(essence, 1, 0); // Arcane Essence
            hybrid[offset + 5] = new ItemStack(essence, 1, 5); // Nature Essence
        }
        args.set(5, hybrid);
        dragonfixes$hybridRecipe = true;
        DragonFixes.LOGGER.info("Replaced GTNH Witchwood infusion saplings with AM2 logs and essences");
    }

    @Unique
    private static boolean dragonfixes$isSapling(Object ingredient, Item sapling, int meta) {
        return ingredient instanceof ItemStack && ((ItemStack) ingredient).getItem() == sapling
                && ((ItemStack) ingredient).getItemDamage() == meta;
    }

    @Inject(method = "loadRecipes()V", at = @At("RETURN"), require = 1)
    private void dragonfixes$mergeAm2ResearchDescriptions(CallbackInfo ci) {
        if (dragonfixes$hybridRecipe) {
            dragonfixes$replaceText("ROD_witchwood", "dragonfixes.research_page.witchwood_rod",
                    dragonfixes$rodTextArgs);
        }
        if (dragonfixes$staffTextArgs != null) {
            dragonfixes$replaceText("ROD_witchwood_staff", "dragonfixes.research_page.witchwood_staff",
                    dragonfixes$staffTextArgs);
        }
        if (dragonfixes$capTextArgs != null) {
            dragonfixes$replaceText("CAP_vinteum", "dragonfixes.research_page.vinteum_cap", new Object[0]);
        }
    }

    @Unique
    private static void dragonfixes$replaceText(String key, String textKey, Object[] formatArgs) {
        ResearchItem research = ResearchCategories.getResearch(key);
        if (research == null || !"FORBIDDEN".equals(research.category)) {
            return;
        }
        ResearchPage[] pages = research.getPages();
        if (pages != null && pages.length > 0 && pages[0] != null && pages[0].recipe == null) {
            pages[0] = new FormattedResearchPage(textKey, formatArgs);
        }
    }

    @Unique
    private static Object[] dragonfixes$removeOriginal(String key, int x, int y) {
        ResearchItem research = ResearchCategories.getResearch(key);
        if (research == null || !"FORBIDDEN".equals(research.category)
                || research.displayColumn != x || research.displayRow != y
                || !"fox.spiteful.forbidden.DarkResearchItem".equals(research.getClass().getName())) {
            return null;
        }
        ResearchCategoryList category = ResearchCategories.getResearchList(research.category);
        if (category == null || category.research.get(key) != research) {
            return null;
        }

        // The old recipe and its research page share the same recipe object.
        // Remove by identity so we cannot accidentally remove another mod's
        // recipe merely because it produces the same wand component.
        Object[] formattingData = null;
        ResearchPage[] pages = research.getPages();
        if (pages != null) {
            for (ResearchPage page : pages) {
                if (page instanceof FormattedResearchPage && formattingData == null) {
                    formattingData = ((FormattedResearchPage) page).formattingData.clone();
                }
                if (page != null && page.recipe != null) {
                    final Object originalRecipe = page.recipe;
                    ThaumcraftApi.getCraftingRecipes().removeIf(recipe -> recipe == originalRecipe);
                }
            }
        }
        category.research.remove(key);
        DragonFixes.LOGGER.info("Removed original Forbidden Magic AM2 research {} for GTNH replacement", key);
        return formattingData;
    }
}
