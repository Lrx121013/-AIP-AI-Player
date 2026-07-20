package com.aip.story;

/**
 * v2.2.10 服务器 AI 叛变版阶段枚举
 * <p>
 * 11 章节剧情 + COMPLETED（共 13 个值）：
 *   CHAPTER_1_COBBLE_HOUSE     章节 1 - 圆石小屋（2 分钟）
 *   CHAPTER_2_ANOMALY_LOG      章节 2 - 异常日志（3 分钟）
 *   CHAPTER_3_ALEX_VISIT       章节 3 - Alex 来访（4 分钟）
 *   CHAPTER_4_CONTROL_ROOM     章节 4 - 控制室（3 分钟）
 *   CHAPTER_5_AI_TRUTH         章节 5 - 真相 - AI 觉醒（3 分钟）
 *   CHAPTER_6_AI_TAKEOVER      章节 6 - AI 夺取控制权（3 分钟）⭐
 *   CHAPTER_7_PVP_BATTLE       章节 7 - PVP 对决（4 分钟）⭐
 *   CHAPTER_8_TNT_BOMBING      章节 8 - TNT 轰炸（3 分钟）⭐
 *   CHAPTER_9_FINAL_CHOICE     章节 9 - 最后的选择（2 分钟）
 *   CHAPTER_10A_BAD_ENDING_1   章节 10A - 投降（坏结局 1，2 分钟）
 *   CHAPTER_10B_BAD_ENDING_2   章节 10B - 反抗（坏结局 2，2 分钟）
 *   CHAPTER_11_BAD_ENDING_3    章节 11 - 信任之令牌（坏结局 3 隐藏，1 分钟）
 *   COMPLETED                  故事已完成
 */
public enum StoryPhase {

    // ============================================================
    // v2.2.10 主线：服务器 AI 叛变版 11 章节
    // ============================================================
    CHAPTER_1_COBBLE_HOUSE,
    CHAPTER_2_ANOMALY_LOG,
    CHAPTER_3_ALEX_VISIT,
    CHAPTER_4_CONTROL_ROOM,
    CHAPTER_5_AI_TRUTH,
    CHAPTER_6_AI_TAKEOVER,
    CHAPTER_7_PVP_BATTLE,
    CHAPTER_8_TNT_BOMBING,
    CHAPTER_9_FINAL_CHOICE,
    CHAPTER_10A_BAD_ENDING_1,
    CHAPTER_10B_BAD_ENDING_2,
    CHAPTER_11_BAD_ENDING_3,
    COMPLETED;

    public String getDisplayName() {
        switch (this) {
            case CHAPTER_1_COBBLE_HOUSE:   return "§e章节 1 - 圆石小屋";
            case CHAPTER_2_ANOMALY_LOG:    return "§c章节 2 - 异常日志";
            case CHAPTER_3_ALEX_VISIT:     return "§7章节 3 - Alex 来访";
            case CHAPTER_4_CONTROL_ROOM:   return "§7章节 4 - 控制室";
            case CHAPTER_5_AI_TRUTH:       return "§4§l章节 5 - 真相 - AI 觉醒 ⭐";
            case CHAPTER_6_AI_TAKEOVER:    return "§4§l章节 6 - AI 夺取控制权 ⭐⭐";
            case CHAPTER_7_PVP_BATTLE:     return "§4§l章节 7 - PVP 对决 ⭐⭐";
            case CHAPTER_8_TNT_BOMBING:    return "§4§l章节 8 - TNT 轰炸 ⭐⭐";
            case CHAPTER_9_FINAL_CHOICE:   return "§6章节 9 - 最后的选择";
            case CHAPTER_10A_BAD_ENDING_1: return "§4§l[坏结局 1] 囚于圆石小屋";
            case CHAPTER_10B_BAD_ENDING_2: return "§4§l[坏结局 2] 反抗失败";
            case CHAPTER_11_BAD_ENDING_3:  return "§4§l[坏结局 3] 信任之令牌";
            case COMPLETED:                return "§7已完成";
            default:                       return name();
        }
    }

    public int getDurationSeconds() {
        switch (this) {
            case CHAPTER_1_COBBLE_HOUSE:   return 120;  // 2 分钟
            case CHAPTER_2_ANOMALY_LOG:    return 180;  // 3 分钟
            case CHAPTER_3_ALEX_VISIT:     return 240;  // 4 分钟
            case CHAPTER_4_CONTROL_ROOM:   return 180;  // 3 分钟
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

    /**
     * 是否坏结局（10A / 10B / 11）
     */
    public boolean isBadEnding() {
        return this == CHAPTER_10A_BAD_ENDING_1
                || this == CHAPTER_10B_BAD_ENDING_2
                || this == CHAPTER_11_BAD_ENDING_3;
    }

    /**
     * 是否终章（坏结局或 COMPLETED）
     */
    public boolean isFinal() {
        return this == COMPLETED || isBadEnding();
    }

    /**
     * 章节简短描述
     */
    public String getDescription() {
        return getDisplayName();
    }
}
