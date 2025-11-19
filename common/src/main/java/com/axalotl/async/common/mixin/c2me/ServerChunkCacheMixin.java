package com.axalotl.async.common.mixin.c2me;

import com.axalotl.async.common.accessor.ServerChunkCacheAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(value = ServerChunkCache.class, priority = 2000)
public class ServerChunkCacheMixin implements ServerChunkCacheAccessor {

    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private static final ThreadLocal<ConcurrentLinkedQueue<BlockPos>> async$deferredBlockChanges =
            ThreadLocal.withInitial(ConcurrentLinkedQueue::new);

    @Inject(method = "blockChanged", at = @At("HEAD"), cancellable = true)
    private void deferAsyncBlockChanged(BlockPos pos, CallbackInfo ci) {
        if (!this.level.getServer().isSameThread()) {
            async$deferredBlockChanges.get().offer(pos.immutable());
            ci.cancel();
        }
    }

    @Override
    public void async$flushDeferredBlockChanges() {
        ConcurrentLinkedQueue<BlockPos> deferred = async$deferredBlockChanges.get();
        BlockPos pos;
        while ((pos = deferred.poll()) != null) {
            ((ServerChunkCache)(Object)this).blockChanged(pos);
        }
    }
}