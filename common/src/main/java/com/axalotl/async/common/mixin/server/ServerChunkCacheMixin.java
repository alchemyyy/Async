package com.axalotl.async.common.mixin.server;

import com.axalotl.async.common.AsyncChunkAccessException;
import com.axalotl.async.common.ParallelProcessor;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = ServerChunkCache.class, priority = 900)
public abstract class ServerChunkCacheMixin extends ChunkSource {
    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final Thread mainThread;
    @Shadow @Final private DistanceManager distanceManager;
    @Shadow @Final public ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    @Shadow @Final public ServerLevel level;

    @Shadow public abstract @Nullable ChunkHolder getVisibleChunkIfPresent(long pos);


    @Inject(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void async$nonBlockingGetChunk(int x, int z, ChunkStatus status, boolean create,
                                           CallbackInfoReturnable<ChunkAccess> cir) {

        if (Thread.currentThread() == this.mainThread) {
            return;
        }

        long pos = ChunkPos.asLong(x, z);
        ChunkHolder holder = this.getVisibleChunkIfPresent(pos);


        if (holder != null) {
            ChunkAccess chunk = holder.getChunkIfPresent(status);
            if (chunk != null) {
                cir.setReturnValue(chunk instanceof ImposterProtoChunk proto ? proto.getWrapped() : chunk);
                return;
            }


            var future = holder.scheduleChunkGenerationTask(status, this.chunkMap);
            if (future != null && future.isDone()) {
                ChunkAccess completed = future.join().orElse(null);
                if (completed != null) {
                    cir.setReturnValue(completed instanceof ImposterProtoChunk proto ? proto.getWrapped() : completed);
                    return;
                }
            }
        }


        if (create) {

            ChunkPos chunkPos = new ChunkPos(x, z);
            this.mainThreadProcessor.execute(() -> {
                this.distanceManager.addTicket(
                        TicketType.UNKNOWN,
                        chunkPos,
                        ChunkLevel.byStatus(status),
                        chunkPos
                );
            });


            throw new AsyncChunkAccessException(x, z, status);
        }


        cir.setReturnValue(null);
    }


    @Inject(method = "getChunkNow", at = @At("HEAD"), cancellable = true, require = 0)
    private void async$nonBlockingGetChunkNow(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
        if (Thread.currentThread() != this.mainThread) {
            ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
            if (holder != null) {
                ChunkAccess chunk = holder.getChunkIfPresent(ChunkStatus.FULL);
                if (chunk instanceof LevelChunk levelChunk) {
                    cir.setReturnValue(levelChunk);
                    return;
                }
            }

            cir.setReturnValue(null);
        }
    }


    @Redirect(
            method = "tickChunks",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"),
            require = 0
    )
    private void async$spawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState spawnState,
                                     boolean spawnFriendlies, boolean spawnMonsters, boolean forcedDespawn) {
        ParallelProcessor.asyncSpawnForChunk(level, chunk, spawnState, spawnFriendlies, spawnMonsters, forcedDespawn);
    }
}