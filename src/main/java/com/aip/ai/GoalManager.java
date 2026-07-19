package com.aip.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AI 目标管理器
 * <p>
 * 维护单个 AI 玩家的所有长期目标，支持添加、完成、查询和生成 prompt 摘要。
 */
public class GoalManager {
    private final List<Goal> goals = new ArrayList<>();

    public Goal addGoal(String description, int priority) {
        Goal g = new Goal(description, priority);
        goals.add(g);
        return g;
    }

    public boolean completeGoal(String id) {
        Optional<Goal> g = goals.stream().filter(x -> x.getId().equals(id)).findFirst();
        if (g.isPresent()) {
            g.get().complete();
            return true;
        }
        return false;
    }

    public List<Goal> getActiveGoals() {
        return goals.stream().filter(g -> !g.isCompleted())
                .sorted((a, b) -> b.getPriority() - a.getPriority()).toList();
    }

    public List<Goal> getAllGoals() {
        return goals;
    }

    /** 生成 prompt 摘要，供 system prompt 和自主决策使用 */
    public String getPromptSummary() {
        List<Goal> active = getActiveGoals();
        if (active.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("当前活跃目标：\n");
        for (Goal g : active) {
            sb.append("- ").append(g.getDescription())
                    .append("（优先级").append(g.getPriority())
                    .append("，进度").append(g.getProgress()).append("%）\n");
        }
        return sb.toString();
    }
}
