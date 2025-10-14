package com.axalotl.async.neoforge.mixin.server;

import com.axalotl.async.neoforge.parallelised.ConcurrentBlockSnapshotList;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;

@Mixin(value = Level.class, priority = 1500)
public class LevelCaptureBlockSnapshotsMixin {

    @Shadow
    @Final
    @Mutable
    public ArrayList<BlockSnapshot> capturedBlockSnapshots = new ConcurrentBlockSnapshotList();
}