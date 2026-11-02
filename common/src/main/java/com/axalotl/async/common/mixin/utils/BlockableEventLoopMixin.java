package com.axalotl.async.common.mixin.utils;

import com.axalotl.async.common.ServerThreadMods;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockableEventLoop.class)
public abstract class BlockableEventLoopMixin {
    @ModifyReturnValue(method = "isSameThread", at = @At("RETURN"))
    private boolean async$applyServerThreadMods(boolean sameThread) {
        return ServerThreadMods.isSameThread((BlockableEventLoop<?>) (Object) this, sameThread);
    }
}
