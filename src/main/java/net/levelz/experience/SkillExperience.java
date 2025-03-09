package net.levelz.experience;

import net.levelz.access.LevelManagerAccess;
import net.levelz.init.ConfigInit;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.levelz.util.PacketHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles passive skill experience gain for various player actions
 */
public class SkillExperience {

    /**
     * Main method to add experience to a specific skill
     *
     * @param player   The player receiving experience
     * @param skillId  The skill ID to gain experience in
     * @param amount   The amount of experience to add
     */
    public static void addSkillExperience(PlayerEntity player, int skillId, int amount) {
        if (player.getWorld().isClient()) {
            return;
        }

        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        // Skip if skill doesn't exist or player is at max level
        if (!LevelManager.SKILLS.containsKey(skillId) ||
                levelManager.getSkillLevel(skillId) >= LevelManager.SKILLS.get(skillId).getMaxLevel()) {
            return;
        }

        Skill skill = LevelManager.SKILLS.get(skillId);

        // Apply multipliers based on configuration
        float multiplier = getSkillMultiplier(skillId);
        int adjustedAmount = Math.round(amount * multiplier);

        // Apply level-based multiplier if enabled
        if (ConfigInit.CONFIG.dropXPbasedOnLvl) {
            adjustedAmount = Math.round(adjustedAmount *
                    (1.0F + ConfigInit.CONFIG.basedOnMultiplier * levelManager.getSkillLevel(skillId)));
        }

        // Store current values
        int currentSkillLevel = levelManager.getSkillLevel(skillId);
        float currentProgress = levelManager.getSkillProgress(skillId);
        int requiredExp = getExperienceForNextLevel(skillId, currentSkillLevel);

        // Add experience to skill
        float newProgress = currentProgress + ((float) adjustedAmount / requiredExp);
        levelManager.setSkillProgress(skillId, newProgress);

        // Check for level ups
        checkForLevelUp(serverPlayer, skill, currentSkillLevel);

        // Update combat skill average for overall level
        if (isAveragedCombatSkill(skillId)) {
            updateOverallLevel(serverPlayer);
        }

        // Send updates to client
        PacketHelper.updateSkillExperience(serverPlayer, skillId);
    }

    /**
     * Checks if the player has leveled up and handles level up logic
     */
    private static void checkForLevelUp(ServerPlayerEntity player, Skill skill, int currentLevel) {
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
        float progress = levelManager.getSkillProgress(skill.getId());

        while (progress >= 1.0F && currentLevel < skill.getMaxLevel()) {
            // Level up the skill
            levelManager.setSkillLevel(skill.getId(), currentLevel + 1);
            currentLevel++;

            // Update attributes for this skill
            net.levelz.util.LevelHelper.updateSkill(player, skill);

            // Update progress bar
            progress -= 1.0F;
            levelManager.setSkillProgress(skill.getId(), progress);

            // Send level up notification and play sound
            PacketHelper.sendLevelUpNotification(player, skill.getId());
        }
    }

    /**
     * Updates the overall level based on the average of combat skills
     */
    private static void updateOverallLevel(ServerPlayerEntity player) {
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

        // Calculate average of melee, defense, and magic skills
        int meleeLevel = levelManager.getSkillLevel(1); // Melee
        int defenseLevel = levelManager.getSkillLevel(2); // Defense
        int magicLevel = levelManager.getSkillLevel(5); // Magic

        int averageLevel = Math.round((meleeLevel + defenseLevel + magicLevel) / 3.0f);

        // Only update if changed
        if (averageLevel != levelManager.getOverallLevel()) {
            levelManager.setOverallLevel(averageLevel);
            PacketHelper.updateLevels(player);
        }
    }

    /**
     * Determines if a skill is used for overall level calculation
     */
    private static boolean isAveragedCombatSkill(int skillId) {
        return skillId == 1 || skillId == 2 || skillId == 5; // Melee, Defense, Magic
    }

    /**
     * Returns the experience multiplier for a specific skill
     */
    private static float getSkillMultiplier(int skillId) {
        switch (skillId) {
            case 0: // Constitution
                return 1.0f;
            case 1: // Melee
                return ConfigInit.CONFIG.mobXPMultiplier;
            case 2: // Defense
                return 0.8f;
            case 3: // Archery
                return 0.9f;
            case 4: // Agility
                return 0.7f;
            case 5: // Magic
                return 0.75f;
            case 6: // Mining
                return ConfigInit.CONFIG.oreXPMultiplier;
            case 7: // Smithing
                return ConfigInit.CONFIG.furnaceXPMultiplier;
            case 8: // Farming
                return ConfigInit.CONFIG.breedingXPMultiplier;
            case 9: // Cooking
                return 0.8f;
            case 10: // Bartering
                return ConfigInit.CONFIG.tradingXPMultiplier;
            case 11: // Luck
                return 0.5f;
            default:
                return 1.0f;
        }
    }

    /**
     * Calculates the experience required for the next level
     */
    private static int getExperienceForNextLevel(int skillId, int currentLevel) {
        // Base formula from original LevelZ system
        int experienceCost = (int) (ConfigInit.CONFIG.xpBaseCost +
                ConfigInit.CONFIG.xpCostMultiplicator *
                        Math.pow(currentLevel, ConfigInit.CONFIG.xpExponent));

        // Apply max cap if configured
        if (ConfigInit.CONFIG.xpMaxCost != 0) {
            return Math.min(experienceCost, ConfigInit.CONFIG.xpMaxCost);
        }

        return experienceCost;
    }
}