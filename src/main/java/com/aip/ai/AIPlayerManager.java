package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 玩家管理器：负责生成、移除、查询 AI 玩家
 */
public class AIPlayerManager {

    private final AIPlayerPlugin plugin;
    private final Map<String, AIPlayer> aiPlayers = new ConcurrentHashMap<>();
    private BukkitTask autonomousTask;

    public AIPlayerManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 在玩家附近生成 AI 玩家
     */
    public AIPlayer spawn(String name, Player spawner) {
        if (aiPlayers.containsKey(name.toLowerCase())) {
            return aiPlayers.get(name.toLowerCase());
        }

        Location loc = spawner.getLocation();

        // 使用村民作为 AI 玩家物理载体
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.VILLAGER, SpawnReason.CUSTOM);
        villager.setCustomName(name);
        villager.setCustomNameVisible(true);
        villager.setProfession(Profession.NONE);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setAI(true);
        villager.setInvulnerable(plugin.getConfigManager().isInvulnerable());
        villager.setCollidable(true);
        villager.setCanPickupItems(true);
        // 禁止交易
        villager.setVillagerExperience(0);
        villager.setRecipes(java.util.Collections.emptyList());

        AIPlayer aiPlayer = new AIPlayer(plugin, name, villager.getUniqueId());
        aiPlayers.put(name.toLowerCase(), aiPlayer);

        spawner.sendMessage("§a已生成 AI 玩家: §e" + name);
        return aiPlayer;
    }

    /**
     * 在指定坐标生成 AI 玩家
     */
    public AIPlayer spawnAt(String name, Location loc) {
        if (aiPlayers.containsKey(name.toLowerCase())) {
            return aiPlayers.get(name.toLowerCase());
        }
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.VILLAGER, SpawnReason.CUSTOM);
        villager.setCustomName(name);
        villager.setCustomNameVisible(true);
        villager.setProfession(Profession.NONE);
        villager.setAI(true);
        villager.setInvulnerable(plugin.getConfigManager().isInvulnerable());
        villager.setCollidable(true);
        villager.setCanPickupItems(true);
        villager.setRecipes(java.util.Collections.emptyList());

        AIPlayer aiPlayer = new AIPlayer(plugin, name, villager.getUniqueId());
        aiPlayers.put(name.toLowerCase(), aiPlayer);
        return aiPlayer;
    }

    public AIPlayer get(String name) {
        return aiPlayers.get(name.toLowerCase());
    }

    public AIPlayer getByEntity(UUID entityId) {
        for (AIPlayer p : aiPlayers.values()) {
            if (p.getEntityId().equals(entityId)) return p;
        }
        return null;
    }

    public Collection<AIPlayer> getAll() {
        return aiPlayers.values();
    }

    public boolean remove(String name) {
        AIPlayer p = aiPlayers.remove(name.toLowerCase());
        if (p == null) return false;
        Villager v = p.getEntity();
        if (v != null && v.isValid()) {
            v.remove();
        }
        return true;
    }

    public void removeAll() {
        for (AIPlayer p : new java.util.ArrayList<>(aiPlayers.values())) {
            Villager v = p.getEntity();
            if (v != null && v.isValid()) {
                v.remove();
            }
        }
        aiPlayers.clear();
        stopAutonomousTask();
    }

    /**
     * 启动自动活动任务（无玩家@对话时让 AI 自主活动）
     */
    public void startAutonomousTask() {
        if (autonomousTask != null) return;
        int intervalTicks = plugin.getConfigManager().getAutonomousInterval() * 20;
        autonomousTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (AIPlayer p : aiPlayers.values()) {
                    try {
                        triggerAutonomousAction(p);
                    } catch (Exception e) {
                        plugin.getLogger().warning("自动活动异常: " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void stopAutonomousTask() {
        if (autonomousTask != null) {
            autonomousTask.cancel();
            autonomousTask = null;
        }
    }

    private void triggerAutonomousAction(AIPlayer aiPlayer) {
        // 本方法由 runTaskTimer 调用，已在主线程
        if (!plugin.getConfigManager().isConfigured()) return;
        Villager v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;

        // 先在主线程采集游戏数据，再异步调用 LLM（避免异步访问实体）
        try {
            GameDataCollector collector = plugin.getGameDataCollector();
            String gameData = collector.collect(aiPlayer);
            final String prompt = "（自主思考）当前游戏数据如下：\n" + gameData
                    + "\n请基于当前情况自主决定下一步操作（保持简短，1-2 个动作即可）。";
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                    String reply = cm.chat(prompt, null);
                    // 命令执行必须在主线程
                    final String finalReply = reply;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("AI 自主活动失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("AI 自主活动采集数据失败: " + e.getMessage());
        }
    }
}
