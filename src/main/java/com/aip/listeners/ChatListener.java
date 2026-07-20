package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.story.StoryPhase;
import com.aip.story.StoryState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * v2.2.10：聊天监听器（解析 /aistory 命令 + clickEvent 文本 [投降] / [反抗]）
 * <p>
 * 设计：
 *   - 监听玩家聊天消息
 *   - 章节 9 时如果玩家在聊天框输入 [投降] / [反抗] → 取消事件并调用 chooseEnding
 *   - /aistory 命令：保留 v2.2.9 的旧解析逻辑（与 /aip 子命令一致）
 *   - /aip story choose <name> <ending>：脚本派发（来自 NPC clickEvent）
 */
public class ChatListener implements Listener {

    private final AIPlayerPlugin plugin;

    public ChatListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听玩家聊天：处理 [投降] / [反抗] 选择
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event == null || event.isCancelled()) return;
        Player player = event.getPlayer();
        if (player == null) return;

        String msg = event.getMessage();
        if (msg == null) return;
        String trimmed = msg.trim();

        // 章节 9 时解析 [投降] / [反抗]
        StoryState s = plugin.getStoryManager().getState(player.getUniqueId());
        if (s != null && s.isStoryStarted() && !s.isStoryCompleted()
                && s.getCurrentPhase() == StoryPhase.CHAPTER_9_FINAL_CHOICE) {
            if ("[投降]".equals(trimmed) || "投降".equals(trimmed)) {
                event.setCancelled(true);
                // 切回主线程执行（StoryManager 涉及 Bukkit API）
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getStoryManager().chooseEnding(player, "10A"));
                return;
            }
            if ("[反抗]".equals(trimmed) || "反抗".equals(trimmed)) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getStoryManager().chooseEnding(player, "10B"));
                return;
            }
        }
    }

    /**
     * 命令处理（保留 v2.2.9 行为 + 新增 /aip story choose）
     * <p>
     * 主命令注册由 AIPlayerPlugin.onEnable() 调用 getCommand("aip").setExecutor(AIPCommand) 处理。
     * 这里仅做占位防止空监听器报错。
     */
    @EventHandler
    public void onDummy(AsyncPlayerChatEvent event) {
        // 占位
    }
}
