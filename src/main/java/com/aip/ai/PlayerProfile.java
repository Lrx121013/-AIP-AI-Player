package com.aip.ai;

import java.util.UUID;

/**
 * 真实玩家档案
 */
public class PlayerProfile {
    private final UUID uuid;
    private String name;
    private int threatLevel; // 0-100，越高越危险
    private int attackCount; // 攻击 AI 次数
    private String lastEquipment; // 最后观察到的装备
    private int relationship; // -100 到 100
    private long lastSeen;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.threatLevel = 0;
        this.attackCount = 0;
        this.lastEquipment = "";
        this.relationship = 0;
        this.lastSeen = 0L;
    }

    /** 记录一次攻击：攻击次数 +1，威胁等级 +10（上限 100） */
    public void recordAttack() {
        this.attackCount++;
        this.threatLevel = Math.min(100, this.threatLevel + 10);
    }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getThreatLevel() { return threatLevel; }
    public void setThreatLevel(int threatLevel) {
        this.threatLevel = Math.max(0, Math.min(100, threatLevel));
    }

    public int getAttackCount() { return attackCount; }
    public void setAttackCount(int attackCount) { this.attackCount = attackCount; }

    public String getLastEquipment() { return lastEquipment; }
    public void setLastEquipment(String lastEquipment) { this.lastEquipment = lastEquipment; }

    public int getRelationship() { return relationship; }
    public void setRelationship(int relationship) {
        // 自动 clamp 到 -100 到 100
        this.relationship = Math.max(-100, Math.min(100, relationship));
    }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}
