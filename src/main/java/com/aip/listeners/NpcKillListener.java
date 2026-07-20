package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.MemoryRecord;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * 监听 AI NPC 击杀玩家事件
 * <p>
 * 当某个 AI 玩家（NPC）击杀真实玩家时：
 *   1. 设置 AI 的 lastKillName，让主线任务 KILL_TARGET 条件可被满足
 *   2. 写入长期记忆（KILL 类型，若枚举无此值则降级为 ATTACK）
 *   3. 微调情绪（+5 击杀奖励）
 * <p>
 * 实现策略：监听 PlayerDeathEvent，通过 victim.getLastDamageCause() 找出伤害来源
 * （支持直接攻击 / 远程 Projectile 两种情形）。
 */
public class NpcKillListener implements Listener {

    private final AIPlayerPlugin plugin;

    public NpcKillListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        String victimName = deadPlayer.getName();

        // 找出 killer：可能是直接攻击 / Projectile / 环境
        UUID killerId = resolveKillerUuid(deadPlayer);
        if (killerId == null) return; // 怪物/环境致死，不处理

        // killer 必须是我们的 AI NPC
        AIPlayer ai = plugin.getAiPlayerManager().getByEntity(killerId);
        if (ai == null) return;

        // 1. 设置 lastKillName，让 KILL_TARGET 主线任务条件能匹配
        ai.setLastKillName(victimName);

        // 2. 记录长期记忆（KILL 类型；当前枚举无 KILL，降级使用 ATTACK，失败则跳过）
        try {
            MemoryRecord.Type killType = MemoryRecord.Type.valueOf("KILL");
            ai.getMemory().addRecord(killType, "杀死了玩家 " + victimName, victimName);
        } catch (Exception e) {
            try {
                ai.getMemory().addRecord(MemoryRecord.Type.ATTACK,
                        "杀死了玩家 " + victimName, victimName);
            } catch (Exception ex) {
                plugin.getLogger().warning("NpcKillListener 记录 memory 失败: " + ex.getMessage());
            }
        }

        // 3. 调整情绪（+5 击杀奖励）
        try {
            ai.adjustMood(5);
        } catch (Exception ignored) {
        }

        plugin.getLogger().info("AI " + ai.getName() + " 杀死了玩家 " + victimName);

        // v2.1.3 故事模式：通知 StoryManager 推进阶段
        try {
            com.aip.story.StoryManager sm = plugin.getStoryManager();
            if (sm != null) {
                sm.onPlayerDeathByAi(ai, deadPlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("NpcKillListener 通知 StoryManager 失败: " + e.getMessage());
        }
    }

    /**
     * 通过 victim.getLastDamageCause() 找出 killer 的 UUID。
     * 仅当 killer 是 Player（含 Projectile 的 shooter 是 Player）时返回。
     *
     * @return killer 的 UUID（必须是 Player），否则 null
     */
    private UUID resolveKillerUuid(Player victim) {
        EntityDamageEvent lastDamage = null;
        try {
            lastDamage = victim.getLastDamageCause();
        } catch (Exception ignored) {
        }
        if (lastDamage == null) return null;

        if (lastDamage instanceof EntityDamageByEntityEvent byEntity) {
            org.bukkit.entity.Entity damager = byEntity.getDamager();
            if (damager == null) return null;
            // 直接攻击：damager 本身就是 Player
            if (damager instanceof Player direct) {
                return direct.getUniqueId();
            }
            // 远程攻击：damager 是 Projectile，取其 shooter
            if (damager instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
                return shooter.getUniqueId();
            }
        }
        return null;
    }
}
