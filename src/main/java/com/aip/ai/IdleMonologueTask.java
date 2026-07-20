package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.story.StoryPhase;
import com.aip.story.StoryState;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * v2.2.0：AI 空闲自言自语调度器
 * <p>
 * 每 5 秒扫描一次所有 AI 玩家：
 *   - 跳过 busy / 实体失效 / StoryState 为 null / phase 为 DORMANT 或 COMPLETED 的 AI
 *   - 对每个符合条件的 AI，按配置的 min~max 秒随机阈值节流触发
 *   - 触发时调 LLM 输出一句内心独白（OS），主线程广播
 *   - 失败/异常静默，不影响主流程
 */
public class IdleMonologueTask extends BukkitRunnable {

    private final AIPlayerPlugin plugin;
    /** 每个 AI 最近一次自言自语的时间戳（ms） */
    private final Map<UUID, Long> lastMonologue = new HashMap<>();
    private volatile boolean cancelled = false;

    public IdleMonologueTask(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (cancelled) return;
        long now = System.currentTimeMillis();

        int minSec = plugin.getConfigManager().getIdleMonologueMinSeconds();
        int maxSec = plugin.getConfigManager().getIdleMonologueMaxSeconds();
        if (minSec < 1) minSec = 1;
        if (maxSec < minSec) maxSec = minSec;

        for (AIPlayer ai : plugin.getAiPlayerManager().getAll()) {
            try {
                if (ai == null) continue;
                if (ai.getBusy().get()) continue;
                if (ai.getEntity() == null || !ai.getEntity().isValid()) continue;

                StoryState state = ai.getStoryState();
                if (state == null) continue;
                StoryPhase phase = state.getCurrentPhase();
                if (phase == null || phase == StoryPhase.DORMANT || phase == StoryPhase.COMPLETED) continue;

                UUID id = ai.getEntityId();
                Long last = lastMonologue.get(id);
                if (last == null) {
                    // 第一次：先记录当前时间，下一轮再触发，避免 spawn 立刻说话
                    lastMonologue.put(id, now);
                    continue;
                }
                long elapsed = now - last;
                // 随机阈值 [min, max] 秒
                int thresholdSec = ThreadLocalRandom.current().nextInt(minSec, maxSec + 1);
                if (elapsed < thresholdSec * 1000L) continue;

                // 通过：触发
                lastMonologue.put(id, now);

                final AIPlayer target = ai;
                final StoryPhase currentPhase = phase;
                final String aiName = ai.getName();

                // 异步调 LLM（chatOnce 内部会占 busy，主线程检查已通过即可）
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        StringBuilder prompt = new StringBuilder();
                        prompt.append("你是 AIP ").append(aiName)
                                .append("。当前剧情阶段 ").append(currentPhase.getDisplayName())
                                .append("。用一句话（≤20 字）表达你此刻的心理活动（OS），不要输出 [COMMAND:...]");
                        // v2.2.1：注入最近心理活动 + 不要重复
                        try {
                            java.util.List<String> recent = target.getRecentMessages(3);
                            if (recent != null && !recent.isEmpty()) {
                                prompt.append("\n\n【v2.2.1 复读机防护】你最近的 3 句心理活动：\n");
                                for (int i = 0; i < recent.size(); i++) {
                                    prompt.append("  ").append(i + 1).append(". ").append(recent.get(i)).append("\n");
                                }
                                prompt.append("请用**完全不同**的方式表达，不要重复。");
                            }
                        } catch (Exception ignored) {}
                        String reply = target.getConversationManager().chatOnce(target, prompt.toString());
                        if (reply == null || reply.isEmpty()) return;
                        String text = reply.replaceAll("\\[COMMAND:[^\\]]+\\]", "").trim();
                        if (text.isEmpty()) return;
                        if (text.length() > 50) text = text.substring(0, 50);
                        final String finalText = text;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (cancelled) return;
                            Bukkit.broadcastMessage("§7" + aiName + " 想着：" + finalText);
                        });
                    } catch (Exception e) {
                        // 静默：自言自语失败不影响主流程
                    }
                });
            } catch (Exception e) {
                // 静默
            }
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        cancelled = true;
        super.cancel();
    }
}
