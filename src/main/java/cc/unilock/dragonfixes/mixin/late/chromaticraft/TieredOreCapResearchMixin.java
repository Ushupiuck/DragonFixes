package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Link each elemental wand-cap recipe to its existing shared research. */
@Mixin(targets = "Reika.ChromatiCraft.ModInterface.ThaumCraft.TieredOreCap", remap = false)
public abstract class TieredOreCapResearchMixin {
    @ModifyArg(method = "addRecipes",
            at = @At(value = "INVOKE",
                    target = "Lthaumcraft/api/ThaumcraftApi;addArcaneCraftingRecipe(Ljava/lang/String;Lnet/minecraft/item/ItemStack;Lthaumcraft/api/aspects/AspectList;[Ljava/lang/Object;)Lthaumcraft/api/crafting/ShapedArcaneRecipe;"),
            index = 0, require = 1, remap = false)
    private static String dragonfixes$elementalCapsResearch(String research) {
        return "".equals(research) ? "tieredorecap" : research;
    }
}
