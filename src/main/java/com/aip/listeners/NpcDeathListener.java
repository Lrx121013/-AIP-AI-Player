package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.DeathRecord;
import com.aip.ai.MemoryRecord;
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

        // v2.1.4：死亡瞬间立即清理短期任务，避免复活前还在执行旧任务
        try {
            ai.setLastKillName(null);
            if (ai.getPursuitTask() != null) {
                try {
                    ai.getPursuitTask().cancel();
                } catch (IllegalStateException ignored) {
                }
                ai.setPursuitTask(null);
            }
            if (ai.getMainQuestExecutor() != null) {
                ai.getMainQuestExecutor().cancel();
                ai.setMainQuestExecutor(null);
            }
        } catch (Exception ignored) {
        }

        // P3：长期记忆——记录死亡事件
        // v2.2.1：killer=null 时读 lastDamageCause 转中文死因（如"摔落" / "饥饿" / "环境"）
        String killerNameForMemory;
        String envKillerName = null;
        try {
            org.bukkit.entity.Player killerPlayer = event.getEntity().getKiller();
            if (killerPlayer != null) {
                killerNameForMemory = killerPlayer.getName();
            } else {
                killerNameForMemory = "未知";
                envKillerName = readLastDamageCause(event.getEntity());
            }
        } catch (Exception e) {
            killerNameForMemory = "未知";
            envKillerName = readLastDamageCause(event.getEntity());
        }
        final String finalEnvKillerName = envKillerName;
        ai.getMemory().addRecord(MemoryRecord.Type.DEATH,
                "被 " + killerNameForMemory + " 击杀", killerNameForMemory);

        // v2.2.1：killer=null 时把环境死因也写到 log
        if (finalEnvKillerName != null) {
            plugin.getLogger().info("NPC " + name + " 死亡（环境: " + finalEnvKillerName + "）");
        }

        // 仅移除实体，保留 AIPlayer（功能 7）—— 不调用 aiPlayerManager.remove(name)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                org.bukkit.entity.Player entity = event.getEntity();
                if (entity == null) return;
                if (entity.isValid()) {
                    NpcHelper.removeNpc(entity);
                }
                plugin.getLogger().info("NPC " + name + " 已倒下，实体已移除（可用 /aip revive " + name + " 复活）");
            } catch (Exception e) {
                plugin.getLogger().warning("NPC " + name + " 移除实体失败: " + e.getMessage());
            }
        }, 1L);

        // P2：NPC 死亡后 5 秒（100 tick）自动复活，保留对话历史/个性/记忆
        if (plugin.getConfigManager().isAutoRevive()) {
            final String aiName = name;
            final String finalKiller = killer;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    com.aip.ai.AIPlayer revived = plugin.getAiPlayerManager().revive(aiName);
                    if (revived != null) {
                        plugin.getLogger().info("AI " + aiName + " 已自动复活");
                        // v2.1.4：复活后生成复仇对话（带 killer 名）
                        try {
                            org.bukkit.entity.Player killerPlayer = finalKiller != null
                                    ? Bukkit.getPlayerExact(finalKiller) : null;
                            com.aip.ai.RevengeLine.generateAndSay(
                                    plugin, revived, killerPlayer, 20L);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("AI " + aiName + " 自动复活失败: " + e.getMessage());
                }
            }, 100L);  // 5 秒后复活
        }
    }

    /**
     * v2.2.1：把 {@link org.bukkit.event.entity.EntityDamageEvent#getCause()} 翻译成中文死因。
     * <p>
     * 当 AI 被"环境"杀死（killer == null）时使用，例如摔落、饥饿、窒息、岩浆等。
     * 失败时返回 null，调用方应继续使用"未知"。
     */
    private String readLastDamageCause(org.bukkit.entity.Player entity) {
        try {
            org.bukkit.event.entity.EntityDamageEvent lastDamage = entity.getLastDamageCause();
            if (lastDamage == null) return null;
            org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = lastDamage.getCause();
            if (cause == null) return null;
            return switch (cause) {
                case FALL -> "摔落";
                case FIRE, FIRE_TICK -> "火焰";
                case LAVA -> "岩浆";
                case DROWNING -> "溺水";
                case SUFFOCATION -> "窒息";
                case STARVATION -> "饥饿";
                case VOID -> "虚空";
                case SUICIDE -> "自杀";
                case MAGIC -> "魔法";
                case POISON -> "中毒";
                case WITHER -> "凋零";
                case CRAMMING -> "挤压";
                case FLY_INTO_WALL -> "撞墙";
                case HOT_FLOOR -> "岩浆块";
                case FREEZE -> "冰冻";
                case KILL -> "/kill";
                case WORLD_BORDER -> "世界边界";
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> "实体攻击";
                case PROJECTILE -> "弹射物";
                case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "爆炸";
                case FALLING_BLOCK -> "方块砸";
                case THORNS -> "荆棘";
                case SONIC_BOOM -> "音波";
                default -> "环境(" + cause.name() + ")";
            };
        } catch (Throwable ignored) {
            return null;
        }
    }
}
