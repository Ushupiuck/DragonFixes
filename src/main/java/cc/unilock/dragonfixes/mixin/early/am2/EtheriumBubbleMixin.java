package cc.unilock.dragonfixes.mixin.early.am2;

import cc.unilock.dragonfixes.compat.am2.EtheriumCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityBubbleFX.class)
public abstract class EtheriumBubbleMixin {
    // Vanilla kills bubbles outside water. Permit Etherium bubbles to live
    // until they exit their fluid too, only for the particle's own check.
    @WrapOperation(method = "onUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"),
            require = 1)
    private Material dragonfixes$keepEtheriumBubbles(Block block, Operation<Material> original) {
        Material actual = original.call(block);
        if (actual != Material.water && EtheriumCompat.enabled()
                && (EtheriumCompat.isEtherium(actual) || EtheriumCompat.touches((Entity) (Object) this))) {
            return Material.water;
        }
        return actual;
    }
}
