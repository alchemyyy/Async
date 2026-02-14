#if MC_VER_1_21_11
package com.axalotl.async.common.mixin.utils;

import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.config.AsyncConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevelAccessor.class)
public interface ServerLevelAccessorMixin {

    @Inject(method = "addFreshEntityWithPassengers", at = @At("HEAD"), cancellable = true)
    default void async$syncAddFreshEntityWithPassengers(Entity entity, CallbackInfo ci) {
        if (AsyncConfig.disabled || !AsyncConfig.enableAsyncSpawn) {
            return;
        }

        ci.cancel();

        synchronized (ParallelProcessor.getEntityAddLock()) {
            entity.getSelfAndPassengers().forEach(e -> ((ServerLevelAccessor) this).addFreshEntity(e));
        }
    }
}
#else
package com.axalotl.async.common.mixin.utils;
// Stub — only exists in 1.21.11+
public interface ServerLevelAccessorMixin {}
#endif
