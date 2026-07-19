package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 长期任务管理器
 * <p>
 * 周期性（默认 60 秒）执行指派给 AI 的长期任务，包括：
 *   gather    - 捡起附近掉落物
 *   patrol    - 朝北巡逻 5 格
 *   build     - 在脚下放置石头
 *   farm      - 挖掉脚下方块
 *   escort    - 跟随最近的玩家
 *   siege     - 围攻协同目标玩家（P4）
 *   sabotage  - 破坏附近方块（P4）
 *   infiltrate- 渗透到协同目标附近但不攻击（P4）
 */
public class TaskManager {

    private final AIPlayerPlugin plugin;
    /** aiName -> taskType */
    private final Map<String, String> aiTasks = new ConcurrentHashMap<>();
    private BukkitTask task;

    public TaskManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void assign(String aiName, String taskType) {
        aiTasks.put(aiName, taskType);
    }

    public void cancel(String aiName) {
        aiTasks.remove(aiName);
    }

    public Map<String, String> getAll() {
        return aiTasks;
    }

    /** 启动周期任务（60 秒一次） */
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::executeAll, 0L, 1200L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void executeAll() {
        for (Map.Entry<String, String> entry : aiTasks.entrySet()) {
            String aiName = entry.getKey();
            String type = entry.getValue();
            AIPlayer ai = plugin.getAiPlayerManager().get(aiName);
            if (ai == null) continue;
            try {
                executeTask(ai, type);
            } catch (Exception e) {
                plugin.getLogger().warning("长期任务执行失败 [" + aiName + "/" + type + "]: " + e.getMessage());
            }
        }
    }

    private void executeTask(AIPlayer ai, String type) {
        switch (type) {
            case "gather" -> plugin.getCommandExecutor().execute(ai, "[COMMAND:pickup]");
            case "patrol" -> plugin.getCommandExecutor().execute(ai, "[COMMAND:walk_dir north 5]");
            case "build" -> {
                // 在脚下方块的位置放石头
                Player p = ai.getEntity();
                if (p != null) {
                    Location loc = p.getLocation().clone().subtract(0, 1, 0);
                    plugin.getCommandExecutor().execute(ai,
                            "[COMMAND:place " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " stone]");
                }
            }
            case "farm" -> {
                // 挖掉脚下方块
                Player p = ai.getEntity();
                if (p != null) {
                    Location loc = p.getLocation().clone().subtract(0, 1, 0);
                    plugin.getCommandExecutor().execute(ai,
                            "[COMMAND:break " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "]");
                }
            }
            case "escort" -> {
                // 跟随最近的玩家
                Player p = ai.getEntity();
                if (p == null) return;
                Player nearest = null;
                double minDist = Double.MAX_VALUE;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(p)) continue;
                    double d;
                    try {
                        d = online.getLocation().distance(p.getLocation());
                    } catch (Exception e) {
                        continue;
                    }
                    if (d < minDist) {
                        minDist = d;
                        nearest = online;
                    }
                }
                if (nearest != null) {
                    plugin.getCommandExecutor().execute(ai, "[COMMAND:follow " + nearest.getName() + "]");
                }
            }
            // P4：协同作战任务类型
            case "siege" -> {
                // 围攻：走向协同目标玩家并连续攻击
                String target = plugin.getTeamManager().getCoordinationTarget();
                if (target != null) {
                    plugin.getCommandExecutor().execute(ai, "[COMMAND:approach " + target + "]");
                    plugin.getCommandExecutor().execute(ai, "[COMMAND:attack " + target + "]");
                }
            }
            case "sabotage" -> {
                // 破坏：破坏附近方块（脚下方块）
                Player p = ai.getEntity();
                if (p != null) {
                    Location loc = p.getLocation();
                    plugin.getCommandExecutor().execute(ai, "[COMMAND:break " + loc.getBlockX() + " " + (int) loc.getY() + " " + loc.getBlockZ() + "]");
                }
            }
            case "infiltrate" -> {
                // 渗透：走向协同目标玩家附近但不攻击
                String target = plugin.getTeamManager().getCoordinationTarget();
                if (target != null) {
                    plugin.getCommandExecutor().execute(ai, "[COMMAND:approach " + target + "]");
                }
            }
            default -> plugin.getLogger().warning("未知任务类型: " + type);
        }
    }
}
