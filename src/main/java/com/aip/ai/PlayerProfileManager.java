package com.aip.ai;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家档案管理器
 */
public class PlayerProfileManager {
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> new PlayerProfile(uuid));
    }

    public PlayerProfile getOrCreate(Player player) {
        PlayerProfile p = getProfile(player.getUniqueId());
        p.setName(player.getName());
        p.setLastSeen(System.currentTimeMillis());
        return p;
    }

    public void recordAttack(Player attacker, AIPlayer victim) {
        PlayerProfile p = getOrCreate(attacker);
        p.recordAttack();
    }

    /** 生成附近玩家档案摘要 */
    public String getNearbySummary(Location center, double radius, List<Player> nearbyPlayers) {
        if (nearbyPlayers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("附近玩家档案：\n");
        for (Player p : nearbyPlayers) {
            PlayerProfile profile = getProfile(p.getUniqueId());
            sb.append("- ").append(p.getName())
              .append("：威胁等级 ").append(profile.getThreatLevel())
              .append("，攻击过 AI ").append(profile.getAttackCount()).append(" 次")
              .append("\n");
        }
        return sb.toString();
    }
}
