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
 * <p>
 * P4 扩展：作战角色（CombatRole）+ 协同目标（coordinationTarget），
 * 并通过 {@link #getTeamPrompt(String)} 生成队友协同信息供 LLM 使用。
 */
public class TeamManager {

    /** 作战角色枚举 */
    public enum CombatRole {
        DECOY,    // 诱饵
        ASSAULT,  // 突击
        SUPPORT,  // 支援
        SCOUT     // 侦察
    }

    private final Map<String, Set<String>> teams = new ConcurrentHashMap<>();
    /** 反向索引：AI 名 -> 队伍名（小写），方便 leaveTeam / sameTeam */
    private final Map<String, String> aiToTeam = new ConcurrentHashMap<>();

    /** P4：AI 作战角色映射（aiName -> role），未设置默认 ASSAULT */
    private final Map<String, CombatRole> aiRoles = new ConcurrentHashMap<>();
    /** P4：队伍共同锁定的玩家名 */
    private volatile String coordinationTarget;

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
        // 离队时清除角色
        aiRoles.remove(aiName);
        return true;
    }

    public void disbandTeam(String name) {
        String key = name.toLowerCase();
        Set<String> members = teams.remove(key);
        if (members != null) {
            for (String ai : members) {
                aiToTeam.remove(ai);
                aiRoles.remove(ai);
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

    // ===== P4：作战角色与协同目标 =====

    public void setRole(String aiName, CombatRole role) {
        aiRoles.put(aiName, role);
    }

    public CombatRole getRole(String aiName) {
        return aiRoles.getOrDefault(aiName, CombatRole.ASSAULT);
    }

    public void setCoordinationTarget(String target) {
        this.coordinationTarget = target;
    }

    public String getCoordinationTarget() {
        return coordinationTarget;
    }

    /** 角色枚举转中文显示 */
    private String roleLabel(CombatRole role) {
        return switch (role) {
            case DECOY -> "诱饵";
            case ASSAULT -> "突击";
            case SUPPORT -> "支援";
            case SCOUT -> "侦察";
        };
    }

    /**
     * 生成队友协同信息供 LLM 使用
     * <p>
     * 返回格式示例："队友：Bob（诱饵），Alice（支援）。协同目标：Steve"
     * 若 AI 不在队伍中或无队友，返回空字符串。
     */
    public String getTeamPrompt(String aiName) {
        String team = aiToTeam.get(aiName);
        if (team == null) return "";
        Set<String> members = teams.get(team);
        if (members == null || members.size() <= 1) return "";

        // 收集队友（除自己外）及其角色
        StringBuilder teammates = new StringBuilder();
        for (String m : members) {
            if (m.equals(aiName)) continue;
            if (teammates.length() > 0) teammates.append("，");
            teammates.append(m).append("（").append(roleLabel(getRole(m))).append("）");
        }
        if (teammates.length() == 0) return "";

        StringBuilder sb = new StringBuilder("队友：").append(teammates).append("。");
        if (coordinationTarget != null && !coordinationTarget.isEmpty()) {
            sb.append("协同目标：").append(coordinationTarget);
        }
        return sb.toString();
    }
}
