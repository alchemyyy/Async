package com.axalotl.async.common.utils;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkLoadTricks {
    /**
     * На Fabric это всегда null.
     * На NeoForge этот метод будет перезаписан через Mixin, чтобы возвращать holder.currentlyLoading
     */
    public static ChunkAccess tryRetrieveCurrentlyLoading(ChunkHolder holder) {
        return null;
    }
}