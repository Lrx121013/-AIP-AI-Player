package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 主线任务执行器：周期性推进 AI 玩家的主线任务阶段。
 * <p>
 * 每 {@code executor-interval} tick 执行一次 {@link #tick()}，遍历当前阶段的
 * actions 调用 {@code CommandExecutor} 触发对应行为，并按
 * {@link MainQuest.CompletionCondition} 判定是否进入下一阶段。
 * 阶段完成时异步通知 LLM（若可用）或仅记录日志。
 * </p>
 */
public class MainQuestExecutor {

    /** 插件实例，用于获取配置 / 调度器 / 日志 / 命令执行器 */
    private final AIPlayerPlugin plugin;
    /** 所属 AI 玩家 */
    private final AIPlayer owner;
    /** 周期任务引用，可取消 */
    private BukkitTask task;

    /**
     * 构造执行器。
     *
     * @param plugin 插件实例
     * @param owner  所属 AI 玩家
     */
    public MainQuestExecutor(AIPlayerPlugin plugin, AIPlayer owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    /**
     * 启动周期性执行任务。
     * <p>
     * 若主线任务系统未启用、owner 无主线任务或已启动则直接返回，避免重复调度。
     * </p>
     *
     * @param ai 启动目标的 AI 玩家（与 owner 一致）
     */
    public void startFor(AIPlayer ai) {
        // 若主线任务系统未启用或 owner.mainQuest 为 null，直接返回
        if (!plugin.getConfigManager().isMainQuestEnabled()) return;
        if (owner.getMainQuest() == null) return;
        // 若已启动，不重复
        if (task != null) return;
        int intervalTicks = plugin.getConfigManager().getQuestExecutorInterval();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (Exception e) {
                    plugin.getLogger().warning("MainQuestExecutor tick 异常: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * 单次执行逻辑：
     * <ol>
     *   <li>校验主线任务存在且未完成，否则取消任务</li>
     *   <li>校验实体在线、未忙、未在寻路</li>
     *   <li>遍历当前阶段 actions 调 CommandExecutor</li>
     *   <li>检查完成条件，满足则推进阶段并异步通知</li>
     * </ol>
     */
    private void tick() {
        // 1. 若主线任务为空或已完成 → 取消任务并返回
        if (owner.getMainQuest() == null || owner.getMainQuest().isCompleted()) {
            cancel();
            return;
        }
        // 2. 取 owner 实体，若不存在或无效则跳过本轮
        Player v = owner.getEntity();
        if (v == null || !v.isValid()) return;
        // 3. 若 LLM 决策中或正在寻路则跳过本轮
        if (owner.getBusy().get()) return;
        if (NpcHelper.isNavigating(v)) return;
        // 4. 取当前阶段，若为 null 则跳过
        MainQuest.QuestStage stage = owner.getMainQuest().getCurrentStage();
        if (stage == null) return;
        // 5. 遍历 actions 逐条执行（用 try-catch 包住每条避免单条失败影响整体）
        if (stage.getActions() != null) {
            for (String action : stage.getActions()) {
                try {
                    plugin.getCommandExecutor().execute(owner, "[COMMAND:" + action + "]");
                } catch (Exception e) {
                    plugin.getLogger().warning("MainQuestExecutor 执行动作 [" + action + "] 失败: " + e.getMessage());
                }
            }
        }
        // 6. 检查完成条件，满足则推进阶段
        if (checkCompletion(stage)) {
            int oldIndex = owner.getMainQuest().getCurrentStageIndex();
            MainQuest.QuestStage nextStage = peekNextStage(oldIndex);
            boolean hasNext = owner.getMainQuest().advanceStage();
            owner.setStageStartTime(System.currentTimeMillis());
            // 异步通知 LLM（避免阻塞主线程）
            final int finalOldIndex = oldIndex;
            final MainQuest.QuestStage finalStage = stage;
            final MainQuest.QuestStage finalNextStage = hasNext ? nextStage : null;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    notifyStageComplete(finalOldIndex, finalStage, finalNextStage)
            );
        }
    }

    /**
     * 窥探下一阶段（不推进索引）。
     *
     * @param currentIndex 当前阶段索引
     * @return 下一阶段对象；若越界则返回 null
     */
    private MainQuest.QuestStage peekNextStage(int currentIndex) {
        MainQuest quest = owner.getMainQuest();
        if (quest == null || quest.getStages() == null) return null;
        int next = currentIndex + 1;
        if (next < 0 || next >= quest.getStages().size()) return null;
        return quest.getStages().get(next);
    }

    /**
     * 通知 LLM 阶段已完成。
     * <p>
     * 由于 {@code AIPlayer} 暂未暴露 {@code getConversationManager()} 方法，
     * 此处仅记录日志。LLM 会在下一轮 prompt 注入时通过
     * {@code MainQuest.getPromptSummary()} 看到阶段变化（由 Task 8 处理）。
     * </p>
     *
     * @param oldIndex  刚完成的阶段索引（0-based）
     * @param oldStage  刚完成的阶段对象
     * @param nextStage 下一阶段对象；若为 null 表示主线任务已全部完成
     */
    private void notifyStageComplete(int oldIndex, MainQuest.QuestStage oldStage, MainQuest.QuestStage nextStage) {
        try {
            String questTitle = owner.getMainQuest() != null ? owner.getMainQuest().getTitle() : "";
            String msg = "你的主线任务 [" + questTitle + "] 阶段" + (oldIndex + 1)
                    + " [" + (oldStage != null ? oldStage.getDescription() : "") + "] 已完成"
                    + (nextStage != null
                            ? "，进入阶段" + (oldIndex + 2) + "：" + nextStage.getDescription()
                            : "，主线任务全部完成")
                    + "。";
            plugin.getLogger().info("[MainQuest] " + owner.getName() + ": " + msg);
        } catch (Exception e) {
            plugin.getLogger().warning("notifyStageComplete 异常: " + e.getMessage());
        }
    }

    /**
     * 按完成条件枚举判定当前阶段是否已完成。
     * <p>
     * 各分支语义详见 {@link MainQuest.CompletionCondition}。
     * </p>
     *
     * @param stage 当前阶段
     * @return true=已完成可推进；false=未完成
     */
    private boolean checkCompletion(MainQuest.QuestStage stage) {
        if (stage == null) return false;
        Player v = owner.getEntity();
        if (v == null || !v.isValid()) return false;

        switch (stage.getCompletionCondition()) {
            case REACH_PLAYER: {
                // 附近 3 格内有玩家（排除 owner 自己）
                for (org.bukkit.entity.Entity e : v.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof Player && !e.getUniqueId().equals(owner.getEntityId())) {
                        return true;
                    }
                }
                return false;
            }
            case APPROACH_COUNT: {
                // 每次检查若附近 5 格内有玩家，进度 +1（每 tick 最多 +1）
                boolean near = false;
                for (org.bukkit.entity.Entity e : v.getNearbyEntities(5, 5, 5)) {
                    if (e instanceof Player && !e.getUniqueId().equals(owner.getEntityId())) {
                        near = true;
                        break;
                    }
                }
                if (near && stage.getCurrentProgress() < stage.getTargetProgress()) {
                    owner.getMainQuest().incrementProgress(1);
                }
                return stage.getCurrentProgress() >= stage.getTargetProgress();
            }
            case KILL_TARGET: {
                // 阶段期间杀死任意玩家即满足；满足后清空避免下一阶段误判
                String lastKill = owner.getLastKillName();
                if (lastKill != null && !lastKill.isEmpty()) {
                    owner.setLastKillName(null);
                    return true;
                }
                return false;
            }
            case ELAPSE_TIME: {
                // 阶段开始后经过 target*10 秒即满足
                long elapsedSec = (System.currentTimeMillis() - owner.getStageStartTime()) / 1000L;
                return elapsedSec >= (long) stage.getTargetProgress() * 10L;
            }
            case COLLECT_ITEMS: {
                // 简化判定：背包非空视为正在收集，每 tick 进度 +1
                if (v.getInventory().getSize() > 0 && stage.getCurrentProgress() < stage.getTargetProgress()) {
                    owner.getMainQuest().incrementProgress(1);
                }
                return stage.getCurrentProgress() >= stage.getTargetProgress();
            }
            case REACH_LOCATION:
                // 暂未实现目标地点判定
                return false;
            case NONE:
                // 永远满足，用于最后一阶段兜底
                return true;
            default:
                return false;
        }
    }

    /**
     * 取消周期任务并置空引用。
     */
    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
