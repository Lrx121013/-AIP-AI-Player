package com.aip.story;

import java.util.UUID;

/**
 * 故事模式状态数据类
 * <p>
 * 每个 AIPlayer 持有一个 StoryState 实例，记录其故事进度。
 * 由 {@link StoryManager} 中央管理。
 */
public class StoryState {

    private UUID ownerId;  // v2.1.4: 非 final 以支持 reviveRebind 重赋
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
    /** v2.2.1：觉醒切模式 deferred 标记。死亡时 NPC 已被删除，无法立即执行 force_survival / creative / fly，由 AIPlayerManager.revive 复活完成后消费 */
    private boolean awakeningPending;
    /** v2.2.1：觉醒时记录的 killer 名称，revive 时用于 force_survival_player 指令 */
    private String pendingKillerName;

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
        this.awakeningPending = false;
        this.pendingKillerName = null;
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
                // v2.2.1：F2.1 移除 System.out.println，由调用方 StoryManager 处理
            }
            return false;
        }
        transitionLocked = true;
        try {
            StoryPhase prev = currentPhase;
            this.currentPhase = next;
            this.phaseStartTime = System.currentTimeMillis();
            // v2.2.1：F2.1 移除 System.out.println，StoryManager 在 transitionTo 之后会 broadcast 剧情 X 觉醒
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
    public boolean isAwakeningPending() { return awakeningPending; }
    public void setAwakeningPending(boolean awakeningPending) { this.awakeningPending = awakeningPending; }
    public String getPendingKillerName() { return pendingKillerName; }
    public void setPendingKillerName(String pendingKillerName) { this.pendingKillerName = pendingKillerName; }

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
     * v2.1.4 复活重绑：仅更新 ownerId 和 phaseStartTime，保留所有剧情进度
     * <p>
     * AI 死后 NPC 实体 UUID 改变（re-spawn 时 Citizens 重新分配），但故事进度必须保留：
     *  - aiDeathCount / playerKillCount 累计
     *  - currentPhase / rulebookDelivered / rulebookRead
     *  - aerialBombsRemaining / dictatorshipOrdersGiven
     * <p>
     * 仅 phaseStartTime 重置为 now，让 3.5 分钟轰炸倒计时、30 秒命令倒计时等从复活后重新开始。
     */
    public void reviveRebind(UUID newEntityId) {
        if (newEntityId == null) return;
        this.ownerId = newEntityId;  // ownerId 不是 final 才可重新赋值
        this.phaseStartTime = System.currentTimeMillis();
        // 死亡瞬间的临时 transition lock 应清空（防旧任务残留）
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
