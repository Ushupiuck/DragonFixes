package cc.unilock.dragonfixes.mixin.early.chromaticraft;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import Reika.ChromatiCraft.Items.Tools.ItemDataCrystal;

/**
 * NEI calls getDurabilityForDisplay on clean preview ItemStacks, while
 * ItemDataCrystal assumes a recipe/carve NBT compound is already present.
 * Preserve ChromatiCraft's original calculations for real tagged crystals.
 */
@Mixin(value = ItemDataCrystal.class, remap = false)
public abstract class MixinItemDataCrystalGuard {
    @Inject(
            method = "getDurabilityForDisplay(Lnet/minecraft/item/ItemStack;)D",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void dragonfixes$uninitializedCrystalHasNoDurability(
            ItemStack stack, CallbackInfoReturnable<Double> cir) {
        if (stack == null || stack.stackTagCompound == null) {
            cir.setReturnValue(0D);
        }
    }
}
