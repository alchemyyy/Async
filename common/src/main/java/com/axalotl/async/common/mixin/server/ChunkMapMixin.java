package com.axalotl.async.common.mixin.server;

#if MC_VER_1_21_8 || MC_VER_1_21_11
import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.config.AsyncConfig;
#endif
#if MC_VER_1_21_11
import com.axalotl.async.common.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
#endif
import com.axalotl.async.common.parallelised.fastutil.Int2ObjectConcurrentHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
#if MC_VER_1_21_8 || MC_VER_1_21_11
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
#endif
#if MC_VER_1_21_11
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
#endif
import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
#if MC_VER_1_21_11
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
#endif
import net.minecraft.world.entity.Entity;
#if MC_VER_1_21_11
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
#endif
#if MC_VER_1_21_8 || MC_VER_1_21_11
import net.minecraft.world.level.chunk.LevelChunk;
#endif
#if MC_VER_1_21_11
import net.minecraft.world.level.chunk.status.ChunkStatus;
#endif
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
import net.minecraft.world.level.chunk.storage.ChunkStorage;
#endif
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
#if MC_VER_1_21_11
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.phys.Vec3;
#endif
#if MC_VER_1_21_1
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
#else
import org.spongepowered.asm.mixin.*;
#endif
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
#if MC_VER_1_21_8 || MC_VER_1_21_11
import java.util.ArrayList;
#endif
import java.util.List;
#if MC_VER_1_21_8 || MC_VER_1_21_11
import java.util.concurrent.CompletableFuture;
#endif
import java.util.concurrent.CopyOnWriteArrayList;
#if MC_VER_1_21_8 || MC_VER_1_21_11
import java.util.function.Consumer;
#endif

@Mixin(value = ChunkMap.class, priority = 1500)
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
public abstract class ChunkMapMixin extends ChunkStorage implements ChunkHolder.PlayerProvider {
#elif MC_VER_1_21_11
public abstract class ChunkMapMixin extends SimpleRegionStorage implements ChunkHolder.PlayerProvider {
#endif

    @Shadow
    @Final
    @Mutable
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow
    @Final
    @Mutable
    private List<ChunkGenerationTask> pendingGenerationTasks;

#if MC_VER_1_21_11
    @Shadow
    @Final
    @Mutable
    private LongSet chunksToEagerlySave;
#endif

#if MC_VER_1_21_8 || MC_VER_1_21_11
    @Shadow
    @Final
    private ChunkMap.DistanceManager distanceManager;

    @Shadow
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;
#endif

#if MC_VER_1_21_11
    @Shadow
    @Final
    ServerLevel level;
#endif

#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
    public ChunkMapMixin(RegionStorageInfo regionStorageInfo, Path directory, DataFixer dataFixer, boolean dsync) {
        super(regionStorageInfo, directory, dataFixer, dsync);
    }
#elif MC_VER_1_21_11
    public ChunkMapMixin(RegionStorageInfo p_326109_, Path p_321582_, DataFixer p_321815_, boolean p_321788_, DataFixTypes p_321522_) {
        super(p_326109_, p_321582_, p_321815_, p_321788_, p_321522_);
    }
#endif

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replaceConVars(CallbackInfo ci) {
        entityMap = new Int2ObjectConcurrentHashMap<>();
        pendingGenerationTasks = new CopyOnWriteArrayList<>();
#if MC_VER_1_21_11
        chunksToEagerlySave = new ConcurrentLongLinkedOpenHashSet();
#endif
    }

    @WrapMethod(method = "addEntity")
    private synchronized void addEntity(Entity entity, Operation<Void> original) {
        original.call(entity);
    }

    @WrapMethod(method = "removeEntity")
    private synchronized void removeEntity(Entity entity, Operation<Void> original) {
        original.call(entity);
    }

    @WrapMethod(method = "releaseGeneration")
    private synchronized void releaseGeneration(GenerationChunkHolder chunk, Operation<Void> original) {
        original.call(chunk);
    }

#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
    @Inject(method = "addEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;pauseInIde(Ljava/lang/Throwable;)Ljava/lang/Throwable;"), cancellable = true)
#elif MC_VER_1_21_11
    @Inject(method = "addEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;pauseInIde(Ljava/lang/Throwable;)Ljava/lang/Throwable;"), cancellable = true)
#endif
    private void skipThrowLoadEntity(Entity entity, CallbackInfo ci) {
        ci.cancel();
    }

#if MC_VER_1_21_11
    @WrapMethod(method = "collectSpawningChunks")
    private void async$optimizedCollectSpawningChunks(List<LevelChunk> result, Operation<Void> original) {
        List<ServerPlayer> players = this.level.players();
        double[] playerX = new double[players.size()];
        double[] playerZ = new double[players.size()];
        int playerCount = 0;

        for (int i = 0, size = players.size(); i < size; i++) {
            ServerPlayer player = players.get(i);
            if (!player.isSpectator()) {
                Vec3 pos = player.position();
                playerX[playerCount] = pos.x;
                playerZ[playerCount] = pos.z;
                playerCount++;
            }
        }

        if (playerCount == 0) return;

        LongIterator it = this.distanceManager.getSpawnCandidateChunks();

        while (it.hasNext()) {
            ChunkHolder holder = this.visibleChunkMap.get(it.nextLong());
            if (holder == null) continue;

            ChunkAccess chunk = holder.getTickingChunk();
            if (!(chunk instanceof LevelChunk lc)) continue;

            ChunkPos pos = holder.getPos();
            double cx = SectionPos.sectionToBlockCoord(pos.x, 8);
            double cz = SectionPos.sectionToBlockCoord(pos.z, 8);

            for (int i = 0; i < playerCount; i++) {
                double dx = cx - playerX[i];
                double dz = cz - playerZ[i];
                if (dx * dx + dz * dz < 16384.0) {
                    result.add(lc);
                    break;
                }
            }
        }
    }
#endif

#if MC_VER_1_21_8
    @WrapMethod(method = "forEachBlockTickingChunk")
    private void forEachBlockTickingChunk(Consumer<LevelChunk> action, Operation<Void> original) {
        if (!AsyncConfig.disabled && AsyncConfig.enableAsyncRandomTicks) {
            CompletableFuture.runAsync(() -> original.call(action), ParallelProcessor.tickPool).exceptionally(e -> {
                ParallelProcessor.LOGGER.error("Error in async random tick, switching to synchronous", e);
                original.call(action);
                return null;
            });
        } else {
            original.call(action);
        }
    }

    @WrapMethod(method = "forEachBlockTickingChunk")
    private void forEachBlockTicking(Consumer<LevelChunk> action, Operation<Void> original) {
        if (!AsyncConfig.disabled && AsyncConfig.enableAsyncRandomTicks) {
            List<Long> keys = new ArrayList<>();
            distanceManager.forEachEntityTickingChunk(keys::add);

            for (long chunkPos : keys) {
                ChunkHolder holder = visibleChunkMap.get(chunkPos);
                if (holder != null) {
                    LevelChunk chunk = holder.getTickingChunk();
                    if (chunk != null) action.accept(chunk);
                }
            }
        } else {
            original.call(action);
        }
    }
#elif MC_VER_1_21_11
    @WrapMethod(method = "forEachBlockTickingChunk")
    private void forEachBlockTickingChunk(Consumer<LevelChunk> action, Operation<Void> original) {
        if (!AsyncConfig.disabled && AsyncConfig.enableAsyncRandomTicks) {
            // Snapshot chunk positions for thread-safe iteration
            List<Long> keys = new ArrayList<>();
            distanceManager.forEachEntityTickingChunk(keys::add);

            for (long chunkPos : keys) {
                ChunkHolder holder = visibleChunkMap.get(chunkPos);
                if (holder != null) {
                    LevelChunk chunk = holder.getTickingChunk();
                    if (chunk != null) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(
                                () -> {
                                    if (chunk.getLevel() != null) {
                                        action.accept(chunk);
                                    }
                                },
                                ParallelProcessor.tickPool
                        ).exceptionally(e -> {
                            ParallelProcessor.LOGGER.error("Error in async random tick", e);
                            return null;
                        });
                        ParallelProcessor.addTask(future);
                    }
                }
            }
        } else {
            original.call(action);
        }
    }
#endif
}
