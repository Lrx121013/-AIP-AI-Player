package com.aip.ai;

/**
 * AI 玩家统计数据
 * <p>
 * 用于记录 AI 的对话次数、行走距离、击杀数、在线时长、命令执行情况等。
 */
public class AIStats {

    private long chatCount = 0;
    private long walkDistance = 0;
    private long killCount = 0;
    private long onlineTimeMs = 0;
    private long commandCount = 0;
    private long commandSuccess = 0;

    /** AI 生成时的时间戳，用于计算在线时长 */
    private final long spawnTimeMs = System.currentTimeMillis();

    public void incChat() {
        chatCount++;
    }

    public void addWalk(double d) {
        if (d > 0) walkDistance += (long) d;
    }

    public void incKill() {
        killCount++;
    }

    public void incCommand(boolean success) {
        commandCount++;
        if (success) commandSuccess++;
    }

    public long getChatCount() {
        return chatCount;
    }

    public long getWalkDistance() {
        return walkDistance;
    }

    public long getKillCount() {
        return killCount;
    }

    /**
     * 在线时长（毫秒）—— 从 AI 生成时计算
     */
    public long getOnlineTimeMs() {
        return System.currentTimeMillis() - spawnTimeMs;
    }

    public long getCommandCount() {
        return commandCount;
    }

    public long getCommandSuccess() {
        return commandSuccess;
    }
}
