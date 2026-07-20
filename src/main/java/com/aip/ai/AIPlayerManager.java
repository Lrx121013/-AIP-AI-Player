package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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
    private BukkitTask idleWalkTask;
    private BukkitTask facePlayerTask;
    private BukkitTask stuckCheckTask;
    /** v2.2.0：AI 空闲自言自语调度器 */
    private BukkitTask monologueTask;
    /** v2.2.1：AI 虚空保护 + 饱食度补充任务（每 1 秒扫描一次） */
    private BukkitTask voidGuardTask;
    /** 每个 NPC 最近一次环境反应的时间戳（ms），避免对同一威胁反复触发 */
    private final Map<UUID, Long> lastEnvReact = new ConcurrentHashMap<>();
    /** 每个 NPC 已打招呼的玩家名集合（避免对同一玩家反复打招呼） */
    private final Map<UUID, java.util.Set<String>> greetedPlayers = new ConcurrentHashMap<>();
    /** 每个 NPC 最近一次自动转头看玩家的时间戳（ms），2 秒最多转一次 */
    private final Map<UUID, Long> lastFacePlayer = new ConcurrentHashMap<>();
    /** v2.1.4：每个 AI 最近一次生成复仇对话的时间戳（ms），5 秒节流 */
    private final Map<String, Long> lastRevengeTime = new ConcurrentHashMap<>();
    /** v2.2.1：每个 AI 最近一次 fallback teleport 时间戳（ms），1.5 秒内不重复 */
    private final Map<UUID, Long> lastTeleportFallback = new ConcurrentHashMap<>();
    /** v2.2.1：每个 AI 连续 navigateTo 失败次数（≥3 时进入 5 秒静默期） */
    private final Map<UUID, Integer> consecutiveFails = new ConcurrentHashMap<>();

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
        // 赋予 OP 权限，使 NPC 能通过 Bukkit.dispatchCommand 执行服务器命令（gamemode、tp 等）
        bukkitPlayer.setOp(true);

        AIPlayer aiPlayer = new AIPlayer(plugin, name, actualUuid);
        aiPlayers.put(name.toLowerCase(), aiPlayer);

        // P2：反派模式开启时，新生成的 AI 也强制 VILLAIN 人格
        // v2.1.3：故事模式开启时，同样强制 VILLAIN 人格（替代 villain-mode）
        if (plugin.getConfigManager().isStoryMode() || plugin.getConfigManager().isVillainMode()) {
            aiPlayer.setOriginalPersonality(aiPlayer.getPersonality());
            aiPlayer.setPersonality(Personality.VILLAIN);
        }

        // 启动反射规则周期检查任务（幂等：已启动则不重复）
        aiPlayer.getReflexManager().startCheckTask();

        spawner.sendMessage("§a已生成 AI 玩家: §e" + name + " §7(后端: " + NpcHelper.backendName() + ")");
        // 清理 GameDataCollector 旧缓存，避免拿到旧实体的数据
        plugin.getGameDataCollector().invalidateCache(actualUuid);
        // v2.1.3：注册故事模式状态
        try {
            if (plugin.getStoryManager() != null) {
                plugin.getStoryManager().registerStory(aiPlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("StoryManager.registerStory 失败: " + e.getMessage());
        }
        // v2.1.3：邪恶 AI 改走 StoryManager 流程，不再启动 MainQuestExecutor
        if (plugin.getConfigManager().isStoryMode()
                && plugin.getStoryManager() != null
                && plugin.getStoryManager().isStoryEligible(aiPlayer)) {
            // 不调用 bindMainQuest，邪恶 AI 走故事模式
        } else {
            bindMainQuest(aiPlayer);
        }
        scheduleIntroLine(aiPlayer);
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
        // 赋予 OP 权限，使 NPC 能通过 Bukkit.dispatchCommand 执行服务器命令（gamemode、tp 等）
        bukkitPlayer.setOp(true);

        AIPlayer aiPlayer = new AIPlayer(plugin, name, actualUuid);
        aiPlayers.put(name.toLowerCase(), aiPlayer);

        // P2：反派模式开启时，新生成的 AI 也强制 VILLAIN 人格
        // v2.1.3：故事模式开启时，同样强制 VILLAIN 人格（替代 villain-mode）
        if (plugin.getConfigManager().isStoryMode() || plugin.getConfigManager().isVillainMode()) {
            aiPlayer.setOriginalPersonality(aiPlayer.getPersonality());
            aiPlayer.setPersonality(Personality.VILLAIN);
        }
        // 启动反射规则周期检查任务（幂等：已启动则不重复）
        aiPlayer.getReflexManager().startCheckTask();
        // 清理 GameDataCollector 旧缓存，避免拿到旧实体的数据
        plugin.getGameDataCollector().invalidateCache(actualUuid);
        // v2.1.3：注册故事模式状态
        try {
            if (plugin.getStoryManager() != null) {
                plugin.getStoryManager().registerStory(aiPlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("StoryManager.registerStory 失败: " + e.getMessage());
        }
        // v2.1.3：邪恶 AI 改走 StoryManager 流程
        if (plugin.getConfigManager().isStoryMode()
                && plugin.getStoryManager() != null
                && plugin.getStoryManager().isStoryEligible(aiPlayer)) {
            // 不调用 bindMainQuest
        } else {
            bindMainQuest(aiPlayer);
        }
        scheduleIntroLine(aiPlayer);
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
        // 清理 GameDataCollector 缓存，避免后续读到已移除实体的数据
        plugin.getGameDataCollector().invalidateCache(p.getEntityId());
        // 取消该 AI 所有未完成的追击任务，避免任务结束后操作已移除的实体
        p.getGoalManager().cancelAllPursuits();
        // 取消反射规则周期检查任务并清空规则列表，避免任务操作已移除的实体
        p.getReflexManager().cancel();
        // 取消被攻击后的追击任务
        if (p.getPursuitTask() != null) {
            p.getPursuitTask().cancel();
            p.setPursuitTask(null);
        }
        // 取消主线任务执行器
        if (p.getMainQuestExecutor() != null) {
            p.getMainQuestExecutor().cancel();
            p.setMainQuestExecutor(null);
        }
        // v2.1.3：注销故事模式状态
        try {
            if (plugin.getStoryManager() != null) {
                plugin.getStoryManager().unregisterStory(p.getEntityId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("StoryManager.unregisterStory 失败: " + e.getMessage());
        }
        Player player = p.getEntity();
        if (player != null && player.isValid()) {
            NpcHelper.removeNpc(player);
        }
        return true;
    }

    /**
     * 复活已死亡的 AI 玩家（功能 7）
     * <p>
     * 在 deathLocation（或世界出生点）重新生成实体，更新 AIPlayer 的 entityId，
     * 并恢复血量等基础属性。对话历史、个性、情绪、关系等记忆均保留。
     *
     * @return 复活后的 AIPlayer，失败返回 null
     */
    public AIPlayer revive(String name) {
        AIPlayer p = aiPlayers.get(name.toLowerCase());
        if (p == null) return null;

        // 如果实体还活着，无需复活
        Player existing = p.getEntity();
        if (existing != null && existing.isValid()) {
            return p;
        }

        // 确定复活位置：优先死亡位置，否则默认世界出生点
        Location loc = p.getDeathLocation();
        if (loc == null || loc.getWorld() == null) {
            loc = plugin.getServer().getWorlds().isEmpty()
                    ? null : plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        if (loc == null) return null;

        UUID preferredUuid = UUID.randomUUID();
        Player bukkitPlayer = NpcHelper.createNpc(loc, name, preferredUuid, null);
        UUID actualUuid = bukkitPlayer.getUniqueId();

        // 恢复基础属性
        bukkitPlayer.setInvulnerable(plugin.getConfigManager().isInvulnerable());
        bukkitPlayer.setCollidable(true);
        bukkitPlayer.setCanPickupItems(true);
        // 赋予 OP 权限，使 NPC 能通过 Bukkit.dispatchCommand 执行服务器命令（gamemode、tp 等）
        bukkitPlayer.setOp(true);
        try {
            bukkitPlayer.setHealth(20.0);
        } catch (Exception ignored) {
        }
        try {
            bukkitPlayer.setFoodLevel(20);
        } catch (Exception ignored) {
        }

        // 更新 AIPlayer 的 entityId 指向新实体（先记住旧 id，留给 StoryState 迁移用）
        UUID oldEntityId = p.getEntityId();
        p.setEntityId(actualUuid);
        // v2.2.1：觉醒切模式 deferred —— 复活完成后再执行
        try {
            com.aip.story.StoryState state = plugin.getStoryManager() != null
                    ? plugin.getStoryManager().getState(oldEntityId) : null;
            if (state != null && state.isAwakeningPending()) {
                final String killerName = state.getPendingKillerName();
                final com.aip.story.StoryState finalState = state;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        if (killerName != null && !killerName.isEmpty()) {
                            Player killerPlayer = Bukkit.getPlayerExact(killerName);
                            if (killerPlayer != null && killerPlayer.isOnline()) {
                                killerPlayer.setGameMode(org.bukkit.GameMode.SURVIVAL);
                                plugin.getLogger().info("[Story] 强制玩家 " + killerName + " 切回生存模式");
                            }
                        }
                        Player entity = p.getEntity();
                        if (entity != null) {
                            entity.setGameMode(org.bukkit.GameMode.CREATIVE);
                            entity.setAllowFlight(true);
                            entity.setFlying(true);
                        }
                        p.sayInChat("现在，让我来控制战场！");
                        plugin.getLogger().info("[Story] AI " + p.getName() + " 觉醒切模式：creative + fly + 强制玩家生存");
                    } catch (Exception e) {
                        plugin.getLogger().warning("觉醒 deferred 切模式失败: " + e.getMessage());
                    } finally {
                        finalState.setAwakeningPending(false);
                    }
                }, 20L);  // 1 秒延迟（让 NPC 完全 spawn）
            }
        } catch (Exception e) {
            plugin.getLogger().warning("觉醒 deferred 检查失败: " + e.getMessage());
        }
        // 清除死亡位置（已复活）
        p.setDeathLocation(null);
        // 清理 GameDataCollector 旧缓存，确保下次采集使用新实体
        plugin.getGameDataCollector().invalidateCache(actualUuid);
        // 启动反射规则周期检查任务（幂等：若之前已被 cancel，此处会重新启动）
        // revive 时清空旧规则，让 AI 根据新环境重新定义（避免脏状态）
        p.getReflexManager().clearRules();
        p.getReflexManager().startCheckTask();
        // 复活前先清理旧状态：lastKillName / pursuitTask / 旧 mainQuestExecutor
        p.setLastKillName(null);
        if (p.getPursuitTask() != null) {
            try {
                p.getPursuitTask().cancel();
            } catch (IllegalStateException ignored) {
            }
            p.setPursuitTask(null);
        }
        if (p.getMainQuestExecutor() != null) {
            p.getMainQuestExecutor().cancel();
            p.setMainQuestExecutor(null);
        }
        // 清理旧 mainQuest 引用
        p.setMainQuest(null);
        // v2.1.4：保留故事模式状态跨死亡（NPC 实体 UUID 变了但剧情进度必须保留）
        // 旧逻辑是 unregister + register，会把 aiDeathCount 重置为 0 → 死亡次数永远是 1
        try {
            if (plugin.getStoryManager() != null) {
                // 用旧 entityId 查 states（oldEntityId 还是 register 时的 key）
                com.aip.story.StoryState state = plugin.getStoryManager().getState(oldEntityId);
                if (state != null) {
                    // 复用现有 StoryState：把 states map key 从 oldId 改为 newId
                    // 同时把 StoryState.ownerId 也改成 newId
                    plugin.getStoryManager().rebindOwner(oldEntityId, actualUuid);
                    state.reviveRebind(actualUuid);
                } else {
                    // 之前没注册过（罕见，比如外部 remove 后 revive），重新注册
                    plugin.getStoryManager().registerStory(p);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("StoryManager 复活重绑失败: " + e.getMessage());
        }

        // 复活后重新绑定主线任务（仅非故事模式 AI）
        if (plugin.getConfigManager().isStoryMode()
                && plugin.getStoryManager() != null
                && plugin.getStoryManager().isStoryEligible(p)) {
            // 不调用 bindMainQuest
        } else {
            bindMainQuest(p);
        }
        // v2.1.4：复活走"复仇对话"路径，不再调"我刚刚被生成"的开场白
        scheduleRevengeLine(p);
        plugin.getLogger().info("AI " + name + " 复活成功");
        return p;
    }

    public void removeAll() {
        for (AIPlayer p : new java.util.ArrayList<>(aiPlayers.values())) {
            Player player = p.getEntity();
            if (player != null && player.isValid()) {
                NpcHelper.removeNpc(player);
            }
            // 取消主线任务执行器
            if (p.getMainQuestExecutor() != null) {
                p.getMainQuestExecutor().cancel();
                p.setMainQuestExecutor(null);
            }
            // 取消追击任务
            if (p.getPursuitTask() != null) {
                try {
                    p.getPursuitTask().cancel();
                } catch (IllegalStateException ignored) {
                }
                p.setPursuitTask(null);
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
        if (idleWalkTask != null) {
            idleWalkTask.cancel();
            idleWalkTask = null;
        }
        if (facePlayerTask != null) {
            facePlayerTask.cancel();
            facePlayerTask = null;
        }
        if (stuckCheckTask != null) {
            stuckCheckTask.cancel();
            stuckCheckTask = null;
        }
        if (monologueTask != null) {
            monologueTask.cancel();
            monologueTask = null;
        }
        // v2.2.1：虚空保护任务统一在 stopAutonomousTask 末尾清理
        stopVoidGuardTask();
    }

    /**
     * v2.2.0：启动 AI 空闲自言自语调度器。
     * <p>
     * 周期 5 秒（100 tick）扫描所有 AI，对符合条件的 AI 触发一句内心独白（OS）广播。
     * 幂等：已启动则不重复。
     */
    public void startMonologueTask() {
        if (monologueTask != null) return;
        monologueTask = new IdleMonologueTask(plugin).runTaskTimer(plugin, 100L, 100L);
    }

    /**
     * v2.2.1：启动虚空保护 + 饱食度补充任务。
     * <p>
     * 每 1 秒扫描所有 AI：
     *   - y &lt; 0 时 teleport 回主世界出生点（避免无限掉到 y=0 触发环境死亡）
     *   - 饱食度 &lt; 6 时 setFoodLevel(20)（避免饿死）
     * <p>
     * 幂等：已启动则不重复。死亡时（entity 无效）自动跳过，不影响死亡处理。
     */
    public void startVoidGuardTask() {
        if (voidGuardTask != null) return;
        voidGuardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                for (AIPlayer aiPlayer : aiPlayers.values()) {
                    try {
                        Player ent = aiPlayer.getEntity();
                        if (ent == null || !ent.isValid()) continue;
                        // y < 0 时 teleport 回出生点
                        if (ent.getLocation().getY() < 0) {
                            Location spawn = ent.getWorld().getSpawnLocation();
                            ent.teleport(spawn);
                            plugin.getLogger().warning("[Story] AI " + aiPlayer.getName() + " 掉入虚空，已传送回出生点");
                        }
                        // 饱食度 < 6 时补充
                        if (ent.getFoodLevel() < 6) {
                            ent.setFoodLevel(20);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("虚空保护扫描单个 AI 失败 [" + aiPlayer.getName() + "]: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("虚空保护扫描失败: " + e.getMessage());
            }
        }, 20L, 20L);  // 1 秒后开始，每 1 秒
    }

    /**
     * v2.2.1：停止虚空保护任务。
     */
    public void stopVoidGuardTask() {
        if (voidGuardTask != null) {
            voidGuardTask.cancel();
            voidGuardTask = null;
        }
    }

    /**
     * v2.2.1：让 AI 寻路到指定位置（带连续失败冷却）。
     * <p>
     * 优先调用 {@link NpcHelper#navigateTo(Player, Location, double)}（Citizens A* 寻路）。
     * 若 Citizens 不可用或抛异常则返回 false，由调用方决定 fallback。
     * <p>
     * 冷却策略：
     *   - 单次成功：清零连续失败计数
     *   - 单次失败：连续失败 +1
     *   - 连续失败 ≥3 次：进入 5 秒静默期，静默期内直接返回 false 不再尝试
     *   - 静默期结束：自动重置，连续失败归零
     *
     * @return true=Citizens 寻路成功；false=失败或处于静默期
     */
    public boolean navigateTo(AIPlayer ai, Location target) {
        if (ai == null || target == null) return false;
        Player entity = ai.getEntity();
        if (entity == null || !entity.isValid()) return false;
        UUID id = ai.getEntityId();
        if (id == null) return false;

        double speed = plugin.getConfigManager().getMoveSpeed();

        // v2.2.1：连续失败 ≥3 次后，5 秒内不再 navigateTo
        Integer fails = consecutiveFails.get(id);
        if (fails != null && fails >= 3) {
            long lastFail = lastTeleportFallback.getOrDefault(id, 0L);
            if (System.currentTimeMillis() - lastFail < 5000L) {
                return false;  // 静默期内，直接跳过
            }
            // 5 秒已过，重置计数
            consecutiveFails.put(id, 0);
        }

        // 调 Citizens 的 navigateTo
        try {
            boolean success = NpcHelper.navigateTo(entity, target, speed);
            if (success) {
                // 成功：清零连续失败
                consecutiveFails.put(id, 0);
                lastTeleportFallback.remove(id);
                return true;
            }
        } catch (Exception ignored) {
            // Citizens API 抛异常 = 失败，落到下面 fallback
        }

        // 失败：连续失败 +1
        int failCount = consecutiveFails.getOrDefault(id, 0) + 1;
        consecutiveFails.put(id, failCount);
        return false;
    }

    /**
     * v2.2.1：fallback 分帧 teleport 模拟行走（带 1.5 秒冷却）。
     * <p>
     * 当 {@link #navigateTo(AIPlayer, Location)} 返回 false 时由调用方（如
     * {@link com.aip.ai.CommandExecutor#walkTo}）调用。
     * <p>
     * 冷却策略：单个 AI 1.5 秒内不重复 fallback teleport（避免短时间反复瞬移）。
     * 静默期由 {@link #navigateTo} 的"连续 3 次失败 5 秒"机制独立处理。
     *
     * @return true=实际执行了 fallback teleport；false=处于 1.5 秒冷却内，跳过
     */
    public boolean tryFallbackTeleport(AIPlayer ai, Location target, double totalDist, double speed) {
        if (ai == null || target == null) return false;
        Player entity = ai.getEntity();
        if (entity == null || !entity.isValid()) return false;
        UUID id = ai.getEntityId();
        if (id == null) return false;

        // 1.5 秒内不重复 fallback teleport
        long now = System.currentTimeMillis();
        long last = lastTeleportFallback.getOrDefault(id, 0L);
        if (now - last < 1500L) {
            return false;
        }
        lastTeleportFallback.put(id, now);

        plugin.getLogger().warning("[AIPlayer] Citizens navigateTo 失败，回退到 teleport 模拟行走: "
                + ai.getName() + " -> " + target);

        // 距离太近直接 teleport
        if (totalDist < 0.5) {
            entity.teleport(target);
            return true;
        }
        // 分帧 teleport 模拟"行走"
        Location start = entity.getLocation();
        int steps = Math.max(1, (int) Math.min(60, Math.ceil(totalDist / speed)));
        Vector step = target.toVector().subtract(start.toVector()).multiply(1.0 / steps);
        final int finalSteps = steps;
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= finalSteps || !entity.isValid()) {
                    cancel();
                    return;
                }
                Location next = entity.getLocation().add(step);
                // 保留地面高度（避免穿墙下降）
                next.setY(entity.getLocation().getY());
                entity.teleport(next);
                i++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
        return true;
    }

    /**
     * v2.2.1：清空指定 AI 的 navigateTo 冷却计数（死亡/复活时调用，避免脏状态）。
     */
    public void clearNavigateCooldown(UUID aiEntityId) {
        if (aiEntityId == null) return;
        lastTeleportFallback.remove(aiEntityId);
        consecutiveFails.remove(aiEntityId);
    }

    /**
     * 启动环境感知任务（默认每 5 秒检查一次附近威胁/玩家）
     * <p>
     * 这是"即时反应"机制：即使配置里关了 autonomous，只要附近有怪物/玩家靠近，
     * NPC 也会立刻（5 秒内）感知到并询问 LLM 决策。
     */
    public void startEnvironmentTask() {
        if (environmentTask != null) return;
        int ticks = plugin.getConfigManager().getEnvScanInterval();
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
        }.runTaskTimer(plugin, ticks, ticks);
    }

    /**
     * 启动空闲漫游任务：每隔 idle-walk-interval 秒随机走动一次（不调 LLM）。
     * <p>
     * 解决 AI"原地不动"问题：即使 LLM 不主动调用 walk，NPC 也会在 idle-walk-radius
     * 半径内随机走动，看起来更像真人。
     */
    public void startIdleWalkTask() {
        if (idleWalkTask != null) return;
        int intervalTicks = plugin.getConfigManager().getIdleWalkInterval() * 20;
        idleWalkTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (AIPlayer p : aiPlayers.values()) {
                    try {
                        idleWalk(p);
                    } catch (Exception e) {
                        plugin.getLogger().warning("空闲漫游异常: " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * 启动自动转头任务：每 2 秒检查一次附近玩家，玩家靠近 4 格内时自动转头看玩家（不调 LLM）。
     * <p>
     * 让 NPC 在静止时也显得"有反应"——玩家走近时它会转头看，提升拟人感。
     */
    public void startFacePlayerTask() {
        if (facePlayerTask != null) return;
        int faceIntervalTicks = 40;  // 2 秒
        facePlayerTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (AIPlayer p : aiPlayers.values()) {
                    try {
                        faceNearbyPlayer(p);
                    } catch (Exception e) {
                        plugin.getLogger().warning("转头异常: " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(plugin, faceIntervalTicks, faceIntervalTicks);
    }

    /**
     * 启动卡住检查任务：每 stuck-check-interval 秒检查每个 AI 的位置变化。
     * 若连续 stuck-threshold-ms 未移动且位置变化 < 1 格，强制触发 idleWalk。
     */
    public void startStuckCheckTask() {
        if (stuckCheckTask != null) return;
        int intervalTicks = plugin.getConfigManager().getStuckCheckInterval();
        stuckCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (AIPlayer p : aiPlayers.values()) {
                    try {
                        stuckCheck(p);
                    } catch (Exception e) {
                        plugin.getLogger().warning("卡住检查异常: " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * 检查单个 AI 是否卡住，若卡住则强制移动。
     */
    private void stuckCheck(AIPlayer aiPlayer) {
        Player v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;
        // 正在 LLM 决策、寻路或跟随时，刷新时间不触发
        if (aiPlayer.getBusy().get()) {
            aiPlayer.setLastMoveTime(System.currentTimeMillis());
            aiPlayer.setLastMoveLoc(v.getLocation());
            return;
        }
        if (NpcHelper.isNavigating(v)) {
            aiPlayer.setLastMoveTime(System.currentTimeMillis());
            aiPlayer.setLastMoveLoc(v.getLocation());
            return;
        }
        if (aiPlayer.getFollowing() != null) {
            aiPlayer.setLastMoveTime(System.currentTimeMillis());
            aiPlayer.setLastMoveLoc(v.getLocation());
            return;
        }

        long now = System.currentTimeMillis();
        long lastMove = aiPlayer.getLastMoveTime();
        // 首次记录：初始化并返回
        if (lastMove == 0) {
            aiPlayer.setLastMoveTime(now);
            aiPlayer.setLastMoveLoc(v.getLocation());
            return;
        }

        long elapsed = now - lastMove;
        Location lastLoc = aiPlayer.getLastMoveLoc();
        Location curLoc = v.getLocation();
        double moved = (lastLoc == null || lastLoc.getWorld() == null || !lastLoc.getWorld().equals(curLoc.getWorld()))
                ? Double.MAX_VALUE : lastLoc.distance(curLoc);

        if (elapsed >= plugin.getConfigManager().getStuckThresholdMs() && moved < 1.0) {
            // 卡住超过阈值且未移动，强制 idleWalk
            idleWalk(aiPlayer);
            aiPlayer.setLastMoveTime(now);
            aiPlayer.setLastMoveLoc(v.getLocation());
        } else if (moved >= 1.0) {
            // 实际移动了，刷新时间
            aiPlayer.setLastMoveTime(now);
            aiPlayer.setLastMoveLoc(v.getLocation());
        }
        // 否则保持现状（未到阈值）
    }

    /**
     * 空闲漫游：在 idle-walk-radius 半径内随机选一个目标点，直接调 Citizens navigateTo 走过去。
     * 不调用 LLM，不与反射规则冲突（正在忙/正在寻路/正在跟随时跳过）。
     */
    private void idleWalk(AIPlayer aiPlayer) {
        Player v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;
        // 正在 LLM 决策或正在寻路中，跳过
        if (aiPlayer.getBusy().get()) return;
        if (NpcHelper.isNavigating(v)) return;
        // 跟随玩家中，跳过
        if (aiPlayer.getFollowing() != null) return;

        int radius = plugin.getConfigManager().getIdleWalkRadius();
        Location loc = v.getLocation();
        double angle = Math.random() * Math.PI * 2;
        // 距离 5 到 radius 格之间随机
        double maxDist = Math.max(5.0, (double) radius);
        double dist = 5.0 + Math.random() * (maxDist - 5.0);
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;
        Location target = loc.clone().add(dx, 0, dz);
        // 保持当前 Y（避免穿墙下降或飞起来）
        target.setY(loc.getY());

        // 直接调 Citizens navigateTo（不调 LLM）
        double speed = plugin.getConfigManager().getMoveSpeed();
        boolean ok = NpcHelper.navigateTo(v, target, speed);
        if (!ok) {
            plugin.getLogger().fine("空闲漫游 navigateTo 失败: " + aiPlayer.getName());
        }
        // 同时转头朝向行走方向
        NpcHelper.faceLocation(v, target);
    }

    /**
     * 自发转头：玩家靠近 4 格内时自动转头看玩家（不调 LLM，不打断寻路）。
     * 每个 NPC 2 秒最多转一次。
     */
    private void faceNearbyPlayer(AIPlayer aiPlayer) {
        Player v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;
        if (aiPlayer.getBusy().get()) return;
        if (NpcHelper.isNavigating(v)) return;  // 正在走路时不打断转头
        UUID uid = aiPlayer.getEntityId();
        long now = System.currentTimeMillis();
        Long last = lastFacePlayer.get(uid);
        if (last != null && now - last < 2000) return;  // 2 秒最多转一次头
        double radius = 4.0;
        var nearby = v.getNearbyEntities(radius, radius, radius);
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : nearby) {
            if (e instanceof Player p && !p.equals(v)) {
                double d = LocationUtil.safeDistance(p.getLocation(), v.getLocation());
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
        }
        if (nearest == null) return;
        lastFacePlayer.put(uid, now);
        NpcHelper.faceLocation(v, nearest.getLocation());
    }

    private void triggerAutonomousAction(AIPlayer aiPlayer) {
        if (!plugin.getConfigManager().isConfigured()) return;
        Player v = aiPlayer.getEntity();
        if (v == null || !v.isValid()) return;
        if (aiPlayer.getBusy().get()) return;  // 正在处理上一轮 LLM，跳过本轮自主活动

        // 功能 8：检查日程 —— 当前世界时间匹配则执行对应动作
        try {
            long worldTime = v.getWorld().getTime();
            for (Schedule schedule : aiPlayer.getSchedules()) {
                if (schedule.matches(worldTime)) {
                    plugin.getCommandExecutor().execute(aiPlayer, schedule.getAction());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("日程检查失败 [" + aiPlayer.getName() + "]: " + e.getMessage());
        }

        try {
            GameDataCollector collector = plugin.getGameDataCollector();
            String gameData = collector.collect(aiPlayer);
            // P2：目标驱动自主决策 —— 有活跃目标时，把目标摘要前置，让 AI 据此主动出击
            StringBuilder promptBuilder = new StringBuilder();
            String goalSummary = aiPlayer.getGoalManager().getPromptSummary();
            if (!goalSummary.isEmpty()) {
                promptBuilder.append(goalSummary).append("\n");
            }
            promptBuilder.append("（自主思考）当前游戏数据如下：\n").append(gameData)
                    .append("\n基于你的目标、当前进度、最近观察到的事件，决定下一步动作。"
                            + "回复格式：先说一句简短的聊天话（像真人在游戏里打字，不要内心独白），再附带命令执行动作。"
                            + "不要描述自己的计划，不要重复之前说过的话。");
            final String prompt = promptBuilder.toString();
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
        if (last != null && now - last < plugin.getConfigManager().getEnvReactCooldownMs()) return;
        if (aiPlayer.getBusy().get()) return;  // 正在处理上一轮 LLM，跳过本轮环境感知

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
                double d = LocationUtil.safeDistance(m.getLocation(), v.getLocation());
                if (d < nearestMonsterDist) {
                    nearestMonsterDist = d;
                    nearestMonster = m;
                }
            } else if (e instanceof Player p && !p.equals(v)) {
                double d = LocationUtil.safeDistance(p.getLocation(), v.getLocation());
                if (d < nearestPlayerDist) {
                    nearestPlayerDist = d;
                    nearestPlayer = p;
                }
            }
        }

        // 清除已远离的玩家的打招呼记录（让下次再靠近可以再打招呼）
        java.util.Set<String> greeted = greetedPlayers.get(uid);
        if (greeted != null && !greeted.isEmpty()) {
            double farRadius = plugin.getConfigManager().getEntityScanRadius();
            java.util.Set<String> stillNear = ConcurrentHashMap.newKeySet();
            for (org.bukkit.entity.Entity e : nearby) {
                if (e instanceof Player p && !p.equals(v)) {
                    stillNear.add(p.getName());
                }
            }
            // 已打招呼但当前不在 nearby 列表里的玩家，清除标记
            greeted.retainAll(stillNear);
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
                    .append(" 格，威胁很高！请立刻攻击或逃跑。）\n");
        }
        if (nearestPlayer != null && nearestPlayerDist < 4.0) {
            // 去重：只在该玩家首次靠近时打招呼，已在附近的玩家不重复触发
            java.util.Set<String> greetedSet = greetedPlayers.computeIfAbsent(uid, k -> ConcurrentHashMap.newKeySet());
            String playerName = nearestPlayer.getName();
            if (nearestPlayerDist < 2.0) {
                // 距离 < 2 格，标记为"已打招呼"，下次不再触发
                if (!greetedSet.add(playerName)) {
                    // 已打招呼，跳过
                    nearestPlayer = null;
                } else {
                    shouldReact = true;
                    trigger.append("（事件：玩家 ").append(playerName)
                            .append(" 刚走到你身边，距离 ").append(String.format("%.1f", nearestPlayerDist))
                            .append(" 格。你可以说一句话打招呼，比如'嘿'、'你好'、'干嘛呢'。简短口语化，不要描述事件。）\n");
                }
            } else {
                // 距离 2-4 格，玩家可能正在离开，如果之前打过招呼则清除标记（让下次再靠近可以再打招呼）
                if (!greetedSet.contains(playerName)) {
                    shouldReact = true;
                    trigger.append("（事件：玩家 ").append(playerName)
                            .append(" 靠近了，距离 ").append(String.format("%.1f", nearestPlayerDist))
                            .append(" 格。你可以说一句话反应，简短口语化。）\n");
                }
            }
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
                    + "\n请用聊天框说一句简短的话（像真人玩家在游戏里打字），可以附带命令执行动作。"
                    + "禁止描述事件本身（如'玩家走到了我身边'），禁止内心独白（如'我决定...'）。"
                    + "只说真人玩家会说的那种短句，如'嘿'、'我去'、'小心'、'干嘛呢'。";
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

    /**
     * 玩家离开后清除打招呼记录（让下次再靠近可以再打招呼）
     */
    public void clearGreeted(UUID aiUid, String playerName) {
        java.util.Set<String> greeted = greetedPlayers.get(aiUid);
        if (greeted != null) greeted.remove(playerName);
    }

    /**
     * 为 AI 绑定主线任务并启动执行器。
     * 若 main-quest.enabled=false 或 MainQuestFactory 返回 null（无匹配 personality），则跳过。
     */
    private void bindMainQuest(AIPlayer aiPlayer) {
        if (!plugin.getConfigManager().isMainQuestEnabled()) return;
        MainQuest quest = MainQuestFactory.create(aiPlayer.getPersonality(), aiPlayer);
        if (quest == null) return;
        aiPlayer.setMainQuest(quest);
        aiPlayer.setStageStartTime(System.currentTimeMillis());
        // 创建主线任务执行器并存入 aiPlayer（remove/revive 时 cancel）
        MainQuestExecutor executor = new MainQuestExecutor(plugin, aiPlayer);
        aiPlayer.setMainQuestExecutor(executor);
        executor.startFor(aiPlayer);
    }

    /**
     * spawn 后延迟生成开场白。
     * 异步调用 LLM 生成一句话（不超过 30 字，符合个性），过滤 [COMMAND:...] 后回主线程广播。
     */
    private void scheduleIntroLine(AIPlayer aiPlayer) {
        if (!plugin.getConfigManager().isConfigured()) return;
        int delayTicks = plugin.getConfigManager().getIntroDelayTicks();
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                generateIntroLine(aiPlayer);
            } catch (Exception e) {
                plugin.getLogger().warning("生成开场白异常: " + e.getMessage());
            }
        }, delayTicks);
    }

    /**
     * v2.1.4：复活后延迟生成"复仇对话"。
     * <p>
     * 与 {@link #scheduleIntroLine} 的区别：
     *   - 这是"我刚被击杀然后复活"的事件，不是"我刚被生成到这个世界"
     *   - 内部会按当前 StoryPhase 构造不同 prompt（觉醒/轰炸/PVP/制度/独裁/背叛）
     *   - 节流 5 秒：避免连续多次复活刷屏
     *   - 仅在 StoryState != null 且 != COMPLETED 时才生成
     */
    public void scheduleRevengeLine(AIPlayer aiPlayer) {
        if (aiPlayer == null) return;
        if (!plugin.getConfigManager().isConfigured()) return;
        // StoryState 缺失或剧情完结 → 不生成
        com.aip.story.StoryState state = aiPlayer.getStoryState();
        if (state == null) return;
        if (state.getCurrentPhase() == com.aip.story.StoryPhase.COMPLETED) return;
        // 5 秒节流
        String key = aiPlayer.getName().toLowerCase();
        long now = System.currentTimeMillis();
        Long last = lastRevengeTime.get(key);
        if (last != null && now - last < 5_000L) return;
        lastRevengeTime.put(key, now);
        // 延迟 1 秒（让玩家先看到 AI 复活实体），再交给 RevengeLine 处理
        RevengeLine.generateAndSay(plugin, aiPlayer, null, 20L);
    }

    /**
     * 异步生成开场白并广播。
     */
    private void generateIntroLine(AIPlayer aiPlayer) throws java.io.IOException {
        // 构造 LLM 请求
        java.util.List<java.util.Map<String, String>> messages = new java.util.ArrayList<>();
        java.util.Map<String, String> sys = new java.util.HashMap<>();
        sys.put("role", "system");
        sys.put("content", "你是 Minecraft 中的 AI 玩家 " + aiPlayer.getName()
            + "。" + aiPlayer.getPersonality().getPrompt()
            + "你刚刚被生成到这个世界。请说一句开场白，要求：一句话，不超过 30 字，符合你的个性。"
            + "不要输出 [COMMAND:...] 命令。");
        messages.add(sys);
        java.util.Map<String, String> user = new java.util.HashMap<>();
        user.put("role", "user");
        user.put("content", "请说一句开场白。");
        messages.add(user);

        String reply = plugin.getLlmClient().chat(messages);
        if (reply == null || reply.trim().isEmpty()) return;

        // 过滤 [COMMAND:...] 后回主线程广播
        String text = reply.replaceAll("\\[COMMAND:[^\\]]+\\]", "").trim();
        if (text.isEmpty()) return;
        final String finalText = text;
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean broadcast = aiPlayer.sayInChat(finalText);
            if (broadcast) {
                // 存入对话历史作为 assistant 第一条
                aiPlayer.addHistory("assistant", finalText);
            }
        });
    }
}
