package com.axalotl.async.neoforge.mixin.utils;

import com.axalotl.async.common.utils.ChunkLoadTricks;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkLoadTricks.class)
public class ChunkLoadTricksMixin {

    /**
     * @author _Axa_lotL_
     * @reason Apply NeoForge's chunk loading trick to prevent deadlocks
     */
    @Overwrite
    public static ChunkAccess tryRetrieveCurrentlyLoading(ChunkHolder holder) {
        if (holder != null) {
            // На NeoForge это поле существует (добавляется патчами самого NeoForge)
            return holder.currentlyLoading;
        }
        return null;
    }
}