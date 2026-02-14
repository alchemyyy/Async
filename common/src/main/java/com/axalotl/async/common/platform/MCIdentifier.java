package com.axalotl.async.common.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
#if MC_VER_1_21_11
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
#else
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
#endif

public final class MCIdentifier {

    public static Object getId(
        CommandContext<CommandSourceStack> ctx,
        String name
    ) {
#if MC_VER_1_21_11
        return IdentifierArgument.getId(ctx, name);
#else
        return ResourceLocationArgument.getId(ctx, name);
#endif
    }

    public static ArgumentType<?> argument() {
#if MC_VER_1_21_11
        return IdentifierArgument.id();
#else
        return ResourceLocationArgument.id();
#endif
    }

    public static String getNamespace(Object id) {
#if MC_VER_1_21_11
        return ((Identifier) id).getNamespace();
#else
        return ((ResourceLocation) id).getNamespace();
#endif
    }

    public static Object tryParse(String value) {
#if MC_VER_1_21_11
        return Identifier.tryParse(value);
#else
        return ResourceLocation.tryParse(value);
#endif
    }

    public static boolean registryContainsKey(Registry<?> registry, Object id) {
#if MC_VER_1_21_11
        return registry.containsKey((Identifier) id);
#else
        return registry.containsKey((ResourceLocation) id);
#endif
    }

    public static boolean isEntitySynchronized(
        Object entityId,
        java.util.Map<Object, Boolean> syncCache,
        java.util.Set<String> exactEntities,
        java.util.Set<String> namespaceWildcards
    ) {
        Boolean cached = syncCache.get(entityId);
        if (cached != null) return cached;

        String idString = entityId.toString();
        if (exactEntities.contains(idString)) {
            syncCache.put(entityId, true);
            return true;
        }

#if MC_VER_1_21_11
        if (
            namespaceWildcards.contains(((Identifier) entityId).getNamespace())
        ) {
#else
        if (
            namespaceWildcards.contains(
                ((ResourceLocation) entityId).getNamespace()
            )
        ) {
#endif
            syncCache.put(entityId, true);
            return true;
        }

        syncCache.put(entityId, false);
        return false;
    }

    public static boolean existsNamespace(String namespace, Iterable<?> keys) {
        for (Object id : keys) {
#if MC_VER_1_21_11
            if (((Identifier) id).getNamespace().equals(namespace)) return true;
#else
            if (
                ((ResourceLocation) id).getNamespace().equals(namespace)
            ) return true;
#endif
        }
        return false;
    }
}
