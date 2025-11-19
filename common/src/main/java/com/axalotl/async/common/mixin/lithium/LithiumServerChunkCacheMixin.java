package com.axalotl.async.common.mixin.lithium;

import com.axalotl.async.common.AsyncChunkAccessException;
import com.axalotl.async.common.utils.ChunkLoadTricks;
import net.caffeinemc.mods.lithium.common.world.chunk.ChunkHolderExtended;
import net.caffeinemc.mods.lithium.mixin.world.chunk_access.GenerationChunkHolderAccessor;
import net.minecraft.Util;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * Async-совместимый миксин для ServerChunkCache, который работает с Lithium.
 *
 * КЛЮЧЕВОЕ ОТЛИЧИЕ: Async потоки НЕ БЛОКИРУЮТСЯ при загрузке чанков.
 * Вместо этого бросается AsyncChunkAccessException, которую ловит ParallelProcessor
 * и переносит энтити обратно в синхронную очередь.
 */
@Mixin(ServerChunkCache.class)
public abstract class LithiumServerChunkCacheMixin extends ChunkSource {
    @Shadow @Final public ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    @Shadow @Final private DistanceManager distanceManager;
    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final Thread mainThread;

    @Unique private long async$time;
    @Unique private final long[] async$cacheKeys = new long[4];
    @Unique private final ChunkAccess[] async$cacheChunks = new ChunkAccess[4];

    @Shadow public abstract ChunkHolder getVisibleChunkIfPresent(long pos);
    @Shadow protected abstract boolean chunkAbsent(ChunkHolder holder, int level);
    @Shadow public abstract void tick(@NotNull BooleanSupplier shouldKeepTicking, boolean tickChunks);
    @Shadow abstract boolean runDistanceManagerUpdates();

    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(BooleanSupplier shouldKeepTicking, boolean tickChunks, CallbackInfo ci) {
        ++this.async$time;
    }

    /**
     * @author _Axa_lotL_
     * @reason Async-compatible + Lithium integration
     */
    @Nullable
    @Overwrite
    public ChunkAccess getChunk(int x, int z, @NotNull ChunkStatus status, boolean create) {
        if (Thread.currentThread() != this.mainThread) {
            // 🚀 ASYNC ПОТОК - неблокирующий доступ
            return this.async$getChunkNonBlocking(x, z, status, create);
        } else {
            // MAIN THREAD - полная Lithium логика с кешем
            long key = async$createCacheKey(x, z, status);

            for (int i = 0; i < 4; ++i) {
                if (key == this.async$cacheKeys[i]) {
                    ChunkAccess chunk = this.async$cacheChunks[i];
                    if (chunk != null || !create) {
                        return chunk;
                    }
                }
            }

            ChunkAccess chunk = this.async$getChunkBlocking(x, z, status, create);
            if (chunk != null) {
                this.async$addToCache(key, chunk);
            } else if (create) {
                throw new IllegalStateException("Chunk not there when requested");
            }

            return chunk;
        }
    }

    /**
     * 🎯 НЕБЛОКИРУЮЩИЙ доступ для async потоков!
     *
     * Стратегия:
     * 1. Пытаемся получить ГОТОВЫЙ чанк (без блокировок)
     * 2. Используем Lithium's ChunkLoadTricks для получения загружающегося чанка
     * 3. Если чанк НЕ ГОТОВ - бросаем AsyncChunkAccessException
     * 4. ParallelProcessor ловит исключение и переносит энтити в sync очередь
     */
    @Unique
    private ChunkAccess async$getChunkNonBlocking(int x, int z, ChunkStatus status, boolean create) {
        long pos = ChunkPos.asLong(x, z);
        ChunkHolder holder = this.getVisibleChunkIfPresent(pos);

        // 1. Пытаемся получить готовый чанк (fast path)
        if (holder != null) {
            ChunkAccess chunk = holder.getChunkIfPresent(status);
            if (chunk != null) {
                return chunk instanceof ImposterProtoChunk proto ? proto.getWrapped() : chunk;
            }

            // 2. Lithium's ChunkLoadTricks - пытаемся получить загружающийся чанк (NeoForge)
            ChunkAccess loading = ChunkLoadTricks.tryRetrieveCurrentlyLoading(holder);
            if (loading != null) {
                return loading instanceof ImposterProtoChunk proto ? proto.getWrapped() : loading;
            }

            // 3. Проверяем completed future (Lithium optimization)
            if (!((GenerationChunkHolderAccessor) holder).invokeCannotBeLoaded(status)) {
                CompletableFuture<ChunkResult<ChunkAccess>> future =
                        ((GenerationChunkHolderAccessor) holder).lithium$getChunkFuturesByStatus().get(status.getIndex());

                if (future != null && future.isDone()) {
                    ChunkAccess completed = future.join().orElse(null);
                    if (completed != null) {
                        return completed instanceof ImposterProtoChunk proto ? proto.getWrapped() : completed;
                    }
                }
            }
        }

        // ❌ Чанк не готов!
        if (create) {
            // Запрашиваем загрузку чанка (неблокирующе)
            ChunkPos chunkPos = new ChunkPos(x, z);
            this.mainThreadProcessor.execute(() -> {
                this.distanceManager.addTicket(
                        TicketType.UNKNOWN,
                        chunkPos,
                        ChunkLevel.byStatus(status),
                        chunkPos
                );
            });

            // 🎯 Бросаем исключение - сигнал ParallelProcessor что энтити надо тикать синхронно!
            throw new AsyncChunkAccessException(x, z, status);
        }

        return null;
    }

    // ========== LITHIUM BLOCKING LOGIC (для main thread) ==========

    @Unique
    private ChunkAccess async$getChunkBlocking(int x, int z, ChunkStatus leastStatus, boolean create) {
        long key = ChunkPos.asLong(x, z);
        int level = ChunkLevel.byStatus(leastStatus);
        ChunkHolder holder = this.getVisibleChunkIfPresent(key);

        // Lithium's NeoForge chunk loading trick
        ChunkAccess chunkAccess = ChunkLoadTricks.tryRetrieveCurrentlyLoading(holder);
        if (chunkAccess != null) {
            return chunkAccess;
        }

        if (this.chunkAbsent(holder, level)) {
            if (!create) {
                return null;
            }
            this.async$createChunkLoadTicket(x, z, level);
            this.runDistanceManagerUpdates();
            holder = this.getVisibleChunkIfPresent(key);
            if (this.chunkAbsent(holder, level)) {
                throw Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
            }
        } else if (create && ((ChunkHolderExtended) holder).lithium$updateLastAccessTime(this.async$time)) {
            this.async$createChunkLoadTicket(x, z, level);
        }

        // Lithium fast path - direct future access
        if (!((GenerationChunkHolderAccessor) holder).invokeCannotBeLoaded(leastStatus)) {
            CompletableFuture<ChunkResult<ChunkAccess>> directlyAccessedFuture =
                    ((GenerationChunkHolderAccessor) holder).lithium$getChunkFuturesByStatus().get(leastStatus.getIndex());
            if (directlyAccessedFuture != null && directlyAccessedFuture.isDone()) {
                ChunkAccess chunk = directlyAccessedFuture.join().orElse(null);
                if (chunk != null) {
                    return chunk;
                }
            }
        }

        CompletableFuture<ChunkResult<ChunkAccess>> loadFuture = holder.scheduleChunkGenerationTask(leastStatus, this.chunkMap);
        if (!loadFuture.isDone()) {
            this.mainThreadProcessor.managedBlock(loadFuture::isDone);
        }

        return loadFuture.join().orElse(null);
    }

    @Unique
    private void async$createChunkLoadTicket(int x, int z, int level) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        this.distanceManager.addTicket(TicketType.UNKNOWN, chunkPos, level, chunkPos);
    }

    @Unique
    private static long async$createCacheKey(int chunkX, int chunkZ, ChunkStatus status) {
        return (long) chunkX & 0xfffffffL | ((long) chunkZ & 0xfffffffL) << 28 | (long) status.getIndex() << 56;
    }

    @Unique
    private void async$addToCache(long key, ChunkAccess chunk) {
        for (int i = 3; i > 0; --i) {
            this.async$cacheKeys[i] = this.async$cacheKeys[i - 1];
            this.async$cacheChunks[i] = this.async$cacheChunks[i - 1];
        }
        this.async$cacheKeys[0] = key;
        this.async$cacheChunks[0] = chunk;
    }

    @Inject(method = "clearCache()V", at = @At("HEAD"))
    private void onCachesCleared(CallbackInfo ci) {
        Arrays.fill(this.async$cacheKeys, Long.MAX_VALUE);
        Arrays.fill(this.async$cacheChunks, null);
    }
}