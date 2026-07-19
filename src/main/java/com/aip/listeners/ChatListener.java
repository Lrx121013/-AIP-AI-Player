package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.ConversationManager;
import com.aip.ai.GameDataCollector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天事件监听器：检测 @AI名字 并触发 AI 对话
 */
public class ChatListener implements Listener {

    // 匹配 @<名字> 开头（中文/英文/数字/下划线）
    private static final Pattern MENTION_PATTERN = Pattern.compile("^@([\\w\\u4e00-\\u9fa5]+)\\s+(.+)$");

    private final AIPlayerPlugin plugin;

    public ChatListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        Player sender = event.getPlayer();

        Matcher m = MENTION_PATTERN.matcher(msg);
        if (!m.find()) return;

        String aiName = m.group(1);
        String content = m.group(2);

        AIPlayer aiPlayer = plugin.getAiPlayerManager().get(aiName);
        if (aiPlayer == null) return;

        // 首次激活
        if (!aiPlayer.isActivated()) {
            aiPlayer.setActivated(true);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    GameDataCollector collector = plugin.getGameDataCollector();
                    String gameData = collector.collect(aiPlayer);
                    String prompt = "（系统：你刚刚被召唤到这个世界。请简短自我介绍并说明你想做什么。"
                            + "下面是当前游戏数据：）\n" + gameData;
                    ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                    String reply = cm.chat(prompt, null);
                    final String finalReply = reply;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("激活 AI 失败: " + e.getMessage());
                }
            });
        }

        // 异步处理：收集游戏数据 + 调用 LLM
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(aiPlayer);
                String prompt = content + "\n\n（附当前游戏数据：）\n" + gameData;
                ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                String reply = cm.chat(prompt, sender);
                // 命令必须在主线程执行
                final String finalReply = reply;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                });
            } catch (Exception e) {
                sender.sendMessage("§c[AIP] AI 处理失败: " + e.getMessage());
            }
        });
    }
}
