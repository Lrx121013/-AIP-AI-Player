package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 玩家管理器：负责生成、移除、查询、换皮肤 AI 玩家
 */
public class AIPlayerManager {

    private final AIPlayerPlugin plugin;
    private final Map<String, AIPlayer> aiPlayers = new ConcurrentHashMap<>();
    private BukkitTask autonomousTask;
    private BukkitTask environmentTask;
    /** 每个 NPC 最近一次环境反应的时间戳（ms），避免对同一威胁反复触发 */
    private final Map<UUID, Long> lastEnvReact = new ConcurrentHashMap<>();
    private static final long ENV_REACT_COOLDOWN_MS = 8000;

    public AIPlayerManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 在玩家附近生成 AI 玩家（NPC 玩家实体）
     */
    public AIPlayer spawn(String name, Player spawner) {
        if (aiPlayers.containsKey(name.toLowerCase())) {
            return aiPlayers.get(name.toLowerCase());
        }

        Location loc = spawner.getLocation();
        UUID preferredUuid = UUID.randomUUID();

        // 使用 NpcHelper 创建真正的玩家实体（NPC），返回 Bukkit Player
        Player bukkitPlayer = NpcHelper.createNpc(loc, name, preferredUuid, null);

        // 重要：用实体实际的 UUID 跟踪，因为 Citizens 会忽略 preferredUuid 自行生成 UUID
        UUID actualUuid = bukkitPlayer.getUniqueId();

        // 设置基础属性
        bukkitPlayer.setInvulnerable(plugin.getConfigManager().isInvulnerable());
        bukkitPlayer.setCollidable(true);
        bukkitPlayer.setCanPickupItems(true);

        AIPlayer aiPlayer = new AIPlayer(plugin, name, actualUuid);
        aiPlayers.put(name.toLowerCase(), aiPlayer);

        spawner.sendMessage("§a已生成 AI 玩家: §e" + name + " §7(后端: " + NpcHelper.backendName() + ")");
        return aiPlayer;
    }

    /**
     * 在指定坐标生成 AI 玩家
     */
    public AIPlayer spawnAt(String name, Location loc) {
        if (aiPlayers.containsKey(name.toLowerCase())) {
            return aiPlayers.get(name.toLowerCase());
        }
        UUID preferredUuid = UUID.randomUUID();

        Player bukkitPlayer = NpcHelper.createNpc(loc, name, preferredUuid, null);
        UUID actualUuid = bukkitPlayer.getUniqueId();
        bukkitPlayer.setInvulnerable(plugin.getConfigManager().isInvulnerable());
        bukkitPlayer.setCollidable(true);
        bukkitPlayer.setCanPickupItems(true);

        AIPlayer aiPlayer = new AIPlayer(plugin, name, actualUuid);
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
        Player player = p.getEntity();
        if (player != null && player.isValid()) {
            NpcHelper.removeNpc(player);
        }
        return true;
    }

    public void removeAll() {
        for (AIPlayer p : new java.util.ArrayList<>(aiPlayers.values())) {
            Player player = p.getEntity();
            if (player != null && player.isValid()) {
                NpcHelper.removeNpc(player);
            }
        }
        aiPlayers.clear();
        stopAutonomousTask();
    }

    /**
     * 设置 AI 玩家的皮肤
     *
     * @param name       AI 玩家名称
     * @param skinTexture 皮肤纹理属性（Property 对象）
     * @return 是否成功
     */
    public boolean setSkin(String name, Object skinTexture) {
        AIPlayer p = aiPlayers.get(name.toLowerCase());
        if (p == null) return false;
        Player player = p.getEntity();
        if (player == null || !player.isValid()) return false;
        NpcHelper.updateSkin(player, skinTexture);
        return true;
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
        if (environmentTask != null) {
            environmentTask.cancel();
            environmentTask = null;
        }
    }

    /**
     * 启动环境感知任务（默认每 5 秒检查一次附近威胁/玩家）
     * <p>
     * 这是"即时反应"机制：即使配置里关了 autonomous，只要附近有怪物/玩家靠近，
     * NPC 也会立刻（5 秒内）感知到并询问 LLM 决策。
     */
    public void startEnvironmentTask() {
        if (environmentTask != null) return;
        environmentTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (AIPlayer p : aiPlayers.values()) {
                    try {
                        scanEnvironment(p);
                    } catch (Exception e) {
                        plugin.getLogger().warning("环境感知异常: " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);  // 5 秒一次
    }

    private void triggerAutonomousAction(AIPlayer aiPlayer) {
        if (!plugin.getConfigManager().isConfigured()) return;
        Player v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;

        try {
            GameDataCollector collector = plugin.getGameDataCollector();
            String gameData = collector.collect(aiPlayer);
            final String prompt = "（自主思考）当前游戏数据如下：\n" + gameData
                    + "\n请基于当前情况自主决定下一步操作（保持简短，1-2 个动作即可）。";
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                    String reply = cm.chat(prompt, null);
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

    /**
     * 获取所有 AI 玩家名称
     */
    public java.util.List<String> getAllNames() {
        return aiPlayers.values().stream().map(AIPlayer::getName).toList();
    }

    /**
     * 环境感知：扫描 NPC 附近的威胁（怪物）和靠近的玩家，
     * 若发现值得反应的事件则立即触发 LLM 决策。
     * <p>
     * 不论 autonomous 是否启用，环境感知都启用。
     * 每个 NPC 有 8 秒冷却，避免反复触发同一事件。
     */
    private void scanEnvironment(AIPlayer aiPlayer) {
        if (!plugin.getConfigManager().isConfigured()) return;
        Player v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;

        UUID uid = aiPlayer.getEntityId();
        long now = System.currentTimeMillis();
        Long last = lastEnvReact.get(uid);
        if (last != null && now - last < ENV_REACT_COOLDOWN_MS) return;

        double radius = plugin.getConfigManager().getEntityScanRadius();
        var nearby = v.getNearbyEntities(radius, radius, radius);

        // 检查：是否有怪物靠近（距离 < 6 格）
        org.bukkit.entity.LivingEntity nearestMonster = null;
        double nearestMonsterDist = Double.MAX_VALUE;
        // 检查：是否有玩家靠近（距离 < 8 格，且不是 NPC 自己）
        Player nearestPlayer = null;
        double nearestPlayerDist = Double.MAX_VALUE;

        for (org.bukkit.entity.Entity e : nearby) {
            if (e instanceof org.bukkit.entity.Monster m) {
                double d = m.getLocation().distance(v.getLocation());
                if (d < nearestMonsterDist) {
                    nearestMonsterDist = d;
                    nearestMonster = m;
                }
            } else if (e instanceof Player p && !p.equals(v)) {
                double d = p.getLocation().distance(v.getLocation());
                if (d < nearestPlayerDist) {
                    nearestPlayerDist = d;
                    nearestPlayer = p;
                }
            }
        }

        // 低血量也算紧急事件
        boolean lowHealth = v.getHealth() < 10.0;

        // 是否需要反应？
        boolean shouldReact = false;
        StringBuilder trigger = new StringBuilder();
        if (nearestMonster != null && nearestMonsterDist < 6.0) {
            shouldReact = true;
            trigger.append("（紧急事件：附近的怪物 ").append(nearestMonster.getName())
                    .append(" 距离你仅 ").append(String.format("%.1f", nearestMonsterDist))
                    .append(" 格，威胁很高！）\n");
        }
        if (nearestPlayer != null && nearestPlayerDist < 4.0) {
            shouldReact = true;
            trigger.append("（事件：玩家 ").append(nearestPlayer.getName())
                    .append(" 走到了你身边，距离 ").append(String.format("%.1f", nearestPlayerDist))
                    .append(" 格。你可以打招呼或攻击。）\n");
        }
        if (lowHealth) {
            shouldReact = true;
            trigger.append("（紧急事件：你的血量很低（")
                    .append(String.format("%.1f", v.getHealth()))
                    .append("），需要立刻恢复或逃跑！）\n");
        }

        if (!shouldReact) return;

        lastEnvReact.put(uid, now);

        // 触发 LLM 决策（主线程采集 → 异步 LLM → 主线程执行）
        final Player finalNearestPlayer = nearestPlayer;
        try {
            GameDataCollector collector = plugin.getGameDataCollector();
            String gameData = collector.collect(aiPlayer);
            final String prompt = trigger + "当前游戏数据：\n" + gameData
                    + "\n请立刻做出反应（简短，1-2 个命令）。";
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                    String reply = cm.chat(prompt, finalNearestPlayer);
                    final String finalReply = reply;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("环境反应 LLM 失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("环境反应采集数据失败: " + e.getMessage());
        }
    }
}
