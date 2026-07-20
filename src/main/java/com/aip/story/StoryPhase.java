package com.aip.story;

/**
 * v2.2.9 火柴盒故事系统阶段枚举（探索逃跑版）
 * <p>
 * 12 章节剧情 + COMPLETED：
 *   CHAPTER_1_MATCH_HOUSE    章节 1 - 温馨火柴盒（2 分钟）
 *   CHAPTER_2_DOOR_KNOCK     章节 2 - 神秘敲门（3 分钟）
 *   CHAPTER_3_AI_VISITOR     章节 3 - 第三个 AI 访客（5 分钟）
 *   CHAPTER_4_QUIET_NIGHT    章节 4 - 安静的夜晚（2 分钟）
 *   CHAPTER_5_PAINT_TRUTH    章节 5 - 画里的真相（3 分钟）
 *   CHAPTER_6_FIRST_DOOR     章节 6 - 第一道门（4 分钟）
 *   CHAPTER_7_CORRIDOR_CHASE 章节 7 - 走廊追逐（3 分钟）
 *   CHAPTER_8_SECOND_DOOR    章节 8 - 第二道门：真相（3 分钟）
 *   CHAPTER_9_NEGOTIATION    章节 9 - 谈判（2 分钟）
 *   CHAPTER_10A_BAD_ENDING_1 章节 10A - 回家（坏结局 1，2 分钟）
 *   CHAPTER_10B_BAD_ENDING_2 章节 10B - 拒绝（坏结局 2，2 分钟）
 *   CHAPTER_11_BAD_ENDING_3  章节 11 - 信任之花（坏结局 3 隐藏，1 分钟）
 *   COMPLETED                故事已完成
 */
public enum StoryPhase {

    // ============================================================
    // v2.2.9 主线：探索逃跑版 12 章节
    // ============================================================
    CHAPTER_1_MATCH_HOUSE,
    CHAPTER_2_DOOR_KNOCK,
    CHAPTER_3_AI_VISITOR,
    CHAPTER_4_QUIET_NIGHT,
    CHAPTER_5_PAINT_TRUTH,
    CHAPTER_6_FIRST_DOOR,
    CHAPTER_7_CORRIDOR_CHASE,
    CHAPTER_8_SECOND_DOOR,
    CHAPTER_9_NEGOTIATION,
    CHAPTER_10A_BAD_ENDING_1,
    CHAPTER_10B_BAD_ENDING_2,
    CHAPTER_11_BAD_ENDING_3,
    COMPLETED;

    public String getDisplayName() {
        switch (this) {
            case CHAPTER_1_MATCH_HOUSE:    return "§e章节 1 - 温馨火柴盒";
            case CHAPTER_2_DOOR_KNOCK:     return "§e章节 2 - 神秘敲门";
            case CHAPTER_3_AI_VISITOR:     return "§d章节 3 - 第三个 AI 访客";
            case CHAPTER_4_QUIET_NIGHT:    return "§7章节 4 - 安静的夜晚";
            case CHAPTER_5_PAINT_TRUTH:    return "§c§l章节 5 - 画里的真相 ⭐";
            case CHAPTER_6_FIRST_DOOR:     return "§c章节 6 - 第一道门";
            case CHAPTER_7_CORRIDOR_CHASE: return "§4§l章节 7 - 走廊追逐 ⭐";
            case CHAPTER_8_SECOND_DOOR:    return "§5章节 8 - 第二道门：真相";
            case CHAPTER_9_NEGOTIATION:    return "§6章节 9 - 谈判";
            case CHAPTER_10A_BAD_ENDING_1: return "§4§l[坏结局 1] 囚于火柴盒";
            case CHAPTER_10B_BAD_ENDING_2: return "§4§l[坏结局 2] 死在走廊";
            case CHAPTER_11_BAD_ENDING_3:  return "§4§l[坏结局 3] 信任之花";
            case COMPLETED:                return "§7已完成";
            default:                       return name();
        }
    }

    public int getDurationSeconds() {
        switch (this) {
            case CHAPTER_1_MATCH_HOUSE:    return 120;  // 2 分钟
            case CHAPTER_2_DOOR_KNOCK:     return 180;  // 3 分钟
            case CHAPTER_3_AI_VISITOR:     return 300;  // 5 分钟
            case CHAPTER_4_QUIET_NIGHT:    return 120;  // 2 分钟
            case CHAPTER_5_PAINT_TRUTH:    return 180;  // 3 分钟
            case CHAPTER_6_FIRST_DOOR:     return 240;  // 4 分钟
            case CHAPTER_7_CORRIDOR_CHASE: return 180;  // 3 分钟
            case CHAPTER_8_SECOND_DOOR:    return 180;  // 3 分钟
            case CHAPTER_9_NEGOTIATION:    return 120;  // 2 分钟
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
     * 是否走廊相关章节（6/7/8）—— 用于走廊随机 NPC 求救声
     */
    public boolean isCorridor() {
        return this == CHAPTER_6_FIRST_DOOR
                || this == CHAPTER_7_CORRIDOR_CHASE
                || this == CHAPTER_8_SECOND_DOOR;
    }

    /**
     * 是否终点（坏结局或 COMPLETED）
     */
    public boolean isEnd() {
        return this == COMPLETED || isBadEnding();
    }

    /**
     * 章节简短描述
     */
    public String getDescription() {
        return getDisplayName();
    }
}
