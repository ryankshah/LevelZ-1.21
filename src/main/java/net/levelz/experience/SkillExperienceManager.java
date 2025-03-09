package net.levelz.experience;

import net.levelz.access.LevelManagerAccess;
import net.levelz.access.ServerPlayerSyncAccess;
import net.levelz.init.ConfigInit;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the passive XP gain for skills based on player actions
 */
public class SkillExperienceManager {

    // Singleton instance
    private static final SkillExperienceManager INSTANCE = new SkillExperienceManager();

    // Map to track XP types and their multipliers
    private final Map<SkillXpType, Float> xpMultipliers = new HashMap<>();

    private SkillExperienceManager() {
        // Initialize XP multipliers from config
        initializeXpMultipliers();
    }

    public static SkillExperienceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize XP multipliers from the mod config
     */
    private void initializeXpMultipliers() {
        // Mining related XP types
        xpMultipliers.put(SkillXpType.MINING_ORE, 1.5f);
        xpMultipliers.put(SkillXpType.MINING_STONE, 0.3f);

        // Smithing related XP types
        xpMultipliers.put(SkillXpType.SMITHING_ANVIL, 1.0f);
        xpMultipliers.put(SkillXpType.SMITHING_CRAFT, 0.8f);
        xpMultipliers.put(SkillXpType.SMITHING_FURNACE, 0.5f);

        // Combat related XP types
        xpMultipliers.put(SkillXpType.MELEE_DAMAGE, 0.5f);
        xpMultipliers.put(SkillXpType.RANGED_DAMAGE, 0.6f);
        xpMultipliers.put(SkillXpType.DEFENSE_DAMAGE_TAKEN, 0.4f);

        // Farming related XP types
        xpMultipliers.put(SkillXpType.FARMING_HARVEST, 0.8f);
        xpMultipliers.put(SkillXpType.FARMING_BREEDING, 1.0f);

        // Apply config multipliers
        applyConfigMultipliers();
    }

    /**
     * Apply multipliers from config
     */
    private void applyConfigMultipliers() {
        // Update multipliers from config values
        xpMultipliers.put(SkillXpType.BREEDING, ConfigInit.CONFIG.breedingXPMultiplier);
        xpMultipliers.put(SkillXpType.BOTTLE_XP, ConfigInit.CONFIG.bottleXPMultiplier);
        xpMultipliers.put(SkillXpType.DRAGON_XP, ConfigInit.CONFIG.dragonXPMultiplier);
        xpMultipliers.put(SkillXpType.FISHING_XP, ConfigInit.CONFIG.fishingXPMultiplier);
        xpMultipliers.put(SkillXpType.FURNACE_XP, ConfigInit.CONFIG.furnaceXPMultiplier);
        xpMultipliers.put(SkillXpType.ORE_XP, ConfigInit.CONFIG.oreXPMultiplier);
        xpMultipliers.put(SkillXpType.TRADING_XP, ConfigInit.CONFIG.tradingXPMultiplier);
        xpMultipliers.put(SkillXpType.MOB_XP, ConfigInit.CONFIG.mobXPMultiplier);
    }

    /**
     * Awards XP to a specific skill for the player
     *
     * @param player The player to award XP to
     * @param skillId The ID of the skill to award XP to
     * @param amount Base amount of XP to award
     * @param xpType The type of XP (for multiplier application)
     * @return True if the skill leveled up, false otherwise
     */
    public boolean awardSkillXp(PlayerEntity player, int skillId, int amount, SkillXpType xpType) {
        if (player.getWorld().isClient()) {
            return false;
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

        // Skip if skill doesn't exist or player is at max level
        if (!LevelManager.SKILLS.containsKey(skillId) ||
                levelManager.getSkillLevel(skillId) >= LevelManager.SKILLS.get(skillId).getMaxLevel()) {
            return false;
        }

        // Apply XP multiplier
        float multiplier = xpMultipliers.getOrDefault(xpType, 1.0f);
        int modifiedAmount = Math.round(amount * multiplier);

        // Apply level-based multiplier if configured
        if (ConfigInit.CONFIG.dropXPbasedOnLvl) {
            modifiedAmount = Math.round(modifiedAmount *
                    (1.0F + ConfigInit.CONFIG.basedOnMultiplier * levelManager.getOverallLevel()));
        }

        // Use the ServerPlayerSyncAccess interface to directly add experience
        return ((ServerPlayerSyncAccess) serverPlayer).addSkillExperience(skillId, modifiedAmount);
    }

    /**
     * Calculate XP required for a specific level using the mod's formula
     *
     * @param level The level to calculate XP for
     * @return The amount of XP required for the specified level
     */
    private int calculateXpForLevel(int level) {
        int experienceCost = (int) (ConfigInit.CONFIG.xpBaseCost +
                ConfigInit.CONFIG.xpCostMultiplicator *
                        Math.pow(level, ConfigInit.CONFIG.xpExponent));

        if (ConfigInit.CONFIG.xpMaxCost != 0) {
            return Math.min(experienceCost, ConfigInit.CONFIG.xpMaxCost);
        } else {
            return experienceCost;
        }
    }

    /**
     * Static helper method to award skill XP from external code
     *
     * @param player The player to award XP to
     * @param skillId The skill ID
     * @param amount The amount of XP
     * @param xpType The type of XP action
     * @return True if the skill leveled up
     */
    public static boolean awardXp(PlayerEntity player, int skillId, int amount, SkillXpType xpType) {
        return getInstance().awardSkillXp(player, skillId, amount, xpType);
    }

    /**
     * Enum representing different types of XP actions
     */
    public enum SkillXpType {
        // General XP types
        BREEDING,
        BOTTLE_XP,
        DRAGON_XP,
        FISHING_XP,
        FURNACE_XP,
        ORE_XP,
        TRADING_XP,
        MOB_XP,

        // Mining specific
        MINING_ORE,
        MINING_STONE,

        // Smithing specific
        SMITHING_ANVIL,
        SMITHING_CRAFT,
        SMITHING_FURNACE,

        // Combat specific
        MELEE_DAMAGE,
        RANGED_DAMAGE,
        DEFENSE_DAMAGE_TAKEN,

        // Farming specific
        FARMING_HARVEST,
        FARMING_BREEDING,

        // Archery specific
        ARCHERY_BOW_DAMAGE,
        ARCHERY_CROSSBOW_DAMAGE,

        // Magic specific
        MAGIC_ENCHANTING,
        MAGIC_BREWING,

        // Cooking specific
        COOKING_FOOD,

        // Bartering specific
        BARTERING_TRADE,

        // Agility specific
        AGILITY_MOVEMENT,

        // Luck specific
        LUCK_FISHING,
        LUCK_LOOT
    }
}