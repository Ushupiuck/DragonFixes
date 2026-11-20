package cc.unilock.dragonfixes.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Log formatting must never turn a valid OreDictionary registration into a
 * Forge event-bus exception. Preserve the original ItemStack string when it
 * works, but use stable item identity if the block isn't fully initialized.
 */
public final class ReikaOreDictionaryLogBridge {
    private ReikaOreDictionaryLogBridge() {}

    public static StringBuilder appendSafely(StringBuilder builder, Object value) {
        if (!(value instanceof ItemStack)) {
            return builder.append(value);
        }
        ItemStack stack = (ItemStack) value;
        try {
            return builder.append(stack.toString());
        } catch (RuntimeException badDisplayName) {
            Item item = stack.getItem();
            builder.append(stack.stackSize).append("x<initializing ");
            if (item != null) {
                builder.append(item.getClass().getName())
                        .append("#").append(Item.getIdFromItem(item));
            } else {
                builder.append("null item");
            }
            return builder.append(">@").append(stack.getItemDamage());
        }
    }
}
