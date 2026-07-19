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
        ConfigManager cfg = plugin.getConfigManager();

        // 1. 准备消息列表
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示词（首次激活时包含游戏数据/初始状态）
        String systemPrompt = cfg.renderSystemPrompt(aiPlayer.getName());
        messages.add(makeMessage("system", systemPrompt));

        // 历史对话
        for (Map<String, String> h : aiPlayer.getConversationHistory()) {
            messages.add(h);
        }

        // 当前用户消息
        String prefix = speaker != null ? "玩家 " + speaker.getName() + " @你 说：" : "（系统提示）";
        messages.add(makeMessage("user", prefix + userMessage));

        if (cfg.isDebug()) {
            plugin.getLogger().info("=== LLM 请求 ===");
            for (Map<String, String> m : messages) {
                plugin.getLogger().info("[" + m.get("role") + "] " + m.get("content"));
            }
        }

        // 2. 调用 LLM
        String reply;
        try {
            reply = plugin.getLlmClient().chat(messages);
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
    }

    private Map<String, String> makeMessage(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
