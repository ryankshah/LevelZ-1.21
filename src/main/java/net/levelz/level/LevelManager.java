package net.levelz.level;

import net.levelz.init.ConfigInit;
import net.levelz.level.restriction.PlayerRestriction;
import net.levelz.registry.EnchantmentRegistry;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class LevelManager {

    public static final Map<Integer, Skill> SKILLS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> BLOCK_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> CRAFTING_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> ENTITY_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> ITEM_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> MINING_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> ENCHANTMENT_RESTRICTIONS = new HashMap<>();
    public static final Map<String, SkillBonus> BONUSES = new HashMap<>();
    private Map<Integer, Float> skillProgressMap = new HashMap<>();

    private final PlayerEntity playerEntity;
    private Map<Integer, PlayerSkill> playerSkills = new HashMap<>();

    // Level
    private int overallLevel;
    private int totalLevelExperience;
    private float levelProgress;
    private int skillPoints;

    public LevelManager(PlayerEntity playerEntity) {
        this.playerEntity = playerEntity;

        for (Skill skill : SKILLS.values()) {
            if (!this.playerSkills.containsKey(skill.getId())) {
                this.playerSkills.put(skill.getId(), new PlayerSkill(skill.getId(), 0));
            } else if (this.playerSkills.get(skill.getId()).getLevel() > skill.getMaxLevel()) {
                this.playerSkills.get(skill.getId()).setLevel(skill.getMaxLevel());
            }
        }
    }

    public PlayerEntity getPlayerEntity() {
        return playerEntity;
    }

    public float getSkillProgress(int skillId) {
        return this.skillProgressMap.getOrDefault(skillId, 0.0F);
    }

    public void setSkillProgress(int skillId, float progress) {
        this.skillProgressMap.put(skillId, progress);
    }

    public void readNbt(NbtCompound nbt) {
        this.overallLevel = nbt.getInt("Level");
        this.levelProgress = nbt.getFloat("LevelProgress");
        this.totalLevelExperience = nbt.getInt("TotalLevelExperience");
        this.skillPoints = nbt.getInt("SkillPoints");

        NbtList skills = nbt.getList("Skills", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < skills.size(); i++) {
            PlayerSkill skill = new PlayerSkill(skills.getCompound(i));
            if (!SKILLS.containsKey(skill.getId())) {
                continue;
            }
            playerSkills.put(skill.getId(), skill);
        }

        // Add loading of skill progress
        if (nbt.contains("SkillProgress", NbtElement.COMPOUND_TYPE)) {
            NbtCompound skillProgressNbt = nbt.getCompound("SkillProgress");
            for (String key : skillProgressNbt.getKeys()) {
                int skillId = Integer.parseInt(key);
                float progress = skillProgressNbt.getFloat(key);
                this.skillProgressMap.put(skillId, progress);
            }
        } else {
            // Initialize progress map for all skills if not present
            for (int skillId : SKILLS.keySet()) {
                this.skillProgressMap.put(skillId, 0.0F);
            }
        }
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("Level", this.overallLevel);
        nbt.putFloat("LevelProgress", this.levelProgress);
        nbt.putInt("TotalLevelExperience", this.totalLevelExperience);
        nbt.putInt("SkillPoints", this.skillPoints);

        NbtList skills = new NbtList();
        for (PlayerSkill skill : this.playerSkills.values()) {
            skills.add(skill.writeDataToNbt());
        }
        nbt.put("Skills", skills);

        // Add saving of skill progress
        NbtCompound skillProgressNbt = new NbtCompound();
        for (Map.Entry<Integer, Float> entry : this.skillProgressMap.entrySet()) {
            skillProgressNbt.putFloat(String.valueOf(entry.getKey()), entry.getValue());
        }
        nbt.put("SkillProgress", skillProgressNbt);
    }

    public Map<Integer, PlayerSkill> getPlayerSkills() {
        return playerSkills;
    }

    public void setPlayerSkills(Map<Integer, PlayerSkill> playerSkills) {
        this.playerSkills = playerSkills;
    }

    public void setOverallLevel(int overallLevel) {
        this.overallLevel = overallLevel;
    }

    public int getOverallLevel() {
        return overallLevel;
    }

    public void setTotalLevelExperience(int totalLevelExperience) {
        this.totalLevelExperience = totalLevelExperience;
    }

    public int getTotalLevelExperience() {
        return totalLevelExperience;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setLevelProgress(float levelProgress) {
        this.levelProgress = levelProgress;
    }

    public float getLevelProgress() {
        return levelProgress;
    }

    public void setSkillLevel(int skillId, int level) {
        this.playerSkills.get(skillId).setLevel(level);
    }

    public int getSkillLevel(int skillId) {
        return this.playerSkills.get(skillId).getLevel();
    }

    /**
     * Add passive experience to a skill
     * @param skillId The ID of the skill to add experience to
     * @param amount The amount of experience to add
     * @return True if the skill leveled up
     */
    public boolean addSkillExperience(int skillId, int amount) {
        if (!this.playerSkills.containsKey(skillId)) {
            return false;
        }

        PlayerSkill skill = this.playerSkills.get(skillId);
        int currentLevel = skill.getLevel();

        // Calculate XP needed for next level
        int xpForNextLevel = getXpRequiredForLevel(currentLevel + 1);

        // Add experience and check for level up
        float progressBefore = this.getSkillProgress(skillId);
        skill.setCurrentXp(skill.getCurrentXp() + amount);
        float newProgress = (float) skill.getCurrentXp() / xpForNextLevel;
        this.setSkillProgress(skillId, newProgress);

        // Check for level up
        boolean leveledUp = false;
        if (newProgress >= 1.0f && currentLevel < SKILLS.get(skillId).getMaxLevel()) {
            // Level up the skill
            int remainingXp = skill.getCurrentXp() - xpForNextLevel;
            skill.setLevel(currentLevel + 1);
            skill.setCurrentXp(remainingXp);

            // Update progress for new level
            int nextLevelXp = getXpRequiredForLevel(skill.getLevel() + 1);
            float remainingProgress = (float) remainingXp / nextLevelXp;
            this.setSkillProgress(skillId, remainingProgress);

            // Update attributes and send notifications
            if (this.playerEntity instanceof ServerPlayerEntity serverPlayer) {
                LevelHelper.updateSkill(serverPlayer, SKILLS.get(skillId));
                PacketHelper.sendLevelUpNotification(serverPlayer, skillId);
            }

            leveledUp = true;

            // Check if more level ups are available with remaining XP
            if (remainingXp >= nextLevelXp) {
                addSkillExperience(skillId, 0); // Recalculate with existing XP
            }
        }

        // Send skill progress update
        if (this.playerEntity instanceof ServerPlayerEntity serverPlayer) {
            PacketHelper.updateSkillExperience(serverPlayer, skillId);
        }

        return leveledUp;
    }

    /**
     * Calculate XP required for a specific level using the formula from config
     */
    private int getXpRequiredForLevel(int level) {
        int experienceCost = (int) (ConfigInit.CONFIG.xpBaseCost +
                ConfigInit.CONFIG.xpCostMultiplicator *
                        Math.pow(level, ConfigInit.CONFIG.xpExponent));

        if (ConfigInit.CONFIG.xpMaxCost != 0) {
            return Math.min(experienceCost, ConfigInit.CONFIG.xpMaxCost);
        }

        return experienceCost;
    }

    public boolean isMaxLevel() {
        if (ConfigInit.CONFIG.overallMaxLevel > 0) {
            return this.overallLevel >= ConfigInit.CONFIG.overallMaxLevel;
        } else {
            int maxLevel = 0;
            for (Skill skill : SKILLS.values()) {
                maxLevel += skill.getMaxLevel();
            }
            return this.overallLevel >= maxLevel;
        }
    }

    public boolean hasAvailableLevel() {
        return this.skillPoints > 0;
    }

    // Recommend to use https://www.geogebra.org/graphing
    public int getNextLevelExperience() {
        if (isMaxLevel()) {
            return 0;
        }
        int experienceCost = (int) (ConfigInit.CONFIG.xpBaseCost + ConfigInit.CONFIG.xpCostMultiplicator * Math.pow(this.overallLevel, ConfigInit.CONFIG.xpExponent));
        if (ConfigInit.CONFIG.xpMaxCost != 0) {
            return experienceCost >= ConfigInit.CONFIG.xpMaxCost ? ConfigInit.CONFIG.xpMaxCost : experienceCost;
        } else {
            return experienceCost;
        }
    }

    // block
    public boolean hasRequiredBlockLevel(Block block) {
        int itemId = Registries.BLOCK.getRawId(block);
        if (BLOCK_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = BLOCK_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredBlockLevel(Block block) {
        int itemId = Registries.BLOCK.getRawId(block);
        if (BLOCK_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = BLOCK_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // crafting
    public boolean hasRequiredCraftingLevel(Item item) {
        int itemId = Registries.ITEM.getRawId(item);
        if (CRAFTING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = CRAFTING_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredCraftingLevel(Item item) {
        int itemId = Registries.ITEM.getRawId(item);
        if (CRAFTING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = CRAFTING_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // entity
    public boolean hasRequiredEntityLevel(EntityType<?> entityType) {
        int entityId = Registries.ENTITY_TYPE.getRawId(entityType);
        if (ENTITY_RESTRICTIONS.containsKey(entityId)) {
            PlayerRestriction playerRestriction = ENTITY_RESTRICTIONS.get(entityId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredEntityLevel(EntityType<?> entityType) {
        int entityId = Registries.ENTITY_TYPE.getRawId(entityType);
        if (ENTITY_RESTRICTIONS.containsKey(entityId)) {
            PlayerRestriction playerRestriction = ENTITY_RESTRICTIONS.get(entityId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // item
    public boolean hasRequiredItemLevel(Item item) {
        int itemId = Registries.ITEM.getRawId(item);
        if (ITEM_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = ITEM_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredItemLevel(Item item) {
        int itemId = Registries.ITEM.getRawId(item);
        if (ITEM_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = ITEM_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // mining
    public boolean hasRequiredMiningLevel(Block block) {
        int itemId = Registries.BLOCK.getRawId(block);
        if (MINING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = MINING_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredMiningLevel(Block block) {
        int itemId = Registries.BLOCK.getRawId(block);
        if (MINING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = MINING_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // enchantment
    public boolean hasRequiredEnchantmentLevel(RegistryEntry<Enchantment> enchantment, int level) {
        int enchantmentId = EnchantmentRegistry.getId(enchantment, level);
        if (ENCHANTMENT_RESTRICTIONS.containsKey(enchantmentId)) {
            PlayerRestriction playerRestriction = ENCHANTMENT_RESTRICTIONS.get(enchantmentId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredEnchantmentLevel(RegistryEntry<Enchantment> enchantment, int level) {
        int enchantmentId = EnchantmentRegistry.getId(enchantment, level);
        if (ENCHANTMENT_RESTRICTIONS.containsKey(enchantmentId)) {
            PlayerRestriction playerRestriction = ENCHANTMENT_RESTRICTIONS.get(enchantmentId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    /**
     * Reset a skill to level 0 and return the skill points
     */
    public boolean resetSkill(int skillId) {
        int level = this.getSkillLevel(skillId);
        if (level > 0) {
            this.setSkillPoints(this.getSkillPoints() + level);
            this.setSkillLevel(skillId, 0);

            // Reset skill progress
            this.playerSkills.get(skillId).setCurrentXp(0);
            this.setSkillProgress(skillId, 0);

            // Update client
            if (this.playerEntity instanceof ServerPlayerEntity serverPlayer) {
                PacketHelper.updatePlayerSkills(serverPlayer, null);
                LevelHelper.updateSkill(serverPlayer, SKILLS.get(skillId));
                PacketHelper.updateSkillExperience(serverPlayer, skillId);
            }

            return true;
        } else {
            return false;
        }
    }
}