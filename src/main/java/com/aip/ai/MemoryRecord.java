package com.aip.ai;

/**
 * 长期记忆条目
 */
public class MemoryRecord {
    public enum Type { DEATH, ATTACK, DECEIVE, CLAIM, ALLIANCE, DISCOVERY }

    private final long timestamp;
    private final Type type;
    private final String summary;
    private final String relatedEntity; // 玩家名/AI名/区域名

    public MemoryRecord(long timestamp, Type type, String summary, String relatedEntity) {
        this.timestamp = timestamp;
        this.type = type;
        this.summary = summary;
        this.relatedEntity = relatedEntity;
    }

    public long getTimestamp() { return timestamp; }
    public Type getType() { return type; }
    public String getSummary() { return summary; }
    public String getRelatedEntity() { return relatedEntity; }
}
