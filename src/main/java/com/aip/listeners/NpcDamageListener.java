package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.ConversationManager;
import com.aip.ai.GameDataCollector;
import com.aip.ai.MemoryRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监听 NPC 被攻击事件，让 NPC 立即反应
 * <p>
 * 被攻击时（同一 tick 内）：
 *   1. 反击攻击者（按配置 attack-damage）
 *   2. 在聊天里喊话（被打了！为什么要打我！等）
 *   3. 异步通知 LLM 让它决定后续行为（追击/逃跑/对话）
 * <p>
 * 这是一个"反应式"机制：不依赖 LLM 的回复延迟，立刻有物理反馈。
 */
public class NpcDamageListener implements Listener {

    private final AIPlayerPlugin plugin;

    /** 每个 NPC 最近一次反击的时间戳（ms），避免被连击时疯狂反击 */
    private final ConcurrentHashMap<UUID, Long> lastCounterAttack = new ConcurrentHashMap<>();

    /** 反击循环打破：每个 NPC 最近一次反击的目标（UUID），避免两个 AI 互打形成死循环 */
    private final ConcurrentHashMap<UUID, UUID> lastCounterAttackTarget = new ConcurrentHashMap<>();
    /** 反击循环打破：每个 NPC 最近一次反击的时间戳（ms） */
    private final ConcurrentHashMap<UUID, Long> lastCounterAttackTargetTime = new ConcurrentHashMap<>();
    /** 反击循环打破冷却（ms），同一对 damager 在该时间内不重复反击 */
    private static final long COUNTER_LOOP_BREAK_MS = 1500;

    /** 反击冷却（ms） */
    private static final long COUNTER_COOLDOWN_MS = 800;

    /** NPC 喊话冷却（ms），避免刷屏 */
    private final ConcurrentHashMap<UUID, Long> lastShout = new ConcurrentHashMap<>();
    private static final long SHOUT_COOLDOWN_MS = 2000;

    /** NPC 被打时的喊话候选 */
    private static final String[] HURT_LINES = {
            "哎哟！你干嘛打我？",
            "住手！我是无辜的！",
            "疼！别打了，有事好商量！",
            "你为什么攻击我？！",
            "停！我可以做你的朋友！",
            "再打我就还手了！",
            "我警告你，别动手！"
    };

    /** NPC 反击时的喊话候选 */
    private static final String[] COUNTER_LINES = {
            "看招！",
            "你也尝尝这个！",
            "逼我动手！",
            "我反击了！",
            "是你先动手的！"
    };

    public NpcDamageListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNpcDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        UUID victimId = victim.getUniqueId();

        AIPlayer ai = plugin.getAiPlayerManager().getByEntity(victimId);
        if (ai == null) return; // 不是我们的 NPC

        // 取攻击者
        org.bukkit.entity.Entity damagerEntity = event.getDamager();
        LivingEntity attacker = null;
        if (damagerEntity instanceof LivingEntity le) {
            attacker = le;
        } else if (damagerEntity instanceof org.bukkit.entity.Projectile proj) {
            if (proj.getShooter() instanceof LivingEntity le) attacker = le;
        }
        if (attacker == null) return;

        // 功能 9：被攻击时情绪下降
        ai.adjustMood(-10);

        // P3：长期记忆——被玩家攻击时记录
        if (attacker instanceof Player) {
            String attackerName = attacker.getName();
            ai.getMemory().addRecord(MemoryRecord.Type.ATTACK,
                    "被 " + attackerName + " 攻击", attackerName);
            // P3：玩家档案——记录攻击行为
            plugin.getPlayerProfileManager().recordAttack((Player) attacker, ai);
            // 启动 15 秒追击任务（被玩家攻击后）
            startPursuit(ai, (Player) attacker);
        }

        // 1. 立即喊话（被攻击）
        shout(ai, HURT_LINES);

        // 2. 立即反击（带冷却，避免疯狂反击）
        if (plugin.getConfigManager().isCounterattack()) {
            long now = System.currentTimeMillis();
            // 1. 基础冷却：800ms 内同 NPC 不重复反击
            Long last = lastCounterAttack.get(victimId);
            if (last == null || now - last > COUNTER_COOLDOWN_MS) {
                // 2. 反制循环打破：1500ms 内对同一 damager 不重复反击（防止两个 AI 互打）
                UUID lastTarget = lastCounterAttackTarget.get(victimId);
                Long lastTargetTime = lastCounterAttackTargetTime.get(victimId);
                boolean inLoopBreak = lastTarget != null
                        && lastTarget.equals(attacker.getUniqueId())
                        && lastTargetTime != null
                        && now - lastTargetTime < COUNTER_LOOP_BREAK_MS;
                if (!inLoopBreak) {
                    lastCounterAttack.put(victimId, now);
                    lastCounterAttackTarget.put(victimId, attacker.getUniqueId());
                    lastCounterAttackTargetTime.put(victimId, now);
                    // 反击伤害 = 配置的攻击伤害
                    double dmg = plugin.getConfigManager().getAttackDamage();
                    if (attacker == null || !attacker.isValid()) return;
                    if (victim == null || !victim.isValid() || victim.isDead()) return;
                    attacker.damage(dmg, victim);
                    shout(ai, COUNTER_LINES);
                }
            }
        }

        // 3. 异步通知 LLM：我刚被 X 攻击了，怎么办？
        //    先在主线程采集游戏数据，再异步调用 LLM，最后回主线程执行命令
        final LivingEntity finalAttacker = attacker;
        final double finalDamage = event.getFinalDamage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (victim == null || !victim.isValid() || victim.isDead()) return;
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(ai);
                String attackerName = finalAttacker instanceof Player p ? p.getName() :
                        finalAttacker.getName();
                String prompt = "（紧急事件：你刚刚被 " + attackerName
                        + " 攻击了，受到 " + String.format("%.1f", finalDamage)
                        + " 点伤害。你现在的血量是 " + String.format("%.1f", victim.getHealth())
                        + "。请立刻决定下一步：反击/逃跑/对话求和？只回复简短决策和命令。）\n"
                        + gameData;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        ConversationManager cm = new ConversationManager(plugin, ai);
                        String reply = cm.chat(prompt,
                                finalAttacker instanceof Player p ? p : null);
                        final String finalReply = reply;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getCommandExecutor().execute(ai, finalReply);
                        });
                    } catch (Exception e) {
                        plugin.getLogger().warning("NPC 反应 LLM 调用失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("NPC 反应采集数据失败: " + e.getMessage());
            }
        });
    }

    /**
     * 启动被攻击后的追击任务：在 pursuit-duration-ms 内持续追击攻击者。
     * 若已有追击任务在运行，先取消（重置计时）。
     *
     * @param aiPlayer 被攻击的 AI
     * @param attacker 攻击者玩家
     */
    private void startPursuit(AIPlayer aiPlayer, Player attacker) {
        // 取消旧的追击任务（重置计时）
        BukkitTask existing = aiPlayer.getPursuitTask();
        if (existing != null) {
            try {
                existing.cancel();
            } catch (IllegalStateException ignored) {
                // 任务可能已完成，忽略
            }
        }

        final String attackerName = attacker.getName();
        final long startTime = System.currentTimeMillis();
        final long durationMs = plugin.getConfigManager().getPursuitDurationMs();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 超时自动取消
                    if (System.currentTimeMillis() - startTime >= durationMs) {
                        cancel();
                        aiPlayer.setPursuitTask(null);
                        return;
                    }
                    // AI 实体失效 → 取消
                    Player v = aiPlayer.getEntity();
                    if (v == null || !v.isValid()) {
                        cancel();
                        aiPlayer.setPursuitTask(null);
                        return;
                    }
                    // 攻击者离线 → 取消
                    Player target = Bukkit.getPlayerExact(attackerName);
                    if (target == null || !target.isOnline()) {
                        cancel();
                        aiPlayer.setPursuitTask(null);
                        return;
                    }
                    // AI 正在 LLM 决策中 → 跳过本轮（不取消）
                    if (aiPlayer.getBusy().get()) return;

                    // 计算距离
                    double dist;
                    try {
                        dist = v.getLocation().distance(target.getLocation());
                    } catch (Exception e) {
                        return; // 跨世界等异常，跳过本轮
                    }

                    // 距离 > 4 走向攻击者，≤ 4 攻击
                    if (dist > 4) {
                        plugin.getCommandExecutor().execute(aiPlayer, "[COMMAND:walk " + attackerName + "]");
                    } else {
                        plugin.getCommandExecutor().execute(aiPlayer, "[COMMAND:attack " + attackerName + "]");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("追击任务异常: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 立即开始，每 20 tick (1 秒) 一次

        aiPlayer.setPursuitTask(task);
    }

    /** 随机喊一句话（带冷却 + 30 秒去重） */
    private void shout(AIPlayer ai, String[] lines) {
        if (ai == null) return;
        Player entity = ai.getEntity();
        if (entity == null || !entity.isValid()) return;
        long now = System.currentTimeMillis();
        UUID npcId = ai.getEntityId();
        Long last = lastShout.get(npcId);
        if (last != null && now - last < SHOUT_COOLDOWN_MS) return;
        lastShout.put(npcId, now);

        String line = lines[(int) (Math.random() * lines.length)];
        // 通过 sayInChat 广播，自动应用 30 秒去重
        ai.sayInChat(line);
    }
}
