package net.levelz.init;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillEventHandlers {

    // Map to track player damage for skills
    private static final Map<UUID, Long> lastDamageReceived = new HashMap<>();
    private static final long DAMAGE_COOLDOWN_MS = 2000; // 2 seconds cooldown for damage XP

    public static void init() {
        registerMiningEvents();
        registerCombatEvents();
        registerFarmingEvents();
        registerItemUseEvents();
    }

    /**
     * Register mining & block-breaking related skill XP events
     */
    private static void registerMiningEvents() {
        // Add mining XP when breaking blocks
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient() || player.isCreative()) {
                return;
            }

            Block block = state.getBlock();
            SkillExperienceManager xpManager = SkillExperienceManager.getInstance();

            // Mining skill (ID 6) - Ores
            if (state.isIn(BlockTags.OVERWORLD_CARVER_REPLACEABLES) || state.isIn(ConventionalBlockTags.ORES) || block instanceof ExperienceDroppingBlock) {
                int baseXp = 5; // Base XP for mining ores
                xpManager.awardSkillXp(player, 6, baseXp, SkillXpType.MINING_ORE);
            }
            // Mining skill - Stone types (less XP)
            else if (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.STONE_ORE_REPLACEABLES)) {
                int baseXp = 1; // Less XP for regular stone
                xpManager.awardSkillXp(player, 6, baseXp, SkillXpType.MINING_STONE);
            }

            // Agility skill (ID 4) - Small XP for any block broken
            xpManager.awardSkillXp(player, 4, 1, SkillXpType.AGILITY_MOVEMENT);

            // TOOLS - Specific XP gain based on tool used
            ItemStack tool = player.getMainHandStack();

            // Woodcutting (using axes on logs) - Mining skill
            if (tool.getItem() instanceof AxeItem && state.isIn(BlockTags.LOGS)) {
                xpManager.awardSkillXp(player, 6, 2, SkillXpType.MINING_ORE);
            }
        });
    }

    /**
     * Register combat-related skill XP events
     */
    private static void registerCombatEvents() {
        // Award XP for attacking entities
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND && entity instanceof LivingEntity livingEntity) {
                // Process after a slight delay to capture damage
                world.getServer().execute(() -> {
                    ItemStack weapon = player.getMainHandStack();
                    SkillExperienceManager xpManager = SkillExperienceManager.getInstance();

                    // Calculate base XP based on entity health
                    int baseXp = Math.max(1, (int)(livingEntity.getMaxHealth() / 4));

                    // Melee skill (ID 1) XP for melee attacks
                    if (weapon.getItem() instanceof SwordItem ||
                            weapon.getItem() instanceof AxeItem ||
                            weapon.isEmpty()) {
                        xpManager.awardSkillXp(player, 1, baseXp, SkillXpType.MELEE_DAMAGE);
                    }

                    // Archery skill (ID 3) XP for ranged weapons
                    // Note: This is for preparing the weapon; actual hit is tracked in ProjectileEntityMixin
                    if (weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem) {
                        // We don't add XP here as it will be added when projectile hits
                    }
                });
            }
            return ActionResult.PASS;
        });

        // Tracking damage taken for Defense skill
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                UUID playerId = player.getUuid();

                // Check if player recently took damage
                if (player.hurtTime > 0 && player.hurtTime == player.maxHurtTime) {
                    long currentTime = System.currentTimeMillis();
                    Long lastDamage = lastDamageReceived.get(playerId);

                    // Ensure we don't award XP too frequently for damage
                    if (lastDamage == null || currentTime - lastDamage > DAMAGE_COOLDOWN_MS) {
                        lastDamageReceived.put(playerId, currentTime);

                        // Award Defense skill (ID 2) XP
                        int damageXp = Math.max(1, (int)(player.getRecentDamageSource().getExhaustion() / 2));
                        SkillExperienceManager.getInstance().awardSkillXp(
                                player, 2, damageXp, SkillXpType.DEFENSE_DAMAGE_TAKEN);
                    }
                }
            });
        });
    }

    /**
     * Register farming-related skill XP events
     */
    private static void registerFarmingEvents() {
        // Handle harvesting crops
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient() || player.isCreative()) {
                return;
            }

            SkillExperienceManager xpManager = SkillExperienceManager.getInstance();

            // Farming skill (ID 8) - Crop harvesting
            if (state.getBlock() instanceof CropBlock) {
                CropBlock cropBlock = (CropBlock) state.getBlock();
                if (cropBlock.isMature(state)) {
                    xpManager.awardSkillXp(player, 8, 3, SkillXpType.FARMING_HARVEST);
                }
            }

            // Using a hoe gives farming XP
            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() instanceof HoeItem) {
                xpManager.awardSkillXp(player, 8, 1, SkillXpType.FARMING_HARVEST);
            }
        });

        // Breeding handled in AnimalEntityMixin
    }

    /**
     * Register item use-related skill XP events
     */
    private static void registerItemUseEvents() {
        // Handle item use (tools, potions, etc.)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || player.isCreative()) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getStackInHand(hand);
            Item item = stack.getItem();
            SkillExperienceManager xpManager = SkillExperienceManager.getInstance();

            // Magic skill (ID 5)
            if (item instanceof PotionItem || item instanceof EnchantedBookItem) {
                xpManager.awardSkillXp(player, 5, 2, SkillXpType.MAGIC_BREWING);
            }

            // Archery skill (ID 3)
            if (item instanceof BowItem || item instanceof CrossbowItem) {
                xpManager.awardSkillXp(player, 3, 1, SkillXpType.ARCHERY_BOW_DAMAGE);
            }

            // Defense skill (ID 2) - Shield usage
            if (item instanceof ShieldItem) {
                xpManager.awardSkillXp(player, 2, 1, SkillXpType.DEFENSE_DAMAGE_TAKEN);
            }

            // Luck skill (ID 11) - Fishing
            if (item instanceof FishingRodItem) {
                xpManager.awardSkillXp(player, 11, 1, SkillXpType.LUCK_FISHING);
            }

            return TypedActionResult.pass(stack);
        });

        // Handle block interactions (furnaces, enchanting tables, etc.)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || player.isCreative()) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            SkillExperienceManager xpManager = SkillExperienceManager.getInstance();

            // Smithing skill (ID 7) - Using furnace/smithing table
            if (state.getBlock() instanceof FurnaceBlock) {
                xpManager.awardSkillXp(player, 7, 1, SkillXpType.SMITHING_FURNACE);
            }

            // Magic skill (ID 5) - Using enchanting table
            if (state.isOf(Blocks.ENCHANTING_TABLE)) {
                xpManager.awardSkillXp(player, 5, 1, SkillXpType.MAGIC_ENCHANTING);
            }

            // Smithing skill (ID 7) - Using anvil/smithing table
            if (state.isOf(Blocks.ANVIL) || state.isOf(Blocks.SMITHING_TABLE)) {
                xpManager.awardSkillXp(player, 7, 2, SkillXpType.SMITHING_ANVIL);
            }

            return ActionResult.PASS;
        });
    }
}