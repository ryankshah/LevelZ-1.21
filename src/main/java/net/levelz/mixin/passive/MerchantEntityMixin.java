package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin adds XP to bartering skill when trading with villagers
 */
@Mixin(MerchantEntity.class)
public class MerchantEntityMixin {

    @Shadow
    private PlayerEntity customer;

    @Inject(method = "trade", at = @At("TAIL"))
    private void tradeMixin(TradeOffer offer, CallbackInfo info) {
        if (customer != null && !customer.getWorld().isClient() && !customer.isCreative()) {
            // Award bartering skill XP when trading
            SkillExperienceManager.getInstance().awardSkillXp(
                    customer, 10, 3, SkillXpType.BARTERING_TRADE);
        }
    }
}