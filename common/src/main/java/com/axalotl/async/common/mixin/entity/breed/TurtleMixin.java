package com.axalotl.async.common.mixin.entity.breed;

import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
#if MC_VER_1_21_11
import net.minecraft.world.entity.animal.turtle.Turtle;
#else
import net.minecraft.world.entity.animal.Turtle;
#endif

@Mixin(Turtle.TurtleBreedGoal.class)
public abstract class TurtleMixin extends BreedGoal {

    @Shadow
    @Final
    private Turtle turtle;

    public TurtleMixin(Animal animal, double speed) {
        super(animal, speed);
    }

#if MC_VER_1_21_11
    @Redirect(method = "breed()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/turtle/Turtle;setHasEgg(Z)V"))
#else
    @Redirect(method = "breed()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Turtle;setHasEgg(Z)V"))
#endif
    private void redirectSetHasEgg(Turtle instance, boolean value) {
        if (this.partner != null && !this.turtle.hasEgg() && !((Turtle) this.partner).hasEgg()) {
            if (this.turtle.getRandom().nextBoolean()) {
                this.turtle.setHasEgg(true);
            } else {
                ((Turtle) this.partner).setHasEgg(true);
            }
        }
    }
}
