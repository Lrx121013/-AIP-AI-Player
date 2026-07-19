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
        }

        // 1. 立即喊话（被攻击）
        shout(victimId, ai.getName(), HURT_LINES);

        // 2. 立即反击（带冷却，避免疯狂反击）
        if (plugin.getConfigManager().isCounterattack()) {
            long now = System.currentTimeMillis();
            Long last = lastCounterAttack.get(victimId);
            if (last == null || now - last > COUNTER_COOLDOWN_MS) {
                lastCounterAttack.put(victimId, now);
                // 反击伤害 = 配置的攻击伤害
                double dmg = plugin.getConfigManager().getAttackDamage();
                if (attacker == null || !attacker.isValid()) return;
                if (victim == null || !victim.isValid() || victim.isDead()) return;
                attacker.damage(dmg, victim);
                shout(victimId, ai.getName(), COUNTER_LINES);
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

    /** 随机喊一句话（带冷却） */
    private void shout(UUID npcId, String npcName, String[] lines) {
        long now = System.currentTimeMillis();
        Long last = lastShout.get(npcId);
        if (last != null && now - last < SHOUT_COOLDOWN_MS) return;
        lastShout.put(npcId, now);

        String line = lines[(int) (Math.random() * lines.length)];
        Bukkit.broadcastMessage("<" + npcName + "> " + line);
    }
}
