package com.axalotl.async.neoforge.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Map;

/**
 * NeoForge only: Fixes ConcurrentModificationException in Create mod
 */
@Mixin(targets = "com.simibubi.create.content.contraptions.elevator.ElevatorColumn", remap = false)
public class ElevatorColumnMixin {

    /**
     * Wrap the Map returned by LOADED_COLUMNS.get() with a synchronized wrapper
     */
    @Redirect(
            method = "getOrCreate(Lnet/minecraft/world/level/LevelAccessor;Lcom/simibubi/create/content/contraptions/elevator/ElevatorColumn$ColumnCoords;)Lcom/simibubi/create/content/contraptions/elevator/ElevatorColumn;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/catnip/data/WorldAttached;get(Ljava/lang/Object;)Ljava/lang/Object;",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private static Object wrapWithSynchronizedMap(Object worldAttached, Object level) {
        try {
            // Вызываем оригинальный метод get()
            Object originalMap = worldAttached.getClass()
                    .getMethod("get", Object.class)
                    .invoke(worldAttached, level);

            // Оборачиваем в synchronizedMap
            return Collections.synchronizedMap((Map<?, ?>) originalMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to synchronize ElevatorColumn map", e);
        }
    }
}