package net.levelz.access;

public interface ServerPlayerSyncAccess {

    /**
     * Add overall level experience to the player
     * @param experience The amount of overall level experience to add
     */
    void addLevelExperience(int experience);

    /**
     * Add skill-specific experience to the player
     * @param skillId The skill ID to add experience to
     * @param amount The amount of experience to add
     * @return True if the skill leveled up
     */
    boolean addSkillExperience(int skillId, int amount);
}
