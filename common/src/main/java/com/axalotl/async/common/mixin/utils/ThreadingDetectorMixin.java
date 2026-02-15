package com.axalotl.async.common.mixin.utils;

import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.util.ThreadingDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ThreadingDetector.class)
public abstract class ThreadingDetectorMixin {

    @Unique
    private final ReentrantLock async$lock = new ReentrantLock();

    /**
     * @author FurryAsync
     * @reason Replace crash-on-contention with proper mutual exclusion for write operations.
     * PalettedContainer.get() never calls acquire(), so reads remain lock-free.
     * Only write methods (set, getAndSet, read, write, pack) call acquire()/release().
     */
    @Overwrite
    public void checkAndLock() {
        async$lock.lock();
    }

    /**
     * @author FurryAsync
     * @reason Pair with checkAndLock replacement.
     */
    @Overwrite
    public void checkAndUnlock() {
        async$lock.unlock();
    }
}
