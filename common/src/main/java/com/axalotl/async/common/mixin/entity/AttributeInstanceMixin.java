package com.axalotl.async.common.mixin.entity;

import com.axalotl.async.common.parallelised.ConcurrentCollections;
#if MC_VER_1_21_11
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
#if MC_VER_1_21_11
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
#if MC_VER_1_21_11
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#endif

import java.util.Map;
#if MC_VER_1_21_11
import java.util.concurrent.ConcurrentHashMap;
#endif

@Mixin(AttributeInstance.class)
public class AttributeInstanceMixin {

#if MC_VER_1_21_11
    @Shadow
    @Final
    @Mutable
    private Map<Identifier, AttributeModifier> modifierById;

    @Shadow
    @Final
    @Mutable
    private Map<Identifier, AttributeModifier> permanentModifiers;

    @Shadow
    private final Map<AttributeModifier.Operation, Map<Identifier, AttributeModifier>> modifiersByOperation = ConcurrentCollections.newHashMap();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void makeThreadSafe(CallbackInfo ci) {
        modifierById = new ConcurrentHashMap<>(modifierById);
        permanentModifiers = new ConcurrentHashMap<>(permanentModifiers);
    }

    @WrapMethod(method = "getModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Map;")
    private Map<Identifier, AttributeModifier> getModifiersConcurrent(AttributeModifier.Operation operation, Operation<Map<Identifier, AttributeModifier>> original) {
        return modifiersByOperation.computeIfAbsent(operation, op -> ConcurrentCollections.newHashMap());
    }
#else
   @Shadow
   private final Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation = ConcurrentCollections.newHashMap();

   @Shadow
   private final Map<ResourceLocation, AttributeModifier> modifierById = ConcurrentCollections.newHashMap();

   @Shadow
   private final Map<ResourceLocation, AttributeModifier> permanentModifiers = ConcurrentCollections.newHashMap();
#endif
}
