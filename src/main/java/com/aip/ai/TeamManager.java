package com.aip.ai;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 队伍管理器
 * <p>
 * 维护队伍名 -> 成员集合的映射，并提供加入/退出/解散/查询等操作。
 * 同时维护反向索引（AI 名 -> 队伍名）以便快速 leaveTeam / sameTeam。
 */
public class TeamManager {

    private final Map<String, Set<String>> teams = new ConcurrentHashMap<>();
    /** 反向索引：AI 名 -> 队伍名（小写），方便 leaveTeam / sameTeam */
    private final Map<String, String> aiToTeam = new ConcurrentHashMap<>();

    public void createTeam(String name) {
        teams.put(name.toLowerCase(), ConcurrentHashMap.newKeySet());
    }

    public boolean joinTeam(String team, String aiName) {
        String key = team.toLowerCase();
        Set<String> members = teams.get(key);
        if (members == null) return false;
        // 先退出原队伍
        leaveTeam(aiName);
        members.add(aiName);
        aiToTeam.put(aiName, key);
        return true;
    }

    public boolean leaveTeam(String aiName) {
        String team = aiToTeam.remove(aiName);
        if (team == null) return false;
        Set<String> members = teams.get(team);
        if (members != null) members.remove(aiName);
        return true;
    }

    public void disbandTeam(String name) {
        String key = name.toLowerCase();
        Set<String> members = teams.remove(key);
        if (members != null) {
            for (String ai : members) {
                aiToTeam.remove(ai);
            }
        }
    }

    public Set<String> getTeams() {
        return teams.keySet();
    }

    public Set<String> getMembers(String team) {
        return teams.getOrDefault(team.toLowerCase(), new HashSet<>());
    }

    /**
     * 判断两个 AI 是否在同一队伍
     */
    public boolean sameTeam(String ai1, String ai2) {
        String t1 = aiToTeam.get(ai1);
        String t2 = aiToTeam.get(ai2);
        return t1 != null && t1.equals(t2);
    }
}
