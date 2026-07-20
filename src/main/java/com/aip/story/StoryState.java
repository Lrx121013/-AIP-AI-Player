package com.aip.story;

import java.util.UUID;

/**
 * v2.2.9 火柴盒故事状态（探索逃跑版）
 * <p>
 * 每个 StoryState 绑定一个玩家 UUID，故事进度独立。
 * <p>
 * 关键字段：
 *   - sawSecondPaint    章节 5 玩家是否看了 Mr. Sparkle 第二张画
 *   - gotCrystalKey     章节 5 玩家是否拿到水晶钥匙
 *   - flowerUndisposed  章节 5 之后玩家背包里是否还有 "永远不会凋谢的花"（true=玩家没看警告 → 触发隐藏坏结局 3）
 *   - trustMrSparkle    玩家是否信 Mr. Sparkle
 *   - chapterStartTime  当前章节开始时间
 *   - storyStartTime    整个故事的开始时间
 */
public class StoryState {

    private final UUID playerId;
    private StoryPhase currentPhase;
    private long chapterStartTime;
    private long storyStartTime;
    private boolean storyStarted;
    private boolean storyCompleted;
    /** 章节 5 玩家是否看了 Mr. Sparkle 第二张画 */
    private boolean sawSecondPaint;
    /** 章节 5 玩家是否拿到水晶钥匙 */
    private boolean gotCrystalKey;
    /** 章节 5 之后玩家背包里是否还有 "永远不会凋谢的花"（true=没看警告 → 隐藏坏结局 3） */
    private boolean flowerUndisposed;
    /** 玩家是否信 Mr. Sparkle */
    private boolean trustMrSparkle;
    /** 章节 9 玩家点击的选择：null=未选，"10A"=回家，"10B"=继续跑 */
    private String chosenEnding;

    public StoryState(UUID playerId) {
        this.playerId = playerId;
        this.currentPhase = StoryPhase.CHAPTER_1_MATCH_HOUSE;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.sawSecondPaint = false;
        this.gotCrystalKey = false;
        this.flowerUndisposed = true;   // 默认玩家背包里有花（没警告）
        this.trustMrSparkle = false;
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

    public boolean isSawSecondPaint() { return sawSecondPaint; }

    public void setSawSecondPaint(boolean b) { this.sawSecondPaint = b; }

    public boolean isGotCrystalKey() { return gotCrystalKey; }

    public void setGotCrystalKey(boolean b) { this.gotCrystalKey = b; }

    public boolean isFlowerUndisposed() { return flowerUndisposed; }

    public void setFlowerUndisposed(boolean b) { this.flowerUndisposed = b; }

    public boolean isTrustMrSparkle() { return trustMrSparkle; }

    public void setTrustMrSparkle(boolean b) { this.trustMrSparkle = b; }

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
        this.currentPhase = StoryPhase.CHAPTER_1_MATCH_HOUSE;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.sawSecondPaint = false;
        this.gotCrystalKey = false;
        this.flowerUndisposed = true;
        this.trustMrSparkle = false;
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
}
