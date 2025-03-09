package net.levelz.level;

import net.minecraft.nbt.NbtCompound;

/**
 * Represents a player's skill with its current level and stored experience
 */
public class PlayerSkill {

    private final int id;
    private int level;
    private int currentXp;
    private float progress; // 0.0 to 1.0, represents visual progress to next level

    public PlayerSkill(int id, int level) {
        this.id = id;
        this.level = level;
        this.currentXp = 0;
        this.progress = 0.0f;
    }

    public PlayerSkill(NbtCompound nbt) {
        this.id = nbt.getInt("Id");
        this.level = nbt.getInt("Level");
        this.currentXp = nbt.getInt("CurrentXp");
        this.progress = nbt.getFloat("Progress");
    }

    public NbtCompound writeDataToNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Id", this.id);
        nbt.putInt("Level", this.level);
        nbt.putInt("CurrentXp", this.currentXp);
        nbt.putFloat("Progress", this.progress);
        return nbt;
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(int currentXp) {
        this.currentXp = currentXp;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void increaseLevel(int level) {
        int maxLevel = LevelManager.SKILLS.get(this.id).getMaxLevel();
        if ((this.level + level) <= maxLevel) {
            this.level += level;
        } else {
            this.level = maxLevel;
        }
    }

    public void decreaseLevel(int level) {
        if ((this.level - level) >= 0) {
            this.level -= level;
        } else {
            this.level = 0;
        }
    }

    /**
     * Add experience to this skill
     * @param amount The amount of XP to add
     * @param xpForNextLevel The XP required for the next level
     * @return True if the skill has enough XP to level up
     */
    public boolean addExperience(int amount, int xpForNextLevel) {
        this.currentXp += amount;
        this.progress = (float) this.currentXp / xpForNextLevel;

        return this.currentXp >= xpForNextLevel;
    }

    /**
     * Reset experience after leveling up
     * @param remainingXp Any overflow XP to keep for the next level
     */
    public void levelUp(int remainingXp) {
        this.level++;
        this.currentXp = remainingXp;
    }
}