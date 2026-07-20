package com.aip.story;

import java.util.UUID;

/**
 * v2.2.7 火柴盒 AI 故事状态
 * <p>
 * 每个 StoryState 绑定一个玩家 UUID，故事进度独立。
 */
public class StoryState {

    private final UUID playerId;
    private StoryPhase currentPhase;
    private long chapterStartTime;
    private long storyStartTime;
    private boolean storyStarted;
    private boolean storyCompleted;
    /** Eve 的花是否仍在玩家背包里（true=玩家没听 Mr. Sparkle 警告），用于触发隐藏坏结局 3 */
    private boolean flowerUndisposed;
    /** 玩家原始 OP 状态备份（章节 6 会被 AI 夺取 OP） */
    private boolean playerOriginalOpStatus;
    /** 玩家是否在章节 9 点了 [投降]（决定走 10A） */
    private boolean choseSurrender;
    /** 隐藏坏结局 3 是否已触发（章节 5→6 时检测） */
    private boolean hiddenEndingPending;

    public StoryState(UUID playerId) {
        this.playerId = playerId;
        this.currentPhase = StoryPhase.CHAPTER_1_MATCH_HOUSE;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.flowerUndisposed = true;   // 默认玩家背包里有花（没警告）
        this.playerOriginalOpStatus = false;
        this.choseSurrender = false;
        this.hiddenEndingPending = false;
    }

    public UUID getPlayerId() { return playerId; }

    public StoryPhase getCurrentPhase() { return currentPhase; }

    public void setCurrentPhase(StoryPhase p) {
        this.currentPhase = p;
        this.chapterStartTime = System.currentTimeMillis();
    }

    public long getChapterStartTime() { return chapterStartTime; }

    public long getStoryStartTime() { return storyStartTime; }

    public void setStoryStartTime(long t) { this.storyStartTime = t; }

    public boolean isStoryStarted() { return storyStarted; }

    public void setStoryStarted(boolean b) { this.storyStarted = b; }

    public boolean isStoryCompleted() { return storyCompleted; }

    public void setStoryCompleted(boolean b) { this.storyCompleted = b; }

    public boolean isFlowerUndisposed() { return flowerUndisposed; }

    public void setFlowerUndisposed(boolean b) { this.flowerUndisposed = b; }

    public boolean isPlayerOriginalOpStatus() { return playerOriginalOpStatus; }

    public void setPlayerOriginalOpStatus(boolean b) { this.playerOriginalOpStatus = b; }

    public boolean isChoseSurrender() { return choseSurrender; }

    public void setChoseSurrender(boolean b) { this.choseSurrender = b; }

    public boolean isHiddenEndingPending() { return hiddenEndingPending; }

    public void setHiddenEndingPending(boolean b) { this.hiddenEndingPending = b; }

    /** 当前章节已过时长（秒） */
    public int getElapsedSeconds() {
        if (chapterStartTime == 0L) return 0;
        return (int) ((System.currentTimeMillis() - chapterStartTime) / 1000L);
    }

    /** 当前章节是否到时 */
    public boolean isChapterTimeout() {
        if (chapterStartTime == 0L) return false;
        return getElapsedSeconds() >= currentPhase.getDurationSeconds();
    }

    /** 当前章节剩余时间（秒），最小 0 */
    public int getRemainingSeconds() {
        if (chapterStartTime == 0L) return currentPhase.getDurationSeconds();
        int remain = currentPhase.getDurationSeconds() - getElapsedSeconds();
        return Math.max(0, remain);
    }

    /** 重置故事（用于新玩家） */
    public void reset() {
        this.currentPhase = StoryPhase.CHAPTER_1_MATCH_HOUSE;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.flowerUndisposed = true;
        this.playerOriginalOpStatus = false;
        this.choseSurrender = false;
        this.hiddenEndingPending = false;
    }

    // ============================================================
    // v2.2.7 兼容：旧版故事模式字段/方法（已废弃，新故事不再使用）
    // ============================================================

    /** v2.2.7 兼容：旧版 AI 死亡计数（返回 0，新故事不依赖） */
    public int getAiDeathCount() { return 0; }
    public void setAiDeathCount(int n) { /* no-op */ }
    public void incrementAiDeathCount() { /* no-op */ }

    /** v2.2.7 兼容：旧版制度之书 */
    public boolean isRulebookDelivered() { return false; }
    public void setRulebookDelivered(boolean b) { /* no-op */ }
    public boolean isRulebookRead() { return false; }
    public void setRulebookRead(boolean b) { /* no-op */ }

    /** v2.2.7 兼容：旧版空中轰炸计数 */
    public int getAerialBombsRemaining() { return 0; }
    public void setAerialBombsRemaining(int n) { /* no-op */ }

    /** v2.2.7 兼容：旧版独裁命令计数 */
    public int getDictatorshipOrdersGiven() { return 0; }
    public void setDictatorshipOrdersGiven(int n) { /* no-op */ }

    /** v2.2.7 兼容：旧版阶段转移（已废弃） */
    public boolean transitionTo(StoryPhase target) { return false; }

    /** v2.2.7 兼容：状态摘要 */
    public String getSummary() {
        return getCurrentPhase().getDisplayName();
    }

    /** v2.2.7 兼容：旧版觉醒 deferred 字段 */
    public boolean isAwakeningPending() { return false; }
    public void setAwakeningPending(boolean b) { /* no-op */ }
    public String getPendingKillerName() { return null; }
    public void setPendingKillerName(String s) { /* no-op */ }

    /** v2.2.7 兼容：旧版复活重绑 */
    public void reviveRebind(java.util.UUID newOwnerId) { /* no-op */ }

    /** v2.2.7 兼容：旧版 chosenEnding */
    public String getChosenEnding() { return null; }
    public void setChosenEnding(String s) { /* no-op */ }
}
