package com.axalotl.async.common.mixin.world;

import com.axalotl.async.common.parallelised.ConcurrentCollections;
#if MC_VER_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif
import net.minecraft.world.RandomSequence;
import net.minecraft.world.RandomSequences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(RandomSequences.class)
public class RandomSequencesMixin {

    @Shadow
#if MC_VER_1_21_11
    private final Map<Identifier, RandomSequence> sequences = ConcurrentCollections.newHashMap();
#else
    private final Map<ResourceLocation, RandomSequence> sequences = ConcurrentCollections.newHashMap();
#endif
}
