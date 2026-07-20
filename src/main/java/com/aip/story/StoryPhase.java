package com.aip.story;

/**
 * v2.2.7：火柴盒故事阶段（12 章节 + COMPLETED）
 */
public enum StoryPhase {
    /** 章节 1 - 火柴盒（开场） */
    CHAPTER_1_MATCH_HOUSE,
    /** 章节 2 - 神秘敲门 */
    CHAPTER_2_DOOR_KNOCK,
    /** 章节 3 - 第三个 AI 访客（Eve 出现） */
    CHAPTER_3_AI_VISITOR,
    /** 章节 4 - 安静的夜晚 */
    CHAPTER_4_QUIET_NIGHT,
    /** 章节 5 - 画里的真相（Mr. Sparkle 自爆） */
    CHAPTER_5_PAINT_TRUTH,
    /** 章节 6 - 第一道门（逃出火柴盒） */
    CHAPTER_6_FIRST_DOOR,
    /** 章节 7 - 走廊追逐 */
    CHAPTER_7_CORRIDOR_CHASE,
    /** 章节 8 - 第二道门：真相（AI 总部） */
    CHAPTER_8_SECOND_DOOR,
    /** 章节 9 - 谈判（Eve 给你选择） */
    CHAPTER_9_NEGOTIATION,
    /** 章节 10A - 回家（坏结局 1） */
    CHAPTER_10A_BAD_ENDING_1,
    /** 章节 10B - 拒绝（坏结局 2） */
    CHAPTER_10B_BAD_ENDING_2,
    /** 章节 11 - 隐藏坏结局 3（信任之花） */
    CHAPTER_11_BAD_ENDING_3_HIDDEN,
    /** 故事已完成 */
    COMPLETED;

    public String getDisplayName() {
        switch (this) {
            case CHAPTER_1_MATCH_HOUSE: return "§e火柴盒";
            case CHAPTER_2_DOOR_KNOCK: return "§e神秘敲门";
            case CHAPTER_3_AI_VISITOR: return "§e第三个 AI 访客";
            case CHAPTER_4_QUIET_NIGHT: return "§e安静的夜晚";
            case CHAPTER_5_PAINT_TRUTH: return "§e画里的真相";
            case CHAPTER_6_FIRST_DOOR: return "§e第一道门";
            case CHAPTER_7_CORRIDOR_CHASE: return "§c走廊追逐";
            case CHAPTER_8_SECOND_DOOR: return "§c第二道门：真相";
            case CHAPTER_9_NEGOTIATION: return "§4谈判";
            case CHAPTER_10A_BAD_ENDING_1: return "§4[坏结局 1] 囚于火柴盒";
            case CHAPTER_10B_BAD_ENDING_2: return "§4[坏结局 2] 死在走廊";
            case CHAPTER_11_BAD_ENDING_3_HIDDEN: return "§4[坏结局 3] 信任之花";
            case COMPLETED: return "§7已完成";
            default: return name();
        }
    }

    public int getDurationSeconds() {
        switch (this) {
            case CHAPTER_1_MATCH_HOUSE: return 120;        // 2 分钟
            case CHAPTER_2_DOOR_KNOCK: return 180;         // 3 分钟
            case CHAPTER_3_AI_VISITOR: return 300;         // 5 分钟
            case CHAPTER_4_QUIET_NIGHT: return 120;        // 2 分钟
            case CHAPTER_5_PAINT_TRUTH: return 180;        // 3 分钟
            case CHAPTER_6_FIRST_DOOR: return 240;         // 4 分钟
            case CHAPTER_7_CORRIDOR_CHASE: return 180;     // 3 分钟
            case CHAPTER_8_SECOND_DOOR: return 180;        // 3 分钟
            case CHAPTER_9_NEGOTIATION: return 120;        // 2 分钟
            case CHAPTER_10A_BAD_ENDING_1: return 120;     // 2 分钟
            case CHAPTER_10B_BAD_ENDING_2: return 120;     // 2 分钟
            case CHAPTER_11_BAD_ENDING_3_HIDDEN: return 60; // 1 分钟
            default: return 0;
        }
    }
}
