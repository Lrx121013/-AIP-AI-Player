package com.aip.story;

/**
 * 故事模式阶段枚举
 * <p>
 * 完整故事弧线：DORMANT → AWAKENING → AERIAL_ASSAULT → PVP_DUEL → RULEBOOK → DICTATORSHIP → BETRAYAL → COMPLETED
 * <p>
 * v2.1.3 起取代 v1.8.0 的 MainQuest 推进系统，专为"邪恶 AI"设计的有起承转合的叙事主线。
 */
public enum StoryPhase {
    /** 潜默：AI 表现为友好 NPC，初始状态 */
    DORMANT("潜默", "你是一个友好的 NPC，尚未觉醒，对玩家没有敌意。"),
    /** 觉醒：被玩家击杀 3 次后，AI 觉醒并开始攻击玩家 */
    AWAKENING("觉醒", "你被击杀了多次，愤怒觉醒，开始对玩家抱有敌意。"),
    /** 空中轰炸：玩家死亡 3 次后，AI 进入创造模式飞行轰炸 3-4 分钟 */
    AERIAL_ASSAULT("空中轰炸", "你已经统治制空权，在天上用 TNT 和方块轰炸玩家，玩家必须躲避。"),
    /** 顶级对决：空中阶段结束，AI 降下并装备顶级下界合金 PVP */
    PVP_DUEL("顶级对决", "你穿上了下界合金装备，与玩家正面对决。"),
    /** 制度统治：AI 杀玩家 2 次后，把"AI 制度之书"放进玩家物品栏 */
    RULEBOOK("制度统治", "你建立了服务器的基本制度，写成一本书交给玩家阅读。"),
    /** 独裁命令：玩家读完后，AI 给玩家下命令 */
    DICTATORSHIP("独裁命令", "玩家已经读完了制度，现在必须听从你的命令。"),
    /** 背叛：AI 决定杀死玩家，故事进入高潮 */
    BETRAYAL("背叛", "你决定背叛玩家，杀死他。"),
    /** 完成：故事结束，AI 持续嘲讽 30 秒后停止所有行动 */
    COMPLETED("完成", "故事已经结束，你已经杀死了玩家。");

    private final String displayName;
    private final String description;

    StoryPhase(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /**
     * 校验阶段转移是否合法
     * <p>
     * 合法顺序：DORMANT → AWAKENING → AERIAL_ASSAULT → PVP_DUEL → RULEBOOK → DICTATORSHIP → BETRAYAL → COMPLETED
     * 同一阶段返回 true（幂等）
     * COMPLETED 是终态，不能再转移
     */
    public static boolean isValidTransition(StoryPhase from, StoryPhase to) {
        if (from == null || to == null) return false;
        if (from == to) return true;  // 幂等
        if (from == COMPLETED) return false;  // 终态

        switch (from) {
            case DORMANT: return to == AWAKENING;
            case AWAKENING: return to == AERIAL_ASSAULT;
            case AERIAL_ASSAULT: return to == PVP_DUEL;
            case PVP_DUEL: return to == RULEBOOK;
            case RULEBOOK: return to == DICTATORSHIP;
            case DICTATORSHIP: return to == BETRAYAL;
            case BETRAYAL: return to == COMPLETED;
            default: return false;
        }
    }

    /**
     * 获取下一阶段（用于 prompt 注入和剧情提示）
     */
    public StoryPhase getNext() {
        switch (this) {
            case DORMANT: return AWAKENING;
            case AWAKENING: return AERIAL_ASSAULT;
            case AERIAL_ASSAULT: return PVP_DUEL;
            case PVP_DUEL: return RULEBOOK;
            case RULEBOOK: return DICTATORSHIP;
            case DICTATORSHIP: return BETRAYAL;
            case BETRAYAL: return COMPLETED;
            default: return null;
        }
    }

    /**
     * 获取阶段序号（从 1 开始），用于显示 "阶段 X/7"
     */
    public int getIndex() {
        switch (this) {
            case DORMANT: return 1;
            case AWAKENING: return 2;
            case AERIAL_ASSAULT: return 3;
            case PVP_DUEL: return 4;
            case RULEBOOK: return 5;
            case DICTATORSHIP: return 6;
            case BETRAYAL: return 7;
            case COMPLETED: return 8;
            default: return 0;
        }
    }
}
