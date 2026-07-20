package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.ConversationManager;
import com.aip.ai.GameDataCollector;
import com.aip.story.StoryPhase;
import com.aip.story.StoryState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天事件监听器
 * <p>
 * 功能：
 *   1. 检测 @AI名字 并触发 AI 对话
 *   2. v2.2.9：检测 [回家] / [继续跑]（Chapter 9 Eve 的聊天框选择）
 *      - 玩家直接输入这两个关键词，调用 StoryManager.chooseEnding
 *      - 取消事件（避免污染公开聊天）
 *      - 仅在玩家有未完成的故事且当前在 Chapter 9 时响应
 */
public class ChatListener implements Listener {

    // 匹配 @<名字> 开头（中文/英文/数字/下划线）
    private static final Pattern MENTION_PATTERN = Pattern.compile("^@([\\w\\u4e00-\\u9fa5]+)\\s+(.+)$");
    // v2.2.9：Chapter 9 选择 —— 回家 / 继续跑
    private static final Pattern HOME_PATTERN = Pattern.compile("^\\s*\\[回家\\]\\s*$");
    private static final Pattern RUN_PATTERN = Pattern.compile("^\\s*\\[继续跑\\]\\s*$");

    private final AIPlayerPlugin plugin;

    public ChatListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        Player sender = event.getPlayer();

        // ===== v2.2.9: Chapter 9 聊天框选择 ([回家] / [继续跑]) =====
        if (handleChapter9Choice(sender, msg, event)) {
            return;
        }

        Matcher m = MENTION_PATTERN.matcher(msg);
        if (!m.find()) return;

        String aiName = m.group(1);
        String content = m.group(2);

        AIPlayer aiPlayer = plugin.getAiPlayerManager().get(aiName);
        if (aiPlayer == null) return;

        // 首次激活
        if (!aiPlayer.isActivated()) {
            aiPlayer.setActivated(true);
            // 先在主线程采集游戏数据，再异步调用 LLM（Bukkit.getEntity 必须在主线程）
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    GameDataCollector collector = plugin.getGameDataCollector();
                    String gameData = collector.collect(aiPlayer);
                    final String prompt = "（系统：你刚刚被召唤到这个世界。请简短自我介绍并说明你想做什么。"
                            + "下面是当前游戏数据：）\n" + gameData;
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                            String reply = cm.chat(prompt, null);
                            if (reply == null) {
                                // busy：本轮已被跳过（自主/激活场景无玩家提示）
                                return;
                            }
                            final String finalReply = reply;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                            });
                        } catch (Exception e) {
                            plugin.getLogger().warning("激活 AI 失败: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("激活 AI 采集数据失败: " + e.getMessage());
                }
            });
        }

        // 先在主线程采集游戏数据，再异步调用 LLM（避免异步访问实体）
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(aiPlayer);
                final String prompt = content + "\n\n（附当前游戏数据：）\n" + gameData;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                        String reply = cm.chat(prompt, sender);
                        if (reply == null) {
                            // busy：chat 内部已发送"正在思考…"提示，直接返回
                            return;
                        }
                        // 命令必须在主线程执行
                        final String finalReply = reply;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                        });
                    } catch (Exception e) {
                        sender.sendMessage("§c[AIP] AI 处理失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                sender.sendMessage("§c[AIP] 采集游戏数据失败: " + e.getMessage());
            }
        });
    }

    /**
     * v2.2.9：检测 Chapter 9 Eve 的 [回家]/[继续跑] 聊天输入。
     *
     * @return true 表示该消息已处理（无论是否成功触发选择）
     */
    private boolean handleChapter9Choice(Player player, String msg, AsyncPlayerChatEvent event) {
        if (player == null || msg == null) return false;
        boolean home = HOME_PATTERN.matcher(msg).matches();
        boolean run = RUN_PATTERN.matcher(msg).matches();
        if (!home && !run) return false;

        // 取消聊天事件，避免在公共频道刷屏
        event.setCancelled(true);

        // 校验：玩家必须已开启故事，且当前在 Chapter 9
        StoryState state = plugin.getStoryManager().getState(player.getUniqueId());
        if (state == null || !state.isStoryStarted() || state.isStoryCompleted()) {
            player.sendMessage("§c[AI 故事] 你还没有进入 Chapter 9（先用 /aistory 开启故事）。");
            return true;
        }
        if (state.getCurrentPhase() != StoryPhase.CHAPTER_9_NEGOTIATION) {
            player.sendMessage("§c[AI 故事] 当前阶段不是 Chapter 9（谈判），无法做此选择。"
                    + " 当前：§7" + state.getCurrentPhase().getDisplayName());
            return true;
        }

        // 派发结局选择
        String ending = home ? "10A" : "10B";
        boolean ok = plugin.getStoryManager().chooseEnding(player, ending);
        if (ok) {
            player.sendMessage("§6[AI 故事] §e你选择了 §f[" + (home ? "回家" : "继续跑") + "]§e。"
                    + " 派发到结局 §f" + ending + "§e，详见后续剧情。");
        } else {
            player.sendMessage("§c[AI 故事] 派发失败，请重试。");
        }
        return true;
    }
}
