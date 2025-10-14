package com.axalotl.async.common.mixin.servercore;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collections;
import java.util.Set;

/**
 * Fixes ConcurrentModificationException in ServerCore's broadcast cache
 */
@Mixin(targets = "net.minecraft.server.level.ServerChunkCache")
public class ServerChunkCacheBroadcastMixin {

    /**
     * Wrap newly created ReferenceLinkedOpenHashSet with synchronizedSet
     */
    @ModifyVariable(
            method = "<init>",
            at = @At("STORE"),
            ordinal = 0,
            require = 0
    )
    private Set<?> wrapBlockChangesSet(Set<?> original) {
        if (original instanceof ReferenceLinkedOpenHashSet) {
            return Collections.synchronizedSet(original);
        }
        return original;
    }
}