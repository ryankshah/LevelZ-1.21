package net.levelz.mixin.player;

import net.levelz.access.LevelManagerAccess;
import net.levelz.access.PlayerDropAccess;
import net.levelz.entity.LevelExperienceOrbEntity;
import net.levelz.experience.SkillExperienceManager;
import net.levelz.init.ConfigInit;
import net.levelz.level.LevelManager;
import net.levelz.util.BonusHelper;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements LevelManagerAccess, PlayerDropAccess {

    private final PlayerEntity playerEntity = (PlayerEntity) (Object) this;
    @Unique
    private final LevelManager levelManager = new LevelManager(playerEntity);

    @Unique
    private int killedMobsInChunk;
    @Unique
    @Nullable
    private Chunk killedMobChunk;

    @Unique
    private int movementCounter = 0;

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At(value = "TAIL"))
    public void readCustomDataFromNbtMixin(NbtCompound nbt, CallbackInfo info) {
        this.levelManager.readNbt(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At(value = "TAIL"))
    public void writeCustomDataToNbtMixin(NbtCompound nbt, CallbackInfo info) {
        this.levelManager.writeNbt(nbt);
    }

    @ModifyVariable(method = "addExhaustion", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;addExhaustion(F)V"), ordinal = 0, argsOnly = true)
    private float addExhaustionMixin(float original) {
        original *= BonusHelper.exhaustionReductionBonus(this.playerEntity);
        return original;
    }

    @ModifyVariable(method = "attack", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/Item;getBonusAttackDamage(Lnet/minecraft/entity/Entity;FLnet/minecraft/entity/damage/DamageSource;)F"), ordinal = 0)
    private boolean attackKnockbackkMixin(boolean original) {
        if (!original && BonusHelper.meleeKnockbackAttackChanceBonus(this.playerEntity)) {
            return true;
        }
        return original;
    }

    @ModifyVariable(method = "attack", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/Item;getBonusAttackDamage(Lnet/minecraft/entity/Entity;FLnet/minecraft/entity/damage/DamageSource;)F"), ordinal = 1)
    private boolean attackCriticalMixin(boolean original) {
        if (!original && BonusHelper.meleeCriticalAttackChanceBonus(this.playerEntity)) {
            return true;
        }
        return original;
    }

    @ModifyVariable(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getWeaponStack()Lnet/minecraft/item/ItemStack;"), ordinal = 0)
    private float attackMixin(float original) {
        if (this.playerEntity.isCreative()) {
            return original;
        }
        if (!levelManager.hasRequiredItemLevel(getWeaponStack().getItem())) {
            return 0.0f;
        }
        return original;
    }

    @ModifyVariable(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 0), ordinal = 0)
    private float attackCriticalDamageMixin(float original) {
        original += BonusHelper.meleeCriticalDamageBonus(this.playerEntity);
        return original;
    }

    @ModifyVariable(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getVelocity()Lnet/minecraft/util/math/Vec3d;"), ordinal = 3)
    private float attackDoubleDamageMixin(float original) {
        if (BonusHelper.meleeDoubleDamageBonus(this.playerEntity)) {
            original *= 2f;
        }
        return original;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"), cancellable = true)
    private void damageMixin(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        BonusHelper.damageReflectionBonus(this.playerEntity, source, amount);
        if (!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && BonusHelper.evadingDamageBonus(this.playerEntity)) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "eatFood", at = @At(value = "HEAD"))
    private void eatFoodMixin(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> info) {
        BonusHelper.foodIncreasionBonus(this.playerEntity, stack);
    }

    @Shadow
    public abstract ItemStack getWeaponStack();


    @Override
    public LevelManager getLevelManager() {
        return this.levelManager;
    }

    @Override
    public void increaseKilledMobStat(Chunk chunk) {
        if (killedMobChunk != null && killedMobChunk == chunk) {
            killedMobsInChunk++;
        } else {
            killedMobChunk = chunk;
            killedMobsInChunk = 0;
        }
    }

    @Override
    public void resetKilledMobStat() {
        killedMobsInChunk = 0;
    }

    @Override
    public boolean allowMobDrop() {
        return killedMobsInChunk < ConfigInit.CONFIG.mobKillCount;
    }

    @Override
    protected void dropXp(@Nullable Entity attacker) {
        if (this.playerEntity.getWorld() instanceof ServerWorld serverWorld && this.shouldDropXp() && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT) && ConfigInit.CONFIG.resetCurrentXp) {
            LevelExperienceOrbEntity.spawn(serverWorld, this.getPos(), (int) (this.levelManager.getLevelProgress() * this.levelManager.getNextLevelExperience()));
        }
        super.dropXp(attacker);
    }

    /**
     * Track player movement for agility skill
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickMixin(CallbackInfo info) {
        // Only track on server side
        if (this.getWorld().isClient()) {
            return;
        }

        PlayerEntity player = (PlayerEntity)(Object)this;

        // Track movement for agility XP
        if (player.isSprinting() && player.isOnGround() && !player.isCreative()) {
            movementCounter++;

            // Award agility XP after sprinting a certain distance
            if (movementCounter >= 100) { // Every ~5 seconds of sprinting
                movementCounter = 0;
                SkillExperienceManager.getInstance().awardSkillXp(
                        player, 4, 1, SkillExperienceManager.SkillXpType.AGILITY_MOVEMENT);
            }
        }
    }

    /**
     * Add constitution XP when using health potions or food to heal
     */
    @Inject(method = "eatFood", at = @At("TAIL"))
    private void healMixin(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        if (foodComponent.nutrition() > 0 && !this.getWorld().isClient() && !((PlayerEntity)(Object)this).isCreative()) {
            PlayerEntity player = (PlayerEntity)(Object)this;

            // Award constitution XP based on amount healed
            int healXp = Math.max(1, (int)(foodComponent.nutrition() / 2));
            SkillExperienceManager.getInstance().awardSkillXp(
                    player, 0, healXp, SkillExperienceManager.SkillXpType.DEFENSE_DAMAGE_TAKEN);
        }
    }

    /**
     * Update overall level calculation based on combat skills
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;updateTurtleHelmet()V"))
    private void updateOverallLevelMixin(CallbackInfo info) {
        if (this.getWorld().isClient() || ((PlayerEntity)(Object)this).isCreative()) {
            return;
        }

        // Update overall level every 20 ticks (1 second)
        if (this.age % 20 == 0) {
            PlayerEntity player = (PlayerEntity)(Object)this;
            LevelManager levelManager = ((LevelManagerAccess)player).getLevelManager();

            // Calculate average of combat skills
            int meleeLevel = levelManager.getSkillLevel(1);   // Melee
            int defenseLevel = levelManager.getSkillLevel(2); // Defense
            int magicLevel = levelManager.getSkillLevel(5);   // Magic

            int newOverallLevel = Math.round((meleeLevel + defenseLevel + magicLevel) / 3.0f);

            // Only update if changed
            if (newOverallLevel != levelManager.getOverallLevel()) {
                levelManager.setOverallLevel(newOverallLevel);

                // This is a server player, so we can send the update packet
                if (player instanceof ServerPlayerEntity) {
                    net.levelz.util.PacketHelper.updateLevels((ServerPlayerEntity)player);
                }
            }
        }
    }
}