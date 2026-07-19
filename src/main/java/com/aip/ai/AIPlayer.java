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

/**
 * AI 玩家实例：用真正的玩家实体（NPC）作为物理表现，并维护对话历史与 AI 状态
 */
public class AIPlayer {

    private final AIPlayerPlugin plugin;
    private final String name;
    private final UUID entityId;
    private final List<Map<String, String>> conversationHistory;
    private final List<String> pendingActions;
    private boolean activated;
    private Location lastLocation;
    private double health;
    private int foodLevel;
    private String following; // 正在跟随的玩家名

    public AIPlayer(AIPlayerPlugin plugin, String name, UUID entityId) {
        this.plugin = plugin;
        this.name = name;
        this.entityId = entityId;
        this.conversationHistory = new CopyOnWriteArrayList<>();
        this.pendingActions = new ArrayList<>();
        this.activated = false;
        this.health = 20.0;
        this.foodLevel = 20;
    }

    public String getName() { return name; }
    public UUID getEntityId() { return entityId; }
    public List<Map<String, String>> getConversationHistory() { return conversationHistory; }

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
                if (myLoc.distance(targetLoc) > 3) {
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
