// common/src/main/java/com/axalotl/async/common/mixin/create/BeltInventoryMixin.java
package com.axalotl.async.common.mixin.create;

import com.axalotl.async.common.parallelised.ConcurrentLinkedList;
import org.spongepowered.asm.mixin.*;

import java.util.List;

/**
 * Makes BeltInventory collections thread-safe to prevent ConcurrentModificationException
 * when Create's belt system is accessed from multiple threads in async entity ticking
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.transport.BeltInventory", remap = false)
public class BeltInventoryMixin {

    @Shadow
    @Final
    @Mutable
    private List<?> items = new ConcurrentLinkedList<>();

    @Shadow
    @Final
    @Mutable
    final List<?> toInsert = new ConcurrentLinkedList<>();

    @Shadow
    @Final
    @Mutable
    final List<?> toRemove = new ConcurrentLinkedList<>();
}