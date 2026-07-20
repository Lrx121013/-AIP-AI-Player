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

    private Map<String, String> makeMessage(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
