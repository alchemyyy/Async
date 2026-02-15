package com.axalotl.async.common.mixin.utils;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LegacyRandomSource.class)
public abstract class LegacyRandomSourceMixin {

    @Shadow
    @Final
    private AtomicLong seed;

    /**
     * @author FurryAsync
     * @reason Replace crash-on-contention with lock-free CAS retry loop.
     * Vanilla uses compareAndSet and throws on failure. We retry instead,
     * giving us thread-safe random without any synchronized overhead.
     */
    @Overwrite
    public int next(int bits) {
        long i, j;
        do {
            i = this.seed.get();
            j = (i * 25214903917L + 11L) & 281474976710655L;
        } while (!this.seed.compareAndSet(i, j));
        return (int) (j >> (48 - bits));
    }
}
