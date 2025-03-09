package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This mixin adds XP to magic skill when enchanting items
 */
@Mixin(EnchantmentScreenHandler.class)
public class EnchantmentScreenHandlerMixin {

    @Inject(method = "onButtonClick", at = @At("TAIL"))
    private void onButtonClickMixin(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!player.getWorld().isClient() && !player.isCreative()) {
            // Award magic skill XP based on enchantment level selected
            // buttonId is the enchantment level (0, 1, or 2)
            int xpAmount = 3 + (id * 2); // 3 XP for level 1, 5 XP for level 2, 7 XP for level 3

            SkillExperienceManager.getInstance().awardSkillXp(
                    player, 5, xpAmount, SkillXpType.MAGIC_ENCHANTING);
        }
    }
}