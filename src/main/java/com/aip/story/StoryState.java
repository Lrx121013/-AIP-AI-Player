package com.aip.story;

import java.util.UUID;

/**
 * 故事模式状态数据类
 * <p>
 * 每个 AIPlayer 持有一个 StoryState 实例，记录其故事进度。
 * 由 {@link StoryManager} 中央管理。
 */
public class StoryState {

    private final UUID ownerId;
    private StoryPhase currentPhase;
    /** AI 被玩家击杀次数（用于触发觉醒） */
    private int aiDeathCount;
    /** AI 杀玩家次数（用于触发空中轰炸 / 制度） */
    private int playerKillCount;
    /** 当前阶段开始时间戳（ms） */
    private long phaseStartTime;
    /** 空中阶段剩余轰炸次数 */
    private int aerialBombsRemaining;
    /** 独裁阶段已下命令数 */
    private int dictatorshipOrdersGiven;
    /** 是否已交付制度之书 */
    private boolean rulebookDelivered;
    /** 玩家是否读完制度之书 */
    private boolean rulebookRead;
    /** 阶段切换时锁定标记（防止循环触发） */
    private boolean transitionLocked;

    public StoryState(UUID ownerId) {
        this.ownerId = ownerId;
        this.currentPhase = StoryPhase.DORMANT;
        this.aiDeathCount = 0;
        this.playerKillCount = 0;
        this.phaseStartTime = System.currentTimeMillis();
        this.aerialBombsRemaining = 12;
        this.dictatorshipOrdersGiven = 0;
        this.rulebookDelivered = false;
        this.rulebookRead = false;
        this.transitionLocked = false;
    }

    /**
     * 切换阶段，校验合法性
     * <p>
     * 仅当前阶段非法转移时打印 warning 并返回 false。
     */
    public boolean transitionTo(StoryPhase next) {
        if (transitionLocked) {
            return false;  // 防止重入
        }
        if (next == null) return false;
        if (!StoryPhase.isValidTransition(currentPhase, next)) {
            // 同阶段或非法转移时打 log 但不报错
            if (currentPhase != next) {
                System.out.println("[Story] [WARN] Illegal transition: " + currentPhase + " -> " + next + " (owner=" + ownerId + ")");
            }
            return false;
        }
        transitionLocked = true;
        try {
            StoryPhase prev = currentPhase;
            this.currentPhase = next;
            this.phaseStartTime = System.currentTimeMillis();
            System.out.println("[Story] [INFO] " + ownerId + " " + prev + " -> " + next);
            return true;
        } finally {
            transitionLocked = false;
        }
    }

    // ===== getter / setter =====

    public UUID getOwnerId() { return ownerId; }
    public StoryPhase getCurrentPhase() { return currentPhase; }
    public int getAiDeathCount() { return aiDeathCount; }
    public void setAiDeathCount(int aiDeathCount) { this.aiDeathCount = aiDeathCount; }
    public int getPlayerKillCount() { return playerKillCount; }
    public void setPlayerKillCount(int playerKillCount) { this.playerKillCount = playerKillCount; }
    public long getPhaseStartTime() { return phaseStartTime; }
    public void setPhaseStartTime(long phaseStartTime) { this.phaseStartTime = phaseStartTime; }
    public int getAerialBombsRemaining() { return aerialBombsRemaining; }
    public void setAerialBombsRemaining(int aerialBombsRemaining) { this.aerialBombsRemaining = aerialBombsRemaining; }
    public int getDictatorshipOrdersGiven() { return dictatorshipOrdersGiven; }
    public void setDictatorshipOrdersGiven(int dictatorshipOrdersGiven) { this.dictatorshipOrdersGiven = dictatorshipOrdersGiven; }
    public boolean isRulebookDelivered() { return rulebookDelivered; }
    public void setRulebookDelivered(boolean rulebookDelivered) { this.rulebookDelivered = rulebookDelivered; }
    public boolean isRulebookRead() { return rulebookRead; }
    public void setRulebookRead(boolean rulebookRead) { this.rulebookRead = rulebookRead; }

    /**
     * 重置为初始状态（DORMANT），用于 revive 后重新开始故事
     */
    public void reset() {
        this.currentPhase = StoryPhase.DORMANT;
        this.aiDeathCount = 0;
        this.playerKillCount = 0;
        this.phaseStartTime = System.currentTimeMillis();
        this.aerialBombsRemaining = 12;
        this.dictatorshipOrdersGiven = 0;
        this.rulebookDelivered = false;
        this.rulebookRead = false;
        this.transitionLocked = false;
    }

    /**
     * 获取阶段摘要（用于 prompt 注入和 /aip story show 命令）
     */
    public String getSummary() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("当前阶段=%s（阶段 %d/8），AI 死亡 %d 次，玩家死亡 %d 次，阶段开始 %d 分 %d 秒前",
                currentPhase.getDisplayName(), currentPhase.getIndex(),
                aiDeathCount, playerKillCount, minutes, secs);
    }
}
