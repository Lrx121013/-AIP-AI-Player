package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * v2.2.0：盟军管理器
 * <p>
 * AIP 决战时可以召唤盟军（PVP_DUEL / BETRAYAL 阶段）。盟军自动跟随主 AIP 攻击玩家。
 * 盟军名格式：&lt;mainName&gt;_ally_&lt;N&gt;（N 自增）。
 * 盟军有自己的 StoryState（永远 DORMANT，不参与主线阶段）。
 */
public class AllyManager {

    private final AIPlayerPlugin plugin;
    /** 主 AIP UUID → 盟军 UUID 列表 */
    private final Map<UUID, List<UUID>> mainToAllies = new ConcurrentHashMap<>();
    /** 盟军 UUID → 主 AIP UUID */
    private final Map<UUID, UUID> allyToMain = new ConcurrentHashMap<>();
    /** 盟军名 → 计数器（每主 AIP 独立） */
    private final Map<String, AtomicInteger> nameCounters = new ConcurrentHashMap<>();
    /** 召唤节流：主 AIP UUID → 上次召唤时间戳 */
    private final Map<UUID, Long> lastSummonTime = new ConcurrentHashMap<>();

    public AllyManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** 是否可以召唤：节流 + 上限检查 */
    public boolean canSummon(AIPlayer mainAi) {
        if (mainAi == null) return false;
        UUID uid = mainAi.getEntityId();
        if (uid == null) return false;
        long now = System.currentTimeMillis();
        long cd = plugin.getConfigManager().getAllySummonCooldownSeconds() * 1000L;
        Long last = lastSummonTime.get(uid);
        if (last != null && now - last < cd) return false;
        int max = plugin.getConfigManager().getMaxAlliesPerAi();
        List<UUID> allies = mainToAllies.get(uid);
        if (allies != null && allies.size() >= max) return false;
        return true;
    }

    /** 召唤一个盟军（仅 PVP_DUEL / BETRAYAL 阶段可调） */
    public AIPlayer summon(AIPlayer mainAi) {
        if (mainAi == null) return null;
        String baseName = mainAi.getName();
        AtomicInteger counter = nameCounters.computeIfAbsent(baseName, k -> new AtomicInteger(0));
        int n = counter.getAndIncrement();
        String allyName = baseName + "_ally_" + n;
        // 找主 AIP 位置
        Player entity = mainAi.getEntity();
        if (entity == null || !entity.isValid()) return null;
        // 调用 AIPlayerManager.spawn 生成新 AIP
        AIPlayer ally = plugin.getAiPlayerManager().spawn(allyName, entity);
        if (ally == null) {
            // spawn 已存在，尝试 get
            ally = plugin.getAiPlayerManager().get(allyName);
            if (ally == null) return null;
        }
        // 强制 VILLAIN 人格
        ally.setOriginalPersonality(ally.getPersonality());
        ally.setPersonality(Personality.VILLAIN);
        // 注册盟军关系
        mainToAllies.computeIfAbsent(mainAi.getEntityId(), k -> new java.util.ArrayList<>()).add(ally.getEntityId());
        allyToMain.put(ally.getEntityId(), mainAi.getEntityId());
        // 记录召唤时间
        lastSummonTime.put(mainAi.getEntityId(), System.currentTimeMillis());
        plugin.getLogger().info("[Ally] " + baseName + " 召唤盟军 " + allyName);
        return ally;
    }

    public List<AIPlayer> getAllies(AIPlayer mainAi) {
        if (mainAi == null) return Collections.emptyList();
        List<UUID> ids = mainToAllies.get(mainAi.getEntityId());
        if (ids == null) return Collections.emptyList();
        List<AIPlayer> result = new java.util.ArrayList<>();
        for (UUID id : ids) {
            AIPlayer ally = plugin.getAiPlayerManager().getByEntity(id);
            if (ally != null) result.add(ally);
        }
        return result;
    }

    public boolean isAlly(UUID uuid) {
        return uuid != null && allyToMain.containsKey(uuid);
    }

    public UUID getMainAi(AIPlayer ally) {
        if (ally == null) return null;
        return allyToMain.get(ally.getEntityId());
    }

    /** 移除指定盟军 */
    public boolean remove(String allyName) {
        AIPlayer ally = plugin.getAiPlayerManager().get(allyName);
        if (ally == null) return false;
        return remove(ally);
    }

    public boolean remove(AIPlayer ally) {
        if (ally == null) return false;
        UUID allyId = ally.getEntityId();
        UUID mainId = allyToMain.remove(allyId);
        if (mainId != null) {
            List<UUID> list = mainToAllies.get(mainId);
            if (list != null) list.remove(allyId);
        }
        plugin.getAiPlayerManager().remove(ally.getName());
        return true;
    }

    /** 移除主 AIP 的所有盟军 */
    public void removeAll(UUID mainAiUuid) {
        if (mainAiUuid == null) return;
        List<UUID> list = mainToAllies.remove(mainAiUuid);
        if (list == null) return;
        for (UUID allyId : list) {
            allyToMain.remove(allyId);
            AIPlayer ally = plugin.getAiPlayerManager().getByEntity(allyId);
            if (ally != null) {
                plugin.getAiPlayerManager().remove(ally.getName());
            }
        }
    }

    public void removeAll() {
        for (UUID mainId : new java.util.ArrayList<>(mainToAllies.keySet())) {
            removeAll(mainId);
        }
    }

    /** 列出所有主 AIP → 盟军映射 */
    public Map<UUID, List<UUID>> getAllMappings() {
        return Collections.unmodifiableMap(mainToAllies);
    }
}
