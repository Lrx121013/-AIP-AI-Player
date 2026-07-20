package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话管理器：负责拼装对话历史、调用 LLM、返回 AI 回复
 */
public class ConversationManager {

    private final AIPlayerPlugin plugin;
    private final AIPlayer aiPlayer;

    public ConversationManager(AIPlayerPlugin plugin, AIPlayer aiPlayer) {
        this.plugin = plugin;
        this.aiPlayer = aiPlayer;
    }

    /**
     * 与 AI 进行一轮对话
     *
     * @param userMessage 玩家发送给 AI 的消息（已包含游戏数据上下文）
     * @param speaker     发送消息的玩家（用于显示），可为 null（表示系统触发）
     * @return AI 回复的原始文本（含 [COMMAND:...] 命令）
     */
    public String chat(String userMessage, Player speaker) throws IOException {
        // P1：忙标记防重入，已被占用则直接返回 null（caller 自行处理）
        if (!aiPlayer.getBusy().compareAndSet(false, true)) {
            if (speaker != null) {
                speaker.sendMessage("§7" + aiPlayer.getName() + " 正在思考…");
            }
            return null;
        }
        try {
        ConfigManager cfg = plugin.getConfigManager();

        // 1. 准备消息列表
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示词（首次激活时包含游戏数据/初始状态）
        // 追加个性（功能 3）和情绪（功能 9）
        StringBuilder systemPromptBuilder = new StringBuilder(cfg.renderSystemPrompt(aiPlayer.getName()));
        systemPromptBuilder.append("\n").append(aiPlayer.getPersonality().getPrompt());
        int mood = aiPlayer.getMood();
        if (mood < 30) {
            systemPromptBuilder.append("\n你现在心情沮丧");
        } else if (mood > 70) {
            systemPromptBuilder.append("\n你现在心情愉悦");
        }
        // P1：自动注入命令文档（替代 config.yml 中删除的硬编码命令清单）
        String commandDocs = plugin.getCommandExecutor().getCachedDocs();
        if (commandDocs != null && !commandDocs.isEmpty()) {
            systemPromptBuilder.append("\n\n").append(commandDocs);
        }
        // P2：注入长期目标摘要，让 AI 时时记得自己的战略目标
        String goalSummary = aiPlayer.getGoalManager().getPromptSummary();
        if (!goalSummary.isEmpty()) {
            systemPromptBuilder.append("\n").append(goalSummary);
        }
        // 反射规则摘要：让 AI 知道当前已定义的自动反射规则
        String reflexSummary = aiPlayer.getReflexManager().getPromptSummary();
        if (reflexSummary != null && !reflexSummary.isEmpty()) {
            systemPromptBuilder.append("\n\n").append(reflexSummary);
        }
        // P3：注入长期记忆摘要
        String memorySummary = aiPlayer.getMemory().getPromptSummary();
        if (!memorySummary.isEmpty()) {
            systemPromptBuilder.append("\n").append(memorySummary);
        }
        // P4：注入队友协同信息，让 AI 知道队友角色与协同目标
        String teamInfo = plugin.getTeamManager().getTeamPrompt(aiPlayer.getName());
        if (!teamInfo.isEmpty()) {
            systemPromptBuilder.append("\n").append(teamInfo);
        }
        // P5：注入主线任务摘要，让 AI 知道当前阶段和进度
        String questSummary = aiPlayer.getMainQuest() != null
                ? aiPlayer.getMainQuest().getPromptSummary()
                : "";
        if (!questSummary.isEmpty()) {
            systemPromptBuilder.append("\n").append(questSummary);
        }
        messages.add(makeMessage("system", systemPromptBuilder.toString()));

        // 历史对话
        for (Map<String, String> h : aiPlayer.getConversationHistory()) {
            messages.add(h);
        }

        // 当前用户消息
        // P1：若上一轮有命令执行结果，前置反馈让 AI 从失败中学习
        StringBuilder userContent = new StringBuilder();
        ExecutionResult lastResult = aiPlayer.getLastCommandResult();
        if (lastResult != null) {
            userContent.append("你上一轮 ").append(lastResult.toString())
                    .append("。请据此调整。\n");
            // 回流消费后清空，避免重复提示
            aiPlayer.setLastCommandResult(null);
        }
        // P3：注入上次 query 命令的查询结果
        String lastQuery = aiPlayer.getLastQueryResult();
        if (lastQuery != null && !lastQuery.isEmpty()) {
            userContent.append("上次查询结果：\n").append(lastQuery).append("\n\n");
            aiPlayer.setLastQueryResult(null);  // 注入后清除避免重复
        }
        String prefix = speaker != null ? "玩家 " + speaker.getName() + " @你 说：" : "（系统提示）";
        userContent.append(prefix).append(userMessage);
        messages.add(makeMessage("user", userContent.toString()));

        if (cfg.isDebug()) {
            plugin.getLogger().info("=== LLM 请求 ===");
            for (Map<String, String> m : messages) {
                plugin.getLogger().info("[" + m.get("role") + "] " + m.get("content"));
            }
        }

        // 2. 调用 LLM（P1：流式，首 token 到达时给玩家"正在打字"提示）
        String reply;
        try {
            reply = plugin.getLlmClient().chatStream(messages, (token, isFirst) -> {
                if (isFirst && speaker != null) {
                    speaker.sendMessage("§7" + aiPlayer.getName() + " 正在打字…");
                }
            });
        } catch (IOException e) {
            String errMsg = "（AI 接口出错：" + e.getMessage() + "）";
            plugin.getLogger().warning("LLM 调用失败: " + e.getMessage());
            if (speaker != null) {
                speaker.sendMessage("§c[AIP] AI 接口调用失败: " + e.getMessage());
            }
            throw e;
        }

        if (cfg.isDebug()) {
            plugin.getLogger().info("=== LLM 回复 ===\n" + reply);
        }

        // 3. 记录历史
        String userHistContent = (speaker != null ? "玩家 " + speaker.getName() + "：" : "")
                + userMessage;
        aiPlayer.addHistory("user", userHistContent);
        aiPlayer.addHistory("assistant", reply);

        return reply;
        } finally {
            // P1：释放忙标记，允许下一轮对话进入
            aiPlayer.getBusy().set(false);
        }
    }

    /**
     * 反射规则命中后异步告知 LLM
     * <p>
     * 让 AI 知道刚才哪个反射规则触发了，可以说一句话反应（如"有人靠近了！"），
     * 但不执行任何新命令（[COMMAND:...] 会被过滤掉，只广播文字）。
     * <p>
     * 异步调度，不阻塞主线程。占用 busy 标记防并发。
     *
     * @param eventDescription 事件描述（如"反射规则 [r1] PLAYER_NEARBY 5 刚刚触发了..."）
     */
    public void notifyReflexTrigger(String eventDescription) {
        if (!plugin.getConfigManager().isConfigured()) return;
        // 异步调度，不阻塞反射规则检查主线程
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 占用 busy 标记，避免与正在进行的对话冲突
            if (!aiPlayer.getBusy().compareAndSet(false, true)) return;
            try {
                List<Map<String, String>> messages = new ArrayList<>();
                // system prompt：明确告知 LLM 只需知晓，不要输出命令
                Map<String, String> sys = new HashMap<>();
                sys.put("role", "system");
                sys.put("content", "你是 Minecraft 中的 AI 玩家 " + aiPlayer.getName()
                    + "。你的某个反射规则刚刚自动触发了（反射规则是本地快速执行的，不经过你思考）。"
                    + "现在把这个事件告知你，让你知晓。你可以自言自语一句话反应（如'有人靠近了！'、'血量告急！'），"
                    + "符合你的个性即可。但不要输出任何 [COMMAND:...] 命令——反射规则已经自动处理了动作，你不需要再做任何事。"
                    + "回复要简短（一两句话，不超过30字）。");
                messages.add(sys);
                // user message = 事件描述
                Map<String, String> user = new HashMap<>();
                user.put("role", "user");
                user.put("content", eventDescription);
                messages.add(user);

                // 调用 LLM（用非流式 chat 即可，通知不需要流式）
                String reply = plugin.getLlmClient().chat(messages);

                if (reply != null && !reply.trim().isEmpty()) {
                    // 过滤掉 [COMMAND:...] 文本（防止 LLM 误输出命令字符串污染聊天框）
                    String text = reply.replaceAll("\\[COMMAND:[^\\]]+\\]", "").trim();
                    if (!text.isEmpty()) {
                        // 回到主线程广播文字（Bukkit API 必须主线程调用）
                        final String finalText = text;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            aiPlayer.sayInChat(finalText);
                        });
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("notifyReflexTrigger 异常: " + e.getMessage());
            } finally {
                aiPlayer.getBusy().set(false);
            }
        });
    }

    private Map<String, String> makeMessage(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
