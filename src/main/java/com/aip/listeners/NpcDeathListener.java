package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * 监听 NPC 玩家死亡事件
 * <p>
 * NPC 是真玩家实体，死亡后会进入"死亡屏幕"等待点击重生按钮，
 * 但 NPC 没有客户端，会一直躺在地上。
 * 这里在死亡后立即调度一个主线程任务把它重生（保留在 AIPlayerManager 中）。
 */
public class NpcDeathListener implements Listener {

    private final AIPlayerPlugin plugin;

    public NpcDeathListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID dead = event.getEntity().getUniqueId();
        AIPlayer ai = plugin.getAiPlayerManager().getByEntity(dead);
        if (ai == null) return; // 不是我们的 NPC

        // 标记死亡消息为空，避免刷屏
        String name = ai.getName();
        event.setDeathMessage("§7" + name + " 倒下了");

        // 1 tick 后在主线程强制重生
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                // 调用 Spigot 的 respawn 方法（Paper 1.21+ 仍可用）
                event.getEntity().spigot().respawn();
                plugin.getLogger().info("NPC " + name + " 已重生");
            } catch (Exception e) {
                plugin.getLogger().warning("NPC " + name + " 重生失败: " + e.getMessage());
                // 重生失败就移除这个 NPC
                plugin.getAiPlayerManager().remove(name);
            }
        }, 1L);
    }
}
