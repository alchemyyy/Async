package com.axalotl.async.common.mixin.servercore;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

/**
 * Синхронизирует ServerCore broadcastCache
 */
@Mixin(value = ServerChunkCache.class, priority = 2000)
public class ServerChunkCacheBroadcastMixin {

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void wrapBroadcastCache(CallbackInfo ci) {
        try {
            Field field = asyncMultiloader$findField(this.getClass());
            if (field != null) {
                field.setAccessible(true);
                Set<?> original = (Set<?>) field.get(this);
                if (original != null) {
                    field.set(this, Collections.synchronizedSet(original));
                }
            }
        } catch (Exception ignored) {}
    }

    @Unique
    private static Field asyncMultiloader$findField(Class<?> clazz) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("servercore$broadcastCache");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}