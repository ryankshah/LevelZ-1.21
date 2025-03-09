package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This mixin adds XP to luck skill when fishing
 */
@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin
{
    @Shadow @Nullable public abstract PlayerEntity getPlayerOwner();

    @Inject(method = "use", at = @At("TAIL"))
    private void useMixin(ItemStack usedItem, CallbackInfoReturnable<Integer> info) {
        PlayerEntity owner = getPlayerOwner();
        if (owner != null && !owner.getWorld().isClient() && !owner.isCreative()) {
            // Award luck skill XP when catching fish
            SkillExperienceManager.getInstance().awardSkillXp(
                    owner, 11, 3, SkillXpType.LUCK_FISHING);
        }
    }
}