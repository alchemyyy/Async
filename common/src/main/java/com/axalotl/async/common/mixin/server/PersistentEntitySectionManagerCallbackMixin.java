package com.axalotl.async.common.mixin.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
#if MC_VER_1_21_11
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Shadow;
import java.util.concurrent.locks.ReentrantLock;
#else
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
#endif

#if MC_VER_1_21_11
@Mixin(PersistentEntitySectionManager.Callback.class)
public abstract class PersistentEntitySectionManagerCallbackMixin {

    @Shadow
    private EntitySection<?> currentSection;

    @Unique
    private final ReentrantLock async$lock = new ReentrantLock();

    @Unique
    private volatile boolean async$removed = false;

    @WrapMethod(method = "onMove")
    private void onMove(Operation<Void> original) {
        if (async$removed) {
            return;
        }

        if (!async$lock.tryLock()) {
            return;
        }

        try {
            if (!async$removed && currentSection != null) {
                original.call();
            }
        } finally {
            async$lock.unlock();
        }
    }

    @WrapMethod(method = "onRemove")
    private void onRemove(Entity.RemovalReason reason, Operation<Void> original) {
        async$lock.lock();
        try {
            if (async$removed) {
                return;
            }
            async$removed = true;

            if (currentSection != null) {
                original.call(reason);
            }
        } finally {
            async$lock.unlock();
        }
    }
}
#else
@Mixin(PersistentEntitySectionManager.Callback.class)
public abstract class PersistentEntitySectionManagerCallbackMixin implements AutoCloseable {

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "onMove")
    private void onMove(Operation<Void> original) {
        synchronized (async$lock) {
            original.call();
        }
    }

    //TODO Entity wasn't found in section
    @Redirect(method = "onMove", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V"), remap = false)
    private void notFoundSectionDisable(Logger instance, String s, Object[] objects) {
    }

    @WrapMethod(method = "onRemove")
    private void onRemove(Entity.RemovalReason reason, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(reason);
        }
    }
}
#endif
