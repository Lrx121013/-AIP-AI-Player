package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 目标管理器
 * <p>
 * 维护单个 AI 玩家的所有长期目标，支持添加、完成、查询和生成 prompt 摘要。
 */
public class GoalManager {
    private final List<Goal> goals = new ArrayList<>();
    private final AIPlayerPlugin plugin;
    private final com.aip.ai.AIPlayer aiPlayer;
    private final Map<String, BukkitTask> pursuitTasks = new HashMap<>();

    public GoalManager(AIPlayerPlugin plugin, com.aip.ai.AIPlayer aiPlayer) {
        this.plugin = plugin;
        this.aiPlayer = aiPlayer;
    }

    public Goal addGoal(String description, int priority) {
        Goal g = new Goal(description, priority);
        goals.add(g);
        if (g.getCurrentTarget() != null && !g.getCurrentTarget().isEmpty()) {
            startPursuit(g);
        }
        return g;
    }

    public boolean completeGoal(String id) {
        BukkitTask task = pursuitTasks.remove(id);
        if (task != null) task.cancel();
        Optional<Goal> g = goals.stream().filter(x -> x.getId().equals(id)).findFirst();
        if (g.isPresent()) {
            g.get().complete();
            return true;
        }
        return false;
    }

    public List<Goal> getActiveGoals() {
        return goals.stream().filter(g -> !g.isCompleted())
                .sorted((a, b) -> b.getPriority() - a.getPriority()).toList();
    }

    public List<Goal> getAllGoals() {
        return goals;
    }

    /** 生成 prompt 摘要，供 system prompt 和自主决策使用 */
    public String getPromptSummary() {
        List<Goal> active = getActiveGoals();
        if (active.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("当前活跃目标：\n");
        for (Goal g : active) {
            sb.append("- ").append(g.getDescription())
                    .append("（优先级").append(g.getPriority())
                    .append("，进度").append(g.getProgress()).append("%）\n");
        }
        return sb.toString();
    }

    /**
     * 启动目标追击任务：每 60 tick（3 秒）检查目标距离，自动 issue walk/attack 命令。
     * 不调用 LLM，节省 token。
     */
    private void startPursuit(Goal goal) {
        // 取消该 goal 之前的追击任务（如果有）
        BukkitTask existing = pursuitTasks.get(goal.getId());
        if (existing != null) existing.cancel();

        BukkitTask task = new BukkitRunnable() {
            private long lastOnlineCheck = System.currentTimeMillis();
            @Override
            public void run() {
                if (goal.isCompleted()) {
                    cancel();
                    pursuitTasks.remove(goal.getId());
                    return;
                }
                Player aiEntity = aiPlayer.getEntity();
                if (aiEntity == null || !aiEntity.isValid()) return;

                String target = goal.getCurrentTarget();
                if (target == null || target.isEmpty()) {
                    cancel();
                    pursuitTasks.remove(goal.getId());
                    return;
                }

                // 解析目标：尝试当作玩家名
                Player targetPlayer = Bukkit.getPlayerExact(target);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    // 玩家离线 60 秒则自动完成目标
                    if (System.currentTimeMillis() - lastOnlineCheck > 60000) {
                        goal.setProgress(100);
                        pursuitTasks.remove(goal.getId());
                        cancel();
                    }
                    return;
                }
                lastOnlineCheck = System.currentTimeMillis();

                double dist = LocationUtil.safeDistance(aiEntity.getLocation(), targetPlayer.getLocation());
                if (dist == Double.MAX_VALUE) return;  // 跨世界

                if (dist > 5) {
                    // 距离远，自动 walk
                    plugin.getCommandExecutor().execute(aiPlayer, "[COMMAND:walk " + target + "]");
                } else if (dist < 4) {
                    // 距离近，自动 attack
                    plugin.getCommandExecutor().execute(aiPlayer, "[COMMAND:attack " + target + "]");
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);  // 立即开始，每 3 秒一次
        pursuitTasks.put(goal.getId(), task);
    }

    public void cancelAllPursuits() {
        for (BukkitTask task : pursuitTasks.values()) {
            task.cancel();
        }
        pursuitTasks.clear();
    }
}
