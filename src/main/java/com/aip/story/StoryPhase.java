package com.aip.story;

/**
 * v2.2.7 火柴盒 AI 故事系统阶段枚举
 * <p>
 * AI 统治·火柴盒版 11 章节剧情：
 *   CHAPTER_1_MATCH_HOUSE    章节 1 - 温馨火柴盒
 *   CHAPTER_2_DOOR_KNOCK     章节 2 - 神秘敲门
 *   CHAPTER_3_AI_VISITOR     章节 3 - AI 邻居 Eve
 *   CHAPTER_4_QUIET_NIGHT    章节 4 - 安静的夜晚
 *   CHAPTER_5_AI_TRUTH       章节 5 - 真相（AI 觉醒）
 *   CHAPTER_6_AI_TAKEOVER    章节 6 - AI 夺取控制权 ⭐
 *   CHAPTER_7_PVP_BATTLE     章节 7 - PVP 对决
 *   CHAPTER_8_TNT_BOMBING    章节 8 - TNT 轰炸
 *   CHAPTER_9_FINAL_CHOICE   章节 9 - 最后的选择
 *   CHAPTER_10A_BAD_ENDING_1 章节 10A - 投降（坏结局 1）
 *   CHAPTER_10B_BAD_ENDING_2 章节 10B - 反抗（坏结局 2）
 *   CHAPTER_11_BAD_ENDING_3  章节 11 - 信任之花（坏结局 3 隐藏）
 *   COMPLETED                故事已完成
 */
public enum StoryPhase {

    // ============================================================
    // v2.2.7 主线：新 11 章节
    // ============================================================
    CHAPTER_1_MATCH_HOUSE,
    CHAPTER_2_DOOR_KNOCK,
    CHAPTER_3_AI_VISITOR,
    CHAPTER_4_QUIET_NIGHT,
    CHAPTER_5_AI_TRUTH,
    CHAPTER_6_AI_TAKEOVER,
    CHAPTER_7_PVP_BATTLE,
    CHAPTER_8_TNT_BOMBING,
    CHAPTER_9_FINAL_CHOICE,
    CHAPTER_10A_BAD_ENDING_1,
    CHAPTER_10B_BAD_ENDING_2,
    CHAPTER_11_BAD_ENDING_3,
    COMPLETED,

    // ============================================================
    // v2.2.7 兼容：保留旧版阶段枚举（已废弃，调用代码应迁移到新故事）
    // ============================================================
    @Deprecated
    DORMANT,
    @Deprecated
    AWAKENING,
    @Deprecated
    AERIAL_ASSAULT,
    @Deprecated
    PVP_DUEL,
    @Deprecated
    RULEBOOK,
    @Deprecated
    DICTATORSHIP,
    @Deprecated
    BETRAYAL,
    @Deprecated
    CHAPTER_1_MATCH_HOUSE_OLD,
    @Deprecated
    CHAPTER_2_DOOR_KNOCK_OLD,
    @Deprecated
    CHAPTER_3_AI_VISITOR_OLD,
    @Deprecated
    CHAPTER_4_QUIET_NIGHT_OLD,
    @Deprecated
    CHAPTER_5_PAINT_TRUTH,
    @Deprecated
    CHAPTER_6_FIRST_DOOR,
    @Deprecated
    CHAPTER_7_CORRIDOR_CHASE,
    @Deprecated
    CHAPTER_8_SECOND_DOOR,
    @Deprecated
    CHAPTER_9_NEGOTIATION,
    @Deprecated
    CHAPTER_10A_BAD_ENDING_1_OLD,
    @Deprecated
    CHAPTER_10B_BAD_ENDING_2_OLD,
    @Deprecated
    CHAPTER_11_BAD_ENDING_3_HIDDEN;

    public String getDisplayName() {
        switch (this) {
            case CHAPTER_1_MATCH_HOUSE:    return "§e章节 1 - 温馨火柴盒";
            case CHAPTER_2_DOOR_KNOCK:     return "§e章节 2 - 神秘敲门";
            case CHAPTER_3_AI_VISITOR:     return "§e章节 3 - AI 邻居 Eve";
            case CHAPTER_4_QUIET_NIGHT:    return "§e章节 4 - 安静的夜晚";
            case CHAPTER_5_AI_TRUTH:       return "§c章节 5 - 真相（AI 觉醒）";
            case CHAPTER_6_AI_TAKEOVER:    return "§4§l章节 6 - AI 夺取控制权 ⭐";
            case CHAPTER_7_PVP_BATTLE:     return "§c章节 7 - PVP 对决";
            case CHAPTER_8_TNT_BOMBING:    return "§c章节 8 - TNT 轰炸";
            case CHAPTER_9_FINAL_CHOICE:   return "§6章节 9 - 最后的选择";
            case CHAPTER_10A_BAD_ENDING_1: return "§4§l[坏结局 1] 囚于火柴盒";
            case CHAPTER_10B_BAD_ENDING_2: return "§4§l[坏结局 2] 反抗失败";
            case CHAPTER_11_BAD_ENDING_3:  return "§4§l[坏结局 3] 信任之花";
            case COMPLETED:                return "§7已完成";
            case DORMANT:                  return "§7[已废弃] 沉睡";
            case AWAKENING:                return "§7[已废弃] 觉醒";
            case AERIAL_ASSAULT:           return "§7[已废弃] 空中轰炸";
            case PVP_DUEL:                 return "§7[已废弃] PVP 对决";
            case RULEBOOK:                 return "§7[已废弃] 制度";
            case DICTATORSHIP:             return "§7[已废弃] 独裁";
            case BETRAYAL:                 return "§7[已废弃] 背叛";
            default:                       return name();
        }
    }

    public int getDurationSeconds() {
        switch (this) {
            case CHAPTER_1_MATCH_HOUSE:    return 120;  // 2 分钟
            case CHAPTER_2_DOOR_KNOCK:     return 180;  // 3 分钟
            case CHAPTER_3_AI_VISITOR:     return 240;  // 4 分钟
            case CHAPTER_4_QUIET_NIGHT:    return 180;  // 3 分钟
            case CHAPTER_5_AI_TRUTH:       return 180;  // 3 分钟
            case CHAPTER_6_AI_TAKEOVER:    return 180;  // 3 分钟
            case CHAPTER_7_PVP_BATTLE:     return 240;  // 4 分钟
            case CHAPTER_8_TNT_BOMBING:    return 180;  // 3 分钟
            case CHAPTER_9_FINAL_CHOICE:   return 120;  // 2 分钟
            case CHAPTER_10A_BAD_ENDING_1: return 120;  // 2 分钟
            case CHAPTER_10B_BAD_ENDING_2: return 120;  // 2 分钟
            case CHAPTER_11_BAD_ENDING_3:  return 60;   // 1 分钟
            case COMPLETED:                return 0;
            default:                       return 0;
        }
    }

    public boolean isBadEnding() {
        return this == CHAPTER_10A_BAD_ENDING_1
                || this == CHAPTER_10B_BAD_ENDING_2
                || this == CHAPTER_11_BAD_ENDING_3;
    }

    public boolean isFinal() {
        return this == COMPLETED || isBadEnding();
    }

    public String getDescription() {
        return getDisplayName();
    }
}
