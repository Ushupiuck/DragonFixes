package cc.unilock.dragonfixes.mixin.early.dragonapi;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import Reika.DragonAPI.Instantiable.Event.PlayerPlaceBlockEvent;

/**
 * GuideNH/StructureLib previews place blocks in an ObserverWorld with a null
 * EntityPlayer. Reika's PlayerPlaceBlockEvent is nevertheless dispatched, and
 * ChromatiCraft's handleRubberLogPlacement dereferences evt.player.
 *
 * A synthetic placement has no player to trigger player-specific enchantment,
 * inventory or ability hooks. Skip only the player event. Returning false
 * preserves fireTryPlace's normal world.setBlock continuation; all real
 * player placements still dispatch the original cancellable event.
 */
@Mixin(value = PlayerPlaceBlockEvent.class, remap = false)
public abstract class MixinPlayerPlaceBlockEventNullPlayer {

    @Inject(
            method = "fire(Lnet/minecraft/world/World;IIIILnet/minecraft/block/Block;"
                    + "ILnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private static void dragonfixes$skipPlayerEventForVirtualPlacement(
            World world, int x, int y, int z, int side, Block block, int metadata,
            ItemStack stack, EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) {
            cir.setReturnValue(false);
        }
    }
}
