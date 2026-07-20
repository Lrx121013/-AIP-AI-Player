package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * 监听 AI NPC 死亡事件
 * <p>
 * 当 AI 死亡时：
 *  1. 调用 StoryManager.onAiDeath 推进故事阶段
 *  2. 广播击杀消息（带死亡计数）
 * <p>
 * 注：实际 NPC 实体的销毁由 NpcDeathListener 处理，
 * 本类专注于"AI 死亡计数 + 故事阶段推进"。
 */
public class AiDeathListener implements Listener {

    private final AIPlayerPlugin plugin;

    public AiDeathListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAiDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        AIPlayer ai = plugin.getAiPlayerManager().getByEntity(dead.getUniqueId());
        if (ai == null) return;  // 不是我们的 AI NPC

        // 找到 killer
        Player killer = dead.getKiller();
        if (killer == null && dead.getLastDamageCause() != null
                && dead.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player p) {
                killer = p;
            }
        }

        // 调用 StoryManager 推进
        try {
            plugin.getStoryManager().onAiDeath(ai, killer);
        } catch (Exception e) {
            plugin.getLogger().warning("AiDeathListener 通知 StoryManager 失败: " + e.getMessage());
        }

        // 广播击杀消息（带死亡计数）
        com.aip.story.StoryState state = ai.getStoryState();
        if (state != null) {
            String killerName = killer != null ? killer.getName() : "未知";
            int deathCount = state.getAiDeathCount();
            if (deathCount > 0) {
                Bukkit.broadcastMessage("§7[击杀] §e" + killerName + " §7击杀了 §c" + ai.getName()
                        + " §7（死亡次数：§f" + deathCount + "§7）");
            }
        }
    }
}
