package com.axalotl.async.common.c2me;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Интеграция с C2ME для асинхронной загрузки чанков
 */
public class C2MEIntegration {
    private static final boolean C2ME_AVAILABLE;
    private static Method GET_CHUNK_STATUS_METHOD;

    static {
        boolean available = false;
        try {
            Class.forName("com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus");
            available = true;

            Class<?> holderClass = ChunkHolder.class;
            GET_CHUNK_STATUS_METHOD = holderClass.getMethod("getChunkStatus");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // C2ME не установлен
        }
        C2ME_AVAILABLE = available;
    }

    public static boolean isAvailable() {
        return C2ME_AVAILABLE;
    }

    /**
     * Быстрая неблокирующая проверка готовности чанка для entity ticking
     */
    public static boolean isChunkReadyForEntityTicking(ServerLevel world, Entity entity) {
        ChunkPos chunkPos = entity.chunkPosition();
        ChunkHolder holder = world.getChunkSource().getVisibleChunkIfPresent(chunkPos.toLong());

        if (holder == null) {
            return false;
        }

        if (C2ME_AVAILABLE) {
            return isChunkReadyC2ME(holder);
        } else {
            return isChunkReadyVanilla(holder);
        }
    }

    /**
     * 🎯 ГЛАВНЫЙ МЕТОД - для Lithium миксина!
     *
     * КРИТИЧНО: НЕ вызывает holder.getChunkIfPresent() чтобы избежать рекурсии!
     * Просто запрашивает загрузку и возвращает null.
     */
    @Nullable
    public static ChunkAccess requestAsyncChunkLoad(ServerLevel world, int chunkX, int chunkZ, ChunkStatus status) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        ServerChunkCache chunkSource = world.getChunkSource();

        // 🎯 ПРОСТО запрашиваем загрузку через публичный API
        // C2ME перехватит через свои миксины!
        chunkSource.mainThreadProcessor.execute(() -> {
            chunkSource.addRegionTicket(
                    TicketType.UNKNOWN,
                    chunkPos,
                    ChunkLevel.byStatus(status),
                    chunkPos
            );
        });

        // Возвращаем null - чанк загрузится в фоне
        // Entity пропустит тик, в следующем тике чанк готов!
        return null;
    }

    /**
     * Проверка через C2ME API
     */
    private static boolean isChunkReadyC2ME(ChunkHolder holder) {
        try {
            Object status = GET_CHUNK_STATUS_METHOD.invoke(holder);

            if (status != null) {
                Method levelTypeMethod = status.getClass().getMethod("toChunkLevelType");
                Object levelType = levelTypeMethod.invoke(status);

                return "ENTITY_TICKING".equals(levelType.toString());
            }
        } catch (Exception e) {
            return isChunkReadyVanilla(holder);
        }
        return false;
    }

    /**
     * Vanilla проверка
     */
    private static boolean isChunkReadyVanilla(ChunkHolder holder) {
        var future = holder.getTickingChunkFuture();
        return future.isDone() && future.getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).isSuccess();
    }
}