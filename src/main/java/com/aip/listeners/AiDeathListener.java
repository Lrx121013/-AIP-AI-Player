package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * 监听 AI NPC 死亡事件
 * <p>
 * v2.2.7：火柴盒版移除了"觉醒 / 空中轰炸 / PVP / 独裁"等阶段。
 * NPC 死亡不再推进任何故事阶段，只做"非故事模式"的死亡记录与广播占位。
 * 故事模式阶段推进由 {@link com.aip.story.StoryManager#tickChapter()} 周期任务独立驱动。
 * <p>
 * 实际 NPC 实体的销毁由 NpcDeathListener 处理。
 */
public class AiDeathListener implements Listener {

    private final AIPlayerPlugin plugin;

    public AiDeathListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAiDeath(PlayerDeathEvent event) {
        // 仅占位：v2.2.7 火柴盒版不再因 AI 死亡推进故事阶段
        // 故事推进完全由 StoryManager.tickChapter 周期任务驱动（基于时间超时）
        // 死亡广播统一由 NpcDeathListener 处理
        if (plugin.getStoryManager() == null) return;
        org.bukkit.entity.Player dead = event.getEntity();
        AIPlayer ai = plugin.getAiPlayerManager().getByEntity(dead.getUniqueId());
        if (ai == null) return;  // 不是我们的 AI NPC
        // v2.2.7 stub：原 StoryManager.onAiDeath / state.getAiDeathCount 已废弃
    }
}
