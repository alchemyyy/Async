package com.axalotl.async.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.world.level.chunk.LevelChunk;

public final class RandomTickBatch {

    private RandomTickBatch() {}

    public static void execute(
        LevelChunk[] chunks,
        int count,
        Consumer<LevelChunk> action
    ) {
        int parallelism = ParallelProcessor.tickPool.getParallelism();
        int sliceSize = (count + parallelism - 1) / parallelism;
        int taskCount = (count + sliceSize - 1) / sliceSize;

        CompletableFuture<?>[] futures = new CompletableFuture[taskCount];

        for (int t = 0; t < taskCount; t++) {
            int from = t * sliceSize;
            int to = Math.min(from + sliceSize, count);
            futures[t] = CompletableFuture.runAsync(
                () -> {
                    for (int i = from; i < to; i++) {
                        try {
                            action.accept(chunks[i]);
                        } catch (Throwable e) {
                            ParallelProcessor.LOGGER.error(
                                "Error in async random tick",
                                e
                            );
                        }
                    }
                },
                ParallelProcessor.tickPool
            );
        }

        ParallelProcessor.addTask(CompletableFuture.allOf(futures));
    }
}
