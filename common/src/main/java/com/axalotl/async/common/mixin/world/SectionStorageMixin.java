package com.axalotl.async.common.mixin.world;

import com.axalotl.async.common.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import com.axalotl.async.common.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
#if MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
import it.unimi.dsi.fastutil.longs.LongSet;
#endif
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
#if MC_VER_1_21_1
import org.spongepowered.asm.mixin.Mutable;
#endif
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
#if MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
import java.util.concurrent.CompletableFuture;
#endif

@Mixin(SectionStorage.class)
#if MC_VER_1_21_1
public abstract class SectionStorageMixin<R> implements AutoCloseable {
#else
public abstract class SectionStorageMixin<R, P> implements AutoCloseable {
#endif

    @Shadow
#if MC_VER_1_21_1
    @Mutable
#endif
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectConcurrentHashMap<>();

#if MC_VER_1_21_1
    @Shadow
    @Mutable
    private final LongLinkedOpenHashSet dirty = new ConcurrentLongLinkedOpenHashSet();
#else
    @Shadow final private LongLinkedOpenHashSet dirtyChunks = new ConcurrentLongLinkedOpenHashSet();

    @Shadow final private Long2ObjectMap<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> pendingLoads = new Long2ObjectConcurrentHashMap<>();

    @Shadow final private LongSet loadedChunks = new ConcurrentLongLinkedOpenHashSet();
#endif

#if MC_VER_1_21_1
    @WrapMethod(method = "readColumn(Lnet/minecraft/world/level/ChunkPos;)V")
    private synchronized void release(ChunkPos chunkPos, Operation<Void> original) {
        original.call(chunkPos);
    }
#else
    @WrapMethod(method = "unpackChunk(Lnet/minecraft/world/level/ChunkPos;)V")
    private synchronized void release(ChunkPos pos, Operation<Void> original) {
        original.call(pos);
    }
#endif
}
