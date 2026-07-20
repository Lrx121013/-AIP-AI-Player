package com.aip.story;

import java.util.UUID;

/**
 * v2.2.10 服务器 AI 叛变版故事状态
 * <p>
 * 每个 StoryState 绑定一个玩家 UUID，故事进度独立。
 * <p>
 * 关键字段：
 *   - tokenUndisposed         章节 5 后玩家背包里是否仍有未处理的"§7安全令牌"（true=触发隐藏坏结局 3）
 *   - playerOriginalOpStatus  备份玩家原始 OP 状态（章节 1 记录）
 *   - chosenEnding            章节 9 玩家点击的选择：null=未选，"10A"=投降，"10B"=反抗
 *   - chapterStartTime        当前章节开始时间
 *   - storyStartTime          整个故事的开始时间
 */
public class StoryState {

    private final UUID playerId;
    private StoryPhase currentPhase;
    private long chapterStartTime;
    private long storyStartTime;
    private boolean storyStarted;
    private boolean storyCompleted;
    /** 玩家是否仍持有"§7安全令牌"（true=章节 5 玩家没听警告 → 触发隐藏坏结局 3） */
    private boolean tokenUndisposed;
    /** 备份玩家原始 OP 状态（章节 1 备份，章节 6 deop，章节 9/10 还原） */
    private boolean playerOriginalOpStatus;
    /** 章节 9 玩家点击的选择：null=未选，"10A"=投降，"10B"=反抗 */
    private String chosenEnding;

    public StoryState(UUID playerId) {
        this.playerId = playerId;
        this.currentPhase = StoryPhase.CHAPTER_1_COBBLE_HOUSE;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.tokenUndisposed = false;
        this.playerOriginalOpStatus = false;
        this.chosenEnding = null;
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

    public boolean isTokenUndisposed() { return tokenUndisposed; }

    public void setTokenUndisposed(boolean b) { this.tokenUndisposed = b; }

    public boolean isPlayerOriginalOpStatus() { return playerOriginalOpStatus; }

    public void setPlayerOriginalOpStatus(boolean b) { this.playerOriginalOpStatus = b; }

    public String getChosenEnding() { return chosenEnding; }

    public void setChosenEnding(String s) { this.chosenEnding = s; }

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

    /** 重置故事（用于新玩家或退出后重玩） */
    public void reset() {
        this.currentPhase = StoryPhase.CHAPTER_1_COBBLE_HOUSE;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.tokenUndisposed = false;
        this.playerOriginalOpStatus = false;
        this.chosenEnding = null;
    }

    /** 状态摘要（用于 /aistory status） */
    public String getSummary() {
        return getCurrentPhase().getDisplayName();
    }

    // ============================================================
    // 兼容旧 API（AIPlayerManager 仍调用，新故事不依赖）
    // ============================================================

    /** 兼容：旧版觉醒 pending 字段（始终 false） */
    public boolean isAwakeningPending() { return false; }
    public void setAwakeningPending(boolean b) { /* no-op */ }
    public String getPendingKillerName() { return null; }
    public void setPendingKillerName(String s) { /* no-op */ }

    /** 兼容：旧版复活重绑（无操作） */
    public void reviveRebind(java.util.UUID newOwnerId) { /* no-op */ }

    /** 兼容：旧 sawSecondPaint（始终 false） */
    public boolean isSawSecondPaint() { return false; }
    public void setSawSecondPaint(boolean b) { /* no-op */ }

    /** 兼容：旧 gotCrystalKey（始终 false） */
    public boolean isGotCrystalKey() { return false; }
    public void setGotCrystalKey(boolean b) { /* no-op */ }

    /** 兼容：旧 flowerUndisposed（默认 false） */
    public boolean isFlowerUndisposed() { return tokenUndisposed; }
    public void setFlowerUndisposed(boolean b) { this.tokenUndisposed = b; }

    /** 兼容：旧 trustMrSparkle（始终 false） */
    public boolean isTrustMrSparkle() { return false; }
    public void setTrustMrSparkle(boolean b) { /* no-op */ }
}
