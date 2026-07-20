package com.aip.ai;

/**
 * 反射规则数据类
 * <p>
 * 描述一条 AI 自定义的反射规则：当满足触发条件时本地执行动作，
 * 不调用 LLM。规则由 AI 通过命令添加，可启用/禁用/删除。
 */
public class ReflexRule {

    /** 触发器类型 */
    public enum TriggerType {
        PLAYER_NEARBY,      // 玩家进入半径内（参数：半径格数）
        MOB_NEARBY,         // 怪物进入半径内（参数：半径格数）
        LOW_HEALTH,         // 血量低于百分比（参数：0-100）
        LOW_FOOD,           // 饱食度低于阈值（参数：0-20）
        ON_DAMAGE,          // 受到任何伤害（无参数）
        PLAYER_ATTACK,      // 被玩家攻击（无参数）
        BLOCK_BREAK_NEARBY, // 附近方块被破坏（参数：半径格数）
        TIME_PERIOD         // 时间段（参数：day 或 night）
    }

    private final String id;                 // 规则 ID（如 "r1"）
    private final TriggerType triggerType;   // 触发类型
    private final String condition;          // 触发条件参数（如 "5"）
    private final String action;             // 动作字符串（不含 [COMMAND: 包裹）
    private final int cooldownMs;            // 冷却毫秒
    private boolean enabled;                 // 是否启用
    private long lastTriggered;              // 上次触发时间戳

    public ReflexRule(String id, TriggerType triggerType, String condition,
                      String action, int cooldownMs) {
        this.id = id;
        this.triggerType = triggerType;
        this.condition = condition;
        this.action = action;
        this.cooldownMs = cooldownMs;
        this.enabled = true;
        this.lastTriggered = 0;
    }

    public String getId() { return id; }
    public TriggerType getTriggerType() { return triggerType; }
    public String getCondition() { return condition; }
    public String getAction() { return action; }
    public int getCooldownMs() { return cooldownMs; }
    public boolean isEnabled() { return enabled; }
    public long getLastTriggered() { return lastTriggered; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setLastTriggered(long t) { this.lastTriggered = t; }

    /** 判断当前是否可触发：已启用且冷却已过 */
    public boolean canTrigger(long now) {
        return enabled && (now - lastTriggered >= cooldownMs);
    }

    /** 标记本次触发时间 */
    public void markTriggered(long now) {
        this.lastTriggered = now;
    }
}
