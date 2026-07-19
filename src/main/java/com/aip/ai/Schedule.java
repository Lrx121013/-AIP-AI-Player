package com.aip.ai;

/**
 * AI 日程项
 * <p>
 * 表示一个时间段（用世界 ticks 表示，0-23999）和要执行的动作（[COMMAND:...]）。
 * 支持跨夜时间段（如 22:00 - 6:00）。
 */
public class Schedule {

    private final long startTicks;
    private final long endTicks;
    private final String action;

    public Schedule(long startTicks, long endTicks, String action) {
        this.startTicks = startTicks;
        this.endTicks = endTicks;
        this.action = action;
    }

    public long getStartTicks() {
        return startTicks;
    }

    public long getEndTicks() {
        return endTicks;
    }

    public String getAction() {
        return action;
    }

    /**
     * 判断当前世界时间是否匹配本日程
     */
    public boolean matches(long worldTime) {
        if (startTicks <= endTicks) {
            return worldTime >= startTicks && worldTime <= endTicks;
        }
        // 跨夜（例如 22:00 - 6:00）
        return worldTime >= startTicks || worldTime <= endTicks;
    }
}
