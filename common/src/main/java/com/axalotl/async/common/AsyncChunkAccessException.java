package com.axalotl.async.common;

import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Бросается когда async поток пытается получить доступ к непрогруженному чанку.
 *
 * Это НЕ ошибка - это сигнал для ParallelProcessor что энтити нужно
 * перенести обратно в синхронную очередь и тикнуть на main thread.
 *
 * Это решает проблему дедлока: вместо блокировки async потока мы
 * откладываем тик энтити до следующего тика на main thread.
 */
public class AsyncChunkAccessException extends RuntimeException {
    private final int chunkX;
    private final int chunkZ;
    private final ChunkStatus requestedStatus;

    public AsyncChunkAccessException(int chunkX, int chunkZ, ChunkStatus status) {
        super(String.format("Async thread tried to access unloaded chunk [%d, %d] with status %s",
                chunkX, chunkZ, status));
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.requestedStatus = status;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public ChunkStatus getRequestedStatus() {
        return requestedStatus;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // Оптимизация: не заполняем stack trace - это не настоящая ошибка
        return this;
    }
}