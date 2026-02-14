package com.axalotl.async.common.mixin.world;

#if MC_VER_1_21_11

// Stub: ChunkHolderMixin for 1.21.11+ lives in mixin.server package
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.server.level.ChunkHolder.class)
public abstract class ChunkHolderMixin {
}

#elif MC_VER_1_21_4 || MC_VER_1_21_8

import com.axalotl.async.common.parallelised.fastutil.ConcurrentShortHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkHolder.class, priority = 1500)
public abstract class ChunkHolderMixin {

    @Mutable
    @Shadow
    @Final
    private ShortSet[] changedBlocksPerSection;

    @Inject(method = "<init>", at = @At(value = "TAIL", target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", opcode = Opcodes.PUTFIELD))
    private void overwriteShortSet(ChunkPos pos, int level, LevelHeightAccessor world, LevelLightEngine lightingProvider, ChunkHolder.LevelChangeListener levelUpdateListener, ChunkHolder.PlayerProvider playersWatchingChunkProvider, CallbackInfo ci) {
        this.changedBlocksPerSection = new ConcurrentShortHashSet[world.getSectionsCount()];
    }

    @Redirect(method = "blockChanged", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", args = "array=set"))
    private void setBlockChanged(ShortSet[] array, int index, ShortSet value) {
        array[index] = new ConcurrentShortHashSet();
    }
}

#else

import com.axalotl.async.common.parallelised.fastutil.ConcurrentShortHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkHolder.class, priority = 1500)
public abstract class ChunkHolderMixin {

    @Mutable
    @Shadow
    @Final
    private ShortSet[] changedBlocksPerSection;

    @Inject(method = "<init>", at = @At(value = "TAIL", target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", opcode = Opcodes.PUTFIELD))
    private void overwriteShortSet(ChunkPos pos, int level, LevelHeightAccessor world, LevelLightEngine lightingProvider, ChunkHolder.LevelChangeListener levelUpdateListener, ChunkHolder.PlayerProvider playersWatchingChunkProvider, CallbackInfo ci) {
        this.changedBlocksPerSection = new ConcurrentShortHashSet[world.getSectionsCount()];
    }

    @Redirect(method = "blockChanged", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", args = "array=set"))
    private void setBlockChanged(ShortSet[] array, int index, ShortSet value) {
        array[index] = new ConcurrentShortHashSet();
    }
}

#endif
