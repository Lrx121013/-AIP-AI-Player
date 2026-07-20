package com.aip.story;

import java.util.UUID;

/**
 * v2.2.7：火柴盒故事状态
 * <p>
 * 每个 StoryState 绑定一个玩家 UUID。
 * 阶段切换不依赖 AI 死亡事件，而是由 StoryManager.tickChapter 周期推进。
 */
public class StoryState {
    private final UUID playerId;
    private StoryPhase currentPhase;
    private long chapterStartTime;          // 当前章节开始时间（毫秒）
    private long storyStartTime;            // 整个故事开始时间
    private boolean storyStarted;
    private boolean storyCompleted;
    /** v2.2.7：玩家是否看了 Mr. Sparkle 给的第二张画 */
    private boolean sawMrSparkleWarning;
    /** v2.2.7：玩家是否接受 Eve 的花（默认 true，没看警告就接受） */
    private boolean acceptedEveFlower;
    /** v2.2.7：玩家在 Chapter 9 选择的结局（10A 或 10B） */
    private String chosenEnding;

    public StoryState(UUID playerId) {
        this.playerId = playerId;
        this.currentPhase = StoryPhase.CHAPTER_1_MATCH_HOUSE;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.sawMrSparkleWarning = false;
        this.acceptedEveFlower = false;
        this.chosenEnding = null;
    }

    public UUID getPlayerId() { return playerId; }
    public StoryPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(StoryPhase p) { this.currentPhase = p; this.chapterStartTime = System.currentTimeMillis(); }
    public long getChapterStartTime() { return chapterStartTime; }
    public long getStoryStartTime() { return storyStartTime; }
    public void setStoryStartTime(long t) { this.storyStartTime = t; }
    public boolean isStoryStarted() { return storyStarted; }
    public void setStoryStarted(boolean b) { this.storyStarted = b; }
    public boolean isStoryCompleted() { return storyCompleted; }
    public void setStoryCompleted(boolean b) { this.storyCompleted = b; }
    public boolean isSawMrSparkleWarning() { return sawMrSparkleWarning; }
    public void setSawMrSparkleWarning(boolean b) { this.sawMrSparkleWarning = b; }
    public boolean isAcceptedEveFlower() { return acceptedEveFlower; }
    public void setAcceptedEveFlower(boolean b) { this.acceptedEveFlower = b; }
    public String getChosenEnding() { return chosenEnding; }
    public void setChosenEnding(String s) { this.chosenEnding = s; }

    /** v2.2.7：当前章节已过时长（秒） */
    public int getElapsedSeconds() {
        if (chapterStartTime == 0L) return 0;
        return (int) ((System.currentTimeMillis() - chapterStartTime) / 1000);
    }

    /** v2.2.7：当前章节是否到时 */
    public boolean isChapterTimeout() {
        if (chapterStartTime == 0L) return false;
        return getElapsedSeconds() >= currentPhase.getDurationSeconds();
    }

    /** v2.2.7：重置故事（用于新玩家） */
    public void reset() {
        this.currentPhase = StoryPhase.CHAPTER_1_MATCH_HOUSE;
        this.storyStarted = false;
        this.storyCompleted = false;
        this.chapterStartTime = 0L;
        this.storyStartTime = 0L;
        this.sawMrSparkleWarning = false;
        this.acceptedEveFlower = false;
        this.chosenEnding = null;
    }
}
