package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 玩家实例：用真正的玩家实体（NPC）作为物理表现，并维护对话历史与 AI 状态
 */
public class AIPlayer {

    private final AIPlayerPlugin plugin;
    private final String name;
    private UUID entityId;
    private final List<Map<String, String>> conversationHistory;
    private final List<String> pendingActions;
    private boolean activated;
    private Location lastLocation;
    private double health;
    private int foodLevel;
    private String following; // 正在跟随的玩家名

    // ===== 新增字段（功能 1/3/7/8/9/10） =====
    /** 统计数据（功能 1） */
    private final AIStats stats = new AIStats();
    /** 个性（功能 3） */
    private Personality personality = Personality.GENTLE;
    /** 反派模式开启前的原人格，关闭后恢复（P2） */
    private Personality originalPersonality = null;
    /** 情绪值 0-100，默认 50（功能 9） */
    private int mood = 50;
    /** 死亡位置，用于复活（功能 7） */
    private Location deathLocation;
    /** 日程列表（功能 8） */
    private final List<Schedule> schedules = new ArrayList<>();
    /** 死亡日志（功能 10） */
    private final List<DeathRecord> deathLog = new ArrayList<>();
    /** 上一轮命令执行结果，用于回流给 LLM（P1：执行结果回流） */
    private ExecutionResult lastCommandResult;
    /** P3：上次 query 命令的查询结果，下一轮 prompt 注入后清除 */
    private String lastQueryResult;
    /** P2：长期目标管理器 */
    private GoalManager goalManager;
    /** P3：长期记忆系统 */
    private final LongTermMemory memory = new LongTermMemory();
    /** P1：AI 正在思考的忙标记（防重入），原子操作 */
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public AIPlayer(AIPlayerPlugin plugin, String name, UUID entityId) {
        this.plugin = plugin;
        this.name = name;
        this.entityId = entityId;
        this.conversationHistory = new CopyOnWriteArrayList<>();
        this.pendingActions = new ArrayList<>();
        this.activated = false;
        this.health = 20.0;
        this.foodLevel = 20;
        this.goalManager = new GoalManager(plugin, this);
    }

    public String getName() { return name; }
    public UUID getEntityId() { return entityId; }
    /** 复活时更新实体 UUID（功能 7） */
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public List<Map<String, String>> getConversationHistory() { return conversationHistory; }

    // ===== 新增 getter / setter =====

    /** 统计数据（功能 1） */
    public AIStats getStats() { return stats; }

    /** 个性（功能 3） */
    public Personality getPersonality() { return personality; }
    public void setPersonality(Personality personality) {
        if (personality != null) this.personality = personality;
    }

    /** 反派模式开启前的原人格（P2），可能为 null */
    public Personality getOriginalPersonality() { return originalPersonality; }
    public void setOriginalPersonality(Personality originalPersonality) {
        this.originalPersonality = originalPersonality;
    }

    /** 情绪（功能 9）：0-100，自动 clamp */
    public int getMood() { return mood; }
    public void setMood(int mood) {
        this.mood = Math.max(0, Math.min(100, mood));
    }
    public void adjustMood(int delta) {
        setMood(this.mood + delta);
    }

    /** 死亡位置 / 复活用（功能 7） */
    public Location getDeathLocation() { return deathLocation; }
    public void setDeathLocation(Location deathLocation) { this.deathLocation = deathLocation; }

    /** 日程列表（功能 8） */
    public List<Schedule> getSchedules() { return schedules; }

    /** 死亡日志（功能 10） */
    public List<DeathRecord> getDeathLog() { return deathLog; }

    /** 上一轮命令执行结果（P1：执行结果回流，可能为 null） */
    public ExecutionResult getLastCommandResult() { return lastCommandResult; }
    public void setLastCommandResult(ExecutionResult lastCommandResult) {
        this.lastCommandResult = lastCommandResult;
    }

    /** P3：上次 query 命令的查询结果（可能为 null） */
    public String getLastQueryResult() { return lastQueryResult; }
    public void setLastQueryResult(String lastQueryResult) {
        this.lastQueryResult = lastQueryResult;
    }

    /** P2：长期目标管理器 */
    public GoalManager getGoalManager() { return goalManager; }

    /** P3：长期记忆系统 */
    public LongTermMemory getMemory() { return memory; }

    /** P1：忙标记（用于 chat 防重入） */
    public AtomicBoolean getBusy() { return busy; }

    public Player getEntity() {
        org.bukkit.entity.Entity ent = Bukkit.getEntity(entityId);
        return ent instanceof Player ? (Player) ent : null;
    }

    public Location getLocation() {
        Player p = getEntity();
        if (p != null) {
            lastLocation = p.getLocation();
            return lastLocation;
        }
        return lastLocation;
    }

    public World getWorld() {
        Location loc = getLocation();
        return loc != null ? loc.getWorld() : null;
    }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }

    public double getHealth() {
        Player p = getEntity();
        if (p != null) return p.getHealth();
        return health;
    }

    public int getFoodLevel() { return foodLevel; }

    public void setFollowing(String playerName) { this.following = playerName; }
    public String getFollowing() { return following; }

    /**
     * 添加一条对话历史（保留最大数量限制）
     */
    public void addHistory(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        conversationHistory.add(msg);
        ConfigManager cfg = plugin.getConfigManager();
        int max = cfg.getMaxHistory();
        while (conversationHistory.size() > max) {
            conversationHistory.remove(0);
        }
    }

    /**
     * 重置对话历史
     */
    public void resetHistory() {
        conversationHistory.clear();
    }

    /**
     * 把消息以聊天框形式广播（只显示文字，不含命令）
     */
    public void sayInChat(String message) {
        if (message == null || message.trim().isEmpty()) return;
        String formatted = "<" + name + "> " + message.trim();
        Bukkit.broadcastMessage(formatted);
    }

    /**
     * 跟随任务
     */
    public void startFollowTask() {
        if (following == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (following == null) {
                    cancel();
                    return;
                }
                Player v = getEntity();
                if (v == null || !v.isValid()) {
                    cancel();
                    return;
                }
                org.bukkit.entity.Player target = Bukkit.getPlayerExact(following);
                if (target == null || !target.isOnline()) {
                    following = null;
                    cancel();
                    return;
                }
                Location targetLoc = target.getLocation();
                Location myLoc = v.getLocation();
                double dist;
                try {
                    dist = myLoc.distance(targetLoc);
                } catch (Exception e) {
                    return;
                }
                // 距离 > 3 才需要追
                if (dist <= 3) {
                    // 已靠近，取消寻路
                    NpcHelper.cancelNavigation(v);
                    return;
                }
                // 优先用后端寻路
                boolean navigated = NpcHelper.navigateTo(v, targetLoc,
                        plugin.getConfigManager().getMoveSpeed());
                if (!navigated) {
                    // 回退：分帧 teleport
                    Location dir = targetLoc.clone().subtract(myLoc).toVector().normalize()
                            .multiply(plugin.getConfigManager().getMoveSpeed())
                            .toLocation(myLoc.getWorld());
                    Location next = myLoc.clone().add(dir);
                    next.setY(targetLoc.getY());
                    v.teleport(next);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}
