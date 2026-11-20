package cc.unilock.dragonfixes.compat;

import java.lang.reflect.Field;

/** Accessor for private Forestry registry entries in its GTNH implementation. */
public final class ForestryReflectionBridge {
    private ForestryReflectionBridge() {}

    public static Field getAccessibleDeclaredField(Class<?> owner, String name)
            throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
