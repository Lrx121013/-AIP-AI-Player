package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.story.StoryPhase;
import com.aip.story.StoryState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v2.1.4 复仇对话生成器
 * <p>
 * AI 复活后根据当前 StoryPhase 生成符合剧情的"复仇感"对话，
 * 取代 v2.1.3 中"我刚刚被生成到这个世界"这种生硬的开场白。
 * <p>
 * 设计原则：
 *   - 仅在 revive 路径调用，不在 spawn 路径调用
 *   - 异步调 LLM，避免主线程阻塞
 *   - 过滤 [COMMAND:...] 标记（不应在复仇对话中发出新命令）
 *   - 30 字内短句
 *   - 5 秒内同 AI 节流，避免连续触发
 *   - COMPLETED 阶段不生成
 */
public class RevengeLine {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\[COMMAND:[^\\]]*\\]", Pattern.CASE_INSENSITIVE);
    private static final Set<String> PUNCT_INSIDE = new HashSet<>(Arrays.asList("。", "，", "！", "？", "；", "、", ".", ",", "!", "?", ";", ":", "：", "~", "～", "…", "..."));

    /** 5 秒内同 AI 最多生成 1 次复仇对话 */
    private static final ConcurrentHashMap<UUID, Long> lastGenerateTime = new ConcurrentHashMap<>();
    private static final long THROTTLE_MS = 5_000L;

    /**
     * 异步生成复仇对话并广播
     *
     * @param plugin  插件实例
     * @param ai      复活的 AI
     * @param killer  击杀者（可为 null，未知）
     * @param delayTicks 延迟多少 tick 后生成（让玩家先看到 AI 复活实体）
     */
    public static void generateAndSay(AIPlayerPlugin plugin, AIPlayer ai, Player killer, long delayTicks) {
        if (plugin == null || ai == null) return;

        // StoryState 为 null 或 COMPLETED → 不说话
        StoryState state = ai.getStoryState();
        if (state == null) return;
        if (state.getCurrentPhase() == StoryPhase.COMPLETED) return;

        // 5 秒节流
        UUID id = ai.getEntityId();
        long now = System.currentTimeMillis();
        Long last = lastGenerateTime.get(id);
        if (last != null && now - last < THROTTLE_MS) return;
        lastGenerateTime.put(id, now);

        String aiName = ai.getName();
        String killerName = killer != null ? killer.getName() : "某人";
        // v2.2.7：火柴盒版移除了 getAiDeathCount() 字段（不再因 AI 死亡推进阶段）
        int deathCount = 0;
        StoryPhase phase = state.getCurrentPhase();

        String prompt = buildPrompt(aiName, killerName, deathCount, phase);

        // 延迟 N tick 后调 LLM（让玩家先看到 AI 复活实体）
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                String reply = ai.getConversationManager().chatOnce(ai, prompt);
                String clean = cleanReply(reply);
                if (clean == null || clean.isEmpty()) return;

                // 回主线程广播
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // 写入对话历史作为 assistant（addHistory 内部去重，30 秒内同句不重复）
                        ai.addHistory("assistant", clean);
                        // 全服广播
                        Bukkit.broadcastMessage("§c[" + aiName + "] §f" + clean);
                    } catch (Exception e) {
                        plugin.getLogger().warning("复仇对话广播失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("复仇对话生成失败: " + e.getMessage());
            }
        }, Math.max(1L, delayTicks));
    }

    /**
     * 根据阶段构建 prompt
     */
    private static String buildPrompt(String aiName, String killerName, int deathCount, StoryPhase phase) {
        // v2.2.7：火柴盒版不再有 DORMANT/AWAKENING/AERIAL_ASSAULT/PVP_DUEL/RULEBOOK/DICTATORSHIP/BETRAYAL
        // 简化为通用复仇对话（按当前章节自适应）
        // 限制 token：保持 ≤200 字
        StringBuilder sb = new StringBuilder(160);
        sb.append("你是 ").append(aiName).append("，刚刚被 ").append(killerName).append(" 击杀，");
        if (deathCount > 0) {
            sb.append("这是第 ").append(deathCount).append(" 次被你杀死。");
        } else {
            sb.append("但你又复活了。");
        }
        sb.append("当前阶段：").append(phase == null ? "未知" : phase.getDisplayName())
          .append("。用一句话（≤30字）表达你此刻的复仇心理，不要输出 [COMMAND:...]。");
        return sb.toString();
    }

    /**
     * 清理 LLM 回复：
     *   - 去除 [COMMAND:...] 标记
     *   - 取第一行
     *   - 截断到 60 字
     *   - 去除首尾空白
     */
    private static String cleanReply(String raw) {
        if (raw == null) return null;
        // 过滤 [COMMAND:...]
        String s = COMMAND_PATTERN.matcher(raw).replaceAll("");
        // 取第一个非空行
        for (String line : s.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // 去除引号、书名号等
            line = line.replaceAll("^[\\\"「『]+|[\\\"」』]+$", "");
            // 截断到 60 字
            if (line.length() > 60) {
                line = line.substring(0, 60) + "...";
            }
            return line;
        }
        return null;
    }

    /**
     * 测试用：清理节流 map（reload 时调用）
     */
    public static void clearThrottle() {
        lastGenerateTime.clear();
    }
}
