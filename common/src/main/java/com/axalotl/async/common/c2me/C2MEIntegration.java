// common/src/main/java/com/axalotl/async/common/c2me/C2MEIntegration.java
package com.axalotl.async.common.c2me;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Прямая интеграция с C2ME для проверки статуса чанков
 */
public class C2MEIntegration {
    private static final boolean C2ME_AVAILABLE;
    private static Method GET_CHUNK_STATUS_METHOD;

    static {
        boolean available = false;
        try {
            Class.forName("com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus");
            available = true;

            // Пытаемся получить метод для проверки статуса
            Class<?> holderClass = ChunkHolder.class;
            GET_CHUNK_STATUS_METHOD = holderClass.getMethod("getChunkStatus");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // C2ME не установлен или версия несовместима
        }
        C2ME_AVAILABLE = available;
    }

    public static boolean isAvailable() {
        return C2ME_AVAILABLE;
    }

    /**
     * Быстрая неблокирующая проверка готовности чанка для entity ticking
     * Использует C2ME статусы если доступно, иначе vanilla логику
     */
    public static boolean isChunkReadyForEntityTicking(ServerLevel world, Entity entity) {
        ChunkPos chunkPos = entity.chunkPosition();
        ChunkHolder holder = world.getChunkSource().getVisibleChunkIfPresent(chunkPos.toLong());

        if (holder == null) {
            return false; // Чанк вообще не загружен
        }

        if (C2ME_AVAILABLE) {
            return isChunkReadyC2ME(holder);
        } else {
            return isChunkReadyVanilla(holder);
        }
    }

    /**
     * Проверка через C2ME API - ОЧЕНЬ быстро, без блокировки
     */
    private static boolean isChunkReadyC2ME(ChunkHolder holder) {
        try {
            // В C2ME есть прямой доступ к статусу без создания Future
            Object status = GET_CHUNK_STATUS_METHOD.invoke(holder);

            // Проверяем что статус >= ENTITY_TICKING
            // В C2ME: ENTITY_TICKING имеет ordinal больше чем SERVER_ACCESSIBLE
            if (status != null) {
                Method ordinalMethod = status.getClass().getMethod("ordinal");
                int ordinal = (int) ordinalMethod.invoke(status);

                // ENTITY_TICKING обычно имеет ordinal >= 6 в C2ME
                // Но лучше проверить через toChunkLevelType()
                Method levelTypeMethod = status.getClass().getMethod("toChunkLevelType");
                Object levelType = levelTypeMethod.invoke(status);

                // Если тип ENTITY_TICKING или выше - можно тикать
                return "ENTITY_TICKING".equals(levelType.toString());
            }
        } catch (Exception e) {
            // Fallback на vanilla если что-то пошло не так
            return isChunkReadyVanilla(holder);
        }

        return false;
    }

    /**
     * Vanilla проверка - медленнее, но работает всегда
     */
    private static boolean isChunkReadyVanilla(ChunkHolder holder) {
        // Проверяем уровень чанка без создания Future
        // getTickingChunkFuture возвращает закешированный future
        var future = holder.getTickingChunkFuture();

        // Проверяем ТОЛЬКО isDone, не вызываем get/join
        return future.isDone() && future.getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).isSuccess();
    }
}