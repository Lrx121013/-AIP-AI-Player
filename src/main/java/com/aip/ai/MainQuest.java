package com.aip.ai;

import java.util.List;
import java.util.UUID;

/**
 * 主线任务数据类。
 * <p>
 * 用于描述一个 AI 玩家的长线目标，由多个 {@link QuestStage} 阶段顺序组成。
 * 每个 AI 在 spawn 时根据其 {@code Personality} 绑定一个 {@code MainQuest}，
 * 由 {@code MainQuestExecutor} 周期性推进阶段进度。
 * </p>
 * <p>
 * 该类只承载任务数据结构与摘要拼装逻辑，不依赖 Bukkit 或任何外部库，
 * 便于单元测试与跨模块复用。
 * </p>
 */
public class MainQuest {

    /** 任务 ID（用 UUID 前 8 位或简单字符串） */
    private final String id;

    /** 任务标题，如 "潜入渗透" */
    private final String title;

    /** 阶段列表，按顺序推进 */
    private final List<QuestStage> stages;

    /** 当前阶段索引，从 0 开始 */
    private int currentStageIndex;

    /** 所属 AI 的 UUID */
    private final UUID ownerId;

    /** 是否已完成 */
    private boolean completed;

    /**
     * 构造一个主线任务。
     *
     * @param id      任务 ID（UUID 前 8 位或简单字符串）
     * @param title   任务标题，如 "潜入渗透"
     * @param ownerId 所属 AI 的 UUID
     * @param stages  阶段列表，至少应包含一个阶段
     */
    public MainQuest(String id, String title, UUID ownerId, List<QuestStage> stages) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.stages = stages;
        this.currentStageIndex = 0;
        this.completed = false;
    }

    /**
     * 获取当前阶段。
     *
     * @return 当前阶段对象；若 currentStageIndex 越界则返回 {@code null}
     */
    public QuestStage getCurrentStage() {
        if (stages == null || currentStageIndex < 0 || currentStageIndex >= stages.size()) {
            return null;
        }
        return stages.get(currentStageIndex);
    }

    /**
     * 推进到下一阶段。
     * <p>
     * 将 currentStageIndex 自增 1；若自增后越界（无下一阶段），
     * 则将 {@link #completed} 置为 {@code true} 并返回 {@code false}。
     * </p>
     *
     * @return 若存在下一阶段返回 {@code true}；若无下一阶段（已推进至最终后越界）返回 {@code false}
     */
    public boolean advanceStage() {
        currentStageIndex++;
        if (stages == null || currentStageIndex >= stages.size()) {
            completed = true;
            return false;
        }
        return true;
    }

    /**
     * 返回注入到 system prompt 的多行任务摘要。
     * <p>
     * 格式如下：
     * <pre>
     * 你的主线任务：{title}（阶段 {currentStageIndex+1}/{stages.size()}：{currentStage.description}）
     * 当前进度：{currentStage.currentProgress}/{currentStage.targetProgress}
     * 下一阶段：{nextStage.description or "无（最终阶段）"}
     * </pre>
     * 若 {@link #completed} 为 {@code true} 或 stages 为空，返回空串 ""。
     * </p>
     *
     * @return 多行摘要字符串，或空串
     */
    public String getPromptSummary() {
        if (completed || stages == null || stages.isEmpty()) {
            return "";
        }
        QuestStage current = getCurrentStage();
        if (current == null) {
            return "";
        }
        int nextIndex = currentStageIndex + 1;
        String nextDesc = (nextIndex < stages.size()) ? stages.get(nextIndex).getDescription() : "无（最终阶段）";
        StringBuilder sb = new StringBuilder();
        sb.append("你的主线任务：")
                .append(title)
                .append("（阶段 ").append(currentStageIndex + 1).append("/").append(stages.size())
                .append("：").append(current.getDescription()).append("）")
                .append("\n");
        sb.append("当前进度：")
                .append(current.getCurrentProgress()).append("/").append(current.getTargetProgress())
                .append("\n");
        sb.append("下一阶段：").append(nextDesc);
        return sb.toString();
    }

    /**
     * 给当前阶段的 currentProgress 增加指定增量，并 clamp 到 [0, targetProgress]。
     * <p>
     * 若当前阶段为 {@code null}，则不做任何操作。
     * </p>
     *
     * @param delta 进度增量（可为负数）
     */
    public void incrementProgress(int delta) {
        QuestStage current = getCurrentStage();
        if (current == null) {
            return;
        }
        int target = current.getTargetProgress();
        int newValue = current.getCurrentProgress() + delta;
        if (newValue < 0) {
            newValue = 0;
        }
        if (newValue > target) {
            newValue = target;
        }
        current.setCurrentProgress(newValue);
    }

    /**
     * @return 任务 ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return 任务标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 阶段列表
     */
    public List<QuestStage> getStages() {
        return stages;
    }

    /**
     * @return 当前阶段索引
     */
    public int getCurrentStageIndex() {
        return currentStageIndex;
    }

    /**
     * @return 所属 AI 的 UUID
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * 设置当前阶段索引。
     *
     * @param currentStageIndex 新的阶段索引
     */
    public void setCurrentStageIndex(int currentStageIndex) {
        this.currentStageIndex = currentStageIndex;
    }

    /**
     * 设置任务完成状态。
     *
     * @param completed 是否已完成
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * 主线任务的阶段定义。
     * <p>
     * 每个阶段包含描述文本、完成条件、每周期执行的 COMMAND 动作列表，
     * 以及一个 [0, targetProgress] 范围内的进度计数。
     * </p>
     */
    public static class QuestStage {

        /** 阶段描述 */
        private final String description;

        /** 完成条件枚举 */
        private final CompletionCondition completionCondition;

        /** 每周期执行的 COMMAND 字符串列表，如 ["approach_nearest_player", "say hi"] */
        private final List<String> actions;

        /** 目标进度值 */
        private final int targetProgress;

        /** 当前进度 */
        private int currentProgress;

        /**
         * 构造一个阶段。
         *
         * @param description        阶段描述
         * @param completionCondition 完成条件枚举
         * @param actions            每周期执行的 COMMAND 字符串列表
         * @param targetProgress     目标进度值
         */
        public QuestStage(String description, CompletionCondition completionCondition, List<String> actions, int targetProgress) {
            this.description = description;
            this.completionCondition = completionCondition;
            this.actions = actions;
            this.targetProgress = targetProgress;
            this.currentProgress = 0;
        }

        /**
         * @return 阶段描述
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return 完成条件枚举
         */
        public CompletionCondition getCompletionCondition() {
            return completionCondition;
        }

        /**
         * @return 每周期执行的 COMMAND 字符串列表
         */
        public List<String> getActions() {
            return actions;
        }

        /**
         * @return 目标进度值
         */
        public int getTargetProgress() {
            return targetProgress;
        }

        /**
         * @return 当前进度
         */
        public int getCurrentProgress() {
            return currentProgress;
        }

        /**
         * 设置当前进度。
         *
         * @param currentProgress 新的当前进度
         */
        public void setCurrentProgress(int currentProgress) {
            this.currentProgress = currentProgress;
        }
    }

    /**
     * 阶段完成条件枚举。
     * <p>
     * 由 {@code MainQuestExecutor.checkCompletion} 按枚举分支判定。
     * </p>
     */
    public enum CompletionCondition {
        /** 附近 3 格内有玩家 */
        REACH_PLAYER,

        /** 到达指定地点（暂未用，返回 false） */
        REACH_LOCATION,

        /** 杀死目标（简化：阶段期间杀死任意玩家即满足） */
        KILL_TARGET,

        /** 收集 target 个任意物品 */
        COLLECT_ITEMS,

        /** 经过 target*10 秒 */
        ELAPSE_TIME,

        /** 累计接近玩家数 ≥ target */
        APPROACH_COUNT,

        /** 永远满足（用于最后一阶段兜底） */
        NONE
    }
}
