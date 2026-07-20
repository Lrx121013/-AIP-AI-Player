package com.aip.story;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 故事模式阶段动作辅助类
 * <p>
 * 封装 StoryManager 与 CommandExecutor 的交互，
 * 提供阶段切换时的快捷方法：say / runCommand / getNearestPlayer
 */
public class StageAction {

    /**
     * 让 AI 说话（经过去重检查）
     */
    public static void say(AIPlayer ai, String text) {
        if (ai == null || text == null || text.isEmpty()) return;
        ai.sayInChat(text);
    }

    /**
     * 执行 AI 命令（通过 CommandExecutor）
     */
    public static void runCommand(AIPlayer ai, String commandLine) {
        if (ai == null || commandLine == null || commandLine.isEmpty()) return;
        AIPlayerPlugin plugin = ai.getPlugin();
        plugin.getCommandExecutor().execute(ai, "[COMMAND:" + commandLine + "]");
    }

    /**
     * 获取 AI 附近 32 格内最近的在线玩家
     */
    public static Player getNearestPlayer(AIPlayer ai) {
        if (ai == null) return null;
        Player v = ai.getEntity();
        if (v == null || !v.isValid()) return null;
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(v)) continue;
            double d = LocationUtil.safeDistance(p.getLocation(), v.getLocation());
            if (d < 32.0 && d < nearestDist) {
                nearestDist = d;
                nearest = p;
            }
        }
        return nearest;
    }

    /**
     * 广播全服消息
     */
    public static void broadcast(String message) {
        if (message == null) return;
        Bukkit.broadcastMessage(message);
    }
}
