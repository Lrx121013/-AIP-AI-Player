package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.AIPlayerManager;
import com.aip.ai.ReflexRule;
import com.aip.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * 反射规则事件监听器：监听伤害 / 玩家攻击 / 方块破坏等事件型触发器
 * <p>
 * 事件发生时延迟 1 tick 调度到主线程，调用 ReflexManager.triggerByEvent
 */
public class ReflexListener implements Listener {

    private final AIPlayerPlugin plugin;

    public ReflexListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!(victim instanceof Player)) return;

        // 判断 victim 是否是 AI 玩家
        AIPlayerManager mgr = plugin.getAiPlayerManager();
        AIPlayer aiPlayer = mgr.getByEntity(victim.getUniqueId());
        if (aiPlayer == null) return;

        // 获取攻击者名
        Entity damager = event.getDamager();
        String attackerName = null;
        if (damager instanceof Player) {
            attackerName = ((Player) damager).getName();
        } else if (damager != null) {
            attackerName = damager.getName();  // 怪物名等
        }

        // 延迟 1 tick 调度到主线程（确保事件处理完成）
        final String finalAttackerName = attackerName;
        final boolean isPlayerAttacker = damager instanceof Player;
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Player entity = aiPlayer.getEntity();
                    if (entity == null || !entity.isValid() || entity.isDead()) return;
                    // 任意伤害触发 ON_DAMAGE
                    aiPlayer.getReflexManager().triggerByEvent(
                            ReflexRule.TriggerType.ON_DAMAGE, finalAttackerName, null, null);
                    // 玩家攻击额外触发 PLAYER_ATTACK
                    if (isPlayerAttacker) {
                        aiPlayer.getReflexManager().triggerByEvent(
                                ReflexRule.TriggerType.PLAYER_ATTACK, finalAttackerName, null, null);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("反射规则触发异常: " + e.getMessage());
                }
            }
        }.runTask(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location breakLoc = event.getBlock().getLocation();
        Player breaker = event.getPlayer();

        // 遍历所有 AI 玩家，检查是否在规则半径内
        Collection<AIPlayer> allAis = plugin.getAiPlayerManager().getAll();
        for (AIPlayer aiPlayer : allAis) {
            Player entity = aiPlayer.getEntity();
            if (entity == null || !entity.isValid()) continue;

            double dist = LocationUtil.safeDistance(entity.getLocation(), breakLoc);
            // 默认检查半径 16 格（足够覆盖常见场景，避免过远触发）
            if (dist > 16) continue;

            // 延迟 1 tick 调度
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Player e = aiPlayer.getEntity();
                        if (e == null || !e.isValid() || e.isDead()) return;
                        aiPlayer.getReflexManager().triggerByEvent(
                                ReflexRule.TriggerType.BLOCK_BREAK_NEARBY, breaker.getName(), null, null);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("反射规则触发异常: " + ex.getMessage());
                    }
                }
            }.runTask(plugin);
        }
    }
}
