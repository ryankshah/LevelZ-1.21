package net.levelz.init;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.levelz.access.LevelManagerAccess;
import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.levelz.mixin.entity.EntityAccessor;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

public class EventInit {

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            for (Skill skill : LevelManager.SKILLS.values()) {
                LevelHelper.updateSkill(handler.getPlayer(), skill);
            }
            PacketHelper.syncEnchantments(handler.getPlayer());
            PacketHelper.updateSkills(handler.getPlayer());
            PacketHelper.updatePlayerSkills(handler.getPlayer(), null);
            PacketHelper.updateRestrictions(handler.getPlayer());
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            PacketHelper.updatePlayerSkills(player, null);
            PacketHelper.updateLevels(player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                PacketHelper.updatePlayerSkills(newPlayer, oldPlayer);
                PacketHelper.updateLevels(newPlayer);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (ConfigInit.CONFIG.hardMode) {
                newPlayer.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_GAME_MODE, newPlayer));
                newPlayer.getScoreboard().forEachScore(CriteriaInit.LEVELZ, newPlayer, ScoreAccess::resetScore);
            } else {
                PacketHelper.updatePlayerSkills(newPlayer, oldPlayer);

                if (ConfigInit.CONFIG.resetCurrentXp) {
                    LevelManager levelManager = ((LevelManagerAccess) newPlayer).getLevelManager();
                    levelManager.setLevelProgress(0);
                    levelManager.setTotalLevelExperience(0);
                }

                PacketHelper.updateLevels(newPlayer);
                for (Skill skill : LevelManager.SKILLS.values()) {
                    LevelHelper.updateSkill(newPlayer, skill);
                }
            }
        });

        // Add passive XP for item use
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() && !player.isCreative()) {
                LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                ItemStack stack = player.getStackInHand(hand);

                if (!levelManager.hasRequiredItemLevel(stack.getItem())) {
                    player.sendMessage(Text.translatable("restriction.levelz.locked.tooltip").formatted(Formatting.RED), true);
                    return TypedActionResult.fail(stack);
                }

                // Add small amount of XP for using items
                if (stack.get(DataComponentTypes.FOOD) != null) {
                    // Cooking skill XP (ID 9)
                    SkillExperienceManager.getInstance().awardSkillXp(
                            player, 9, 1, SkillXpType.COOKING_FOOD);
                }
            }
            return TypedActionResult.pass(ItemStack.EMPTY);
        });

        // Add passive XP for block interactions
        UseBlockCallback.EVENT.register((player, world, hand, result) -> {
            if (!player.isCreative() && !player.isSpectator()) {
                BlockPos blockPos = result.getBlockPos();
                if (world.canPlayerModifyAt(player, blockPos)) {
                    LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                    if (!levelManager.hasRequiredBlockLevel(world.getBlockState(blockPos).getBlock())) {
                        player.sendMessage(Text.translatable("restriction.levelz.locked.tooltip").formatted(Formatting.RED), true);
                        return ActionResult.success(false);
                    }
                }
            }
            return ActionResult.PASS;
        });

        // Add passive XP for entity interactions
        UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> {
            if (!player.isCreative() && !player.isSpectator()) {
                if (!entity.hasControllingPassenger() || !((EntityAccessor) entity).callCanAddPassenger(player)) {
                    LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                    if (!levelManager.hasRequiredEntityLevel(entity.getType())) {
                        player.sendMessage(Text.translatable("restriction.levelz.locked.tooltip").formatted(Formatting.RED), true);
                        return ActionResult.success(false);
                    }

                    // Add XP for certain entity interactions (only on server)
                    if (!world.isClient()) {
                        // If interacting with hostile entity, add small amount of defense XP
                        if (entity instanceof HostileEntity) {
                            SkillExperienceManager.getInstance().awardSkillXp(
                                    player, 2, 1, SkillXpType.DEFENSE_DAMAGE_TAKEN);
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });

        // Add attack XP event
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // This will be handled by our mixins for more detailed damage tracking
            return ActionResult.PASS;
        });

        // Register block break events for mining XP
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // This will be handled by our SkillEventHandlers
        });
    }
}