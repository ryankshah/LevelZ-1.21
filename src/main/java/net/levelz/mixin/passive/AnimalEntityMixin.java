package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin adds XP for farming skills when breeding animals
 */
@Mixin(AnimalEntity.class)
public class AnimalEntityMixin {

    @Inject(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;Lnet/minecraft/entity/passive/PassiveEntity;)V",
            at = @At("TAIL"))
    private void onBreedMixin(ServerWorld world, AnimalEntity other, PassiveEntity baby, CallbackInfo info) {
        AnimalEntity self = (AnimalEntity)(Object)this;

        // Award farming XP to the player who bred the animals
        PlayerEntity player = self.getLovingPlayer();
        if (player != null && !player.isCreative()) {
            // Award farming skill XP (ID 8)
            SkillExperienceManager.getInstance().awardSkillXp(
                    player, 8, 5, SkillXpType.FARMING_BREEDING);
        }
    }
}