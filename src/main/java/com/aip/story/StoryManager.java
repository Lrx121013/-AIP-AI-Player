package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v2.2.7：火柴盒故事管理器（章节调度）
 * <p>
 * 阶段切换由 tickChapter 周期推进（10 秒一次扫描）。
 * 不再使用 LLM hook 或 notifyLlm；所有剧情由硬编码模板 + LLM NPC 对话驱动。
 */
public class StoryManager {
    private final AIPlayerPlugin plugin;
    private final Map<UUID, StoryState> states = new HashMap<>();
    private org.bukkit.scheduler.BukkitTask tickTask;

    public StoryManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // 章节调度器：每 10 秒扫描
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickChapter, 200L, 200L);
    }

    public void cancel() {
        if (tickTask != null) tickTask.cancel();
    }

    /** v2.2.7：周期扫描，章节超时则推进 */
    private void tickChapter() {
        try {
            for (StoryState s : states.values()) {
                if (s == null || !s.isStoryStarted() || s.isStoryCompleted()) continue;
                // 章节调度由 sub-agent 后续实施
                if (s.isChapterTimeout()) {
                    advanceChapter(s);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("tickChapter 失败: " + e.getMessage());
        }
    }

    /** v2.2.7：推进到下一章（stub，由 sub-agent 后续实施具体逻辑） */
    private void advanceChapter(StoryState s) {
        // stub
    }

    public StoryState getState(UUID playerId) {
        return states.get(playerId);
    }

    public StoryState getOrCreateState(UUID playerId) {
        return states.computeIfAbsent(playerId, StoryState::new);
    }

    public Collection<StoryState> getAllStates() {
        return states.values();
    }
}
