package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin adds XP to ranged combat when projectiles hit entities
 */
@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onEntityHitMixin(CallbackInfo info) {
        ProjectileEntity projectile = (ProjectileEntity)(Object)this;

        // Check if projectile came from a player
        if (projectile.getOwner() instanceof PlayerEntity player && !player.isCreative()) {
            // Award archery XP when projectile hits a target
            SkillExperienceManager xpManager = SkillExperienceManager.getInstance();

            // Base XP is 3, more for living entities based on their max health
            int baseXp = 3;
            if (projectile.getEntityWorld().getEntityById(projectile.getId()) instanceof LivingEntity target) {
                baseXp += Math.max(1, (int)(target.getMaxHealth() / 4));
            }

            xpManager.awardSkillXp(player, 3, baseXp, SkillXpType.RANGED_DAMAGE);
        }
    }
}