package com.aip.ai;

/**
 * 死亡记录
 * <p>
 * 记录单次死亡的元信息：时间戳、死因、击杀者。
 */
public class DeathRecord {

    private final long timestamp;
    private final String cause;
    private final String killer;

    public DeathRecord(long timestamp, String cause, String killer) {
        this.timestamp = timestamp;
        this.cause = cause;
        this.killer = killer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCause() {
        return cause;
    }

    public String getKiller() {
        return killer;
    }
}
