package com.aip.ai;

import java.util.UUID;

/**
 * AI 长期目标
 * <p>
 * 用于让 AI 有"想占领服务器"的长期战略意识。
 * 目标会注入到 system prompt，并在自主决策时作为引导。
 */
public class Goal {
    private final String id;
    private final String description;
    private final int priority; // 1-10，10 最高
    private int progress; // 0-100
    private boolean completed;
    private final long createdAt;

    public Goal(String description, int priority) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.description = description;
        this.priority = Math.max(1, Math.min(10, priority));
        this.progress = 0;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }

    public String getDescription() { return description; }

    public int getPriority() { return priority; }

    public int getProgress() { return progress; }

    /** 设置进度，自动 clamp 到 0-100，达到 100 自动标记为完成 */
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        if (this.progress >= 100) {
            this.completed = true;
        }
    }

    public boolean isCompleted() { return completed; }

    /** 标记目标完成 */
    public void complete() {
        this.completed = true;
        this.progress = 100;
    }

    public long getCreatedAt() { return createdAt; }
}
