package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.DeathRecord;
import com.aip.ai.NpcHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * 监听 NPC 玩家死亡事件
 * <p>
 * 死亡后：
 *   1. 记录死亡位置到 AIPlayer（供 /aip revive 复活使用，功能 7）
 *   2. 填充死亡日志（功能 10）
 *   3. 仅移除实体，不从 aiPlayers Map 中删除 AIPlayer（保留对话历史 / 个性 / 关系等记忆）
 *      之后可用 /aip revive <name> 重新生成实体。
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

        String name = ai.getName();
        event.setDeathMessage("§7" + name + " 倒下了");

        // 功能 7：记录死亡位置
        ai.setDeathLocation(event.getEntity().getLocation());

        // 功能 10：填充死亡日志
        String cause = "UNKNOWN";
        try {
            if (event.getEntity().getLastDamageCause() != null
                    && event.getEntity().getLastDamageCause().getCause() != null) {
                cause = event.getEntity().getLastDamageCause().getCause().name();
            }
        } catch (Exception ignored) {
        }
        String killer = null;
        try {
            if (event.getEntity().getKiller() != null) {
                killer = event.getEntity().getKiller().getName();
            }
        } catch (Exception ignored) {
        }
        ai.getDeathLog().add(new DeathRecord(System.currentTimeMillis(), cause, killer));

        // 仅移除实体，保留 AIPlayer（功能 7）—— 不调用 aiPlayerManager.remove(name)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                org.bukkit.entity.Player entity = event.getEntity();
                if (entity != null && entity.isValid()) {
                    NpcHelper.removeNpc(entity);
                }
                plugin.getLogger().info("NPC " + name + " 已倒下，实体已移除（可用 /aip revive " + name + " 复活）");
            } catch (Exception e) {
                plugin.getLogger().warning("NPC " + name + " 移除实体失败: " + e.getMessage());
            }
        }, 1L);
    }
}
