package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin adds XP to smithing skill when using an anvil
 */
@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {

    @Inject(method = "onTakeOutput", at = @At("TAIL"))
    private void onTakeOutputMixin(PlayerEntity player, ItemStack stack, CallbackInfo info) {
        if (!player.getWorld().isClient() && !player.isCreative() && !stack.isEmpty()) {
            // Award smithing skill XP based on anvil use
            int xpAmount = 4;

            // More XP if the item is enchanted
            if (stack.hasEnchantments()) {
                xpAmount += 2;
            }

            SkillExperienceManager.getInstance().awardSkillXp(
                    player, 7, xpAmount, SkillXpType.SMITHING_ANVIL);
        }
    }
}