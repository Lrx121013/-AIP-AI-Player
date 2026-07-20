package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.util.LocationUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 反射规则管理器
 * <p>
 * 维护单个 AI 玩家的所有反射规则，周期性检查轮询型触发器
 * （PLAYER_NEARBY / MOB_NEARBY / LOW_HEALTH / LOW_FOOD / TIME_PERIOD），
 * 事件型触发器（ON_DAMAGE / PLAYER_ATTACK / BLOCK_BREAK_NEARBY）由
 * ReflexListener 调用 {@link #triggerByEvent} 触发。
 * <p>
 * 规则触发时本地通过 CommandExecutor 执行动作，不调用 LLM。
 */
public class ReflexManager {

    private final AIPlayerPlugin plugin;
    private final AIPlayer owner;
    /** 保持插入顺序 */
    private final Map<String, ReflexRule> rules = new LinkedHashMap<>();
    private final Map<String, ReflexRule> rulesView = Collections.unmodifiableMap(rules);
    private BukkitTask checkTask;
    private final int maxRules;
    private final int minCooldownMs;
    private int nextId = 1;
    private final int checkIntervalTicks;

    public ReflexManager(AIPlayerPlugin plugin, AIPlayer owner) {
        this.plugin = plugin;
        this.owner = owner;
        this.maxRules = plugin.getConfigManager().getMaxReflexRules();
        this.minCooldownMs = plugin.getConfigManager().getReflexMinCooldownMs();
        this.checkIntervalTicks = plugin.getConfigManager().getReflexCheckInterval();
    }

    /**
     * 添加一条反射规则
     *
     * @param triggerName 触发器名字（不区分大小写，下划线可省略）
     * @param condition   触发条件参数
     * @param action      动作字符串（不含 [COMMAND: 包裹）
     * @param cooldownMs  冷却毫秒
     * @return 规则 ID（如 "r1"）
     */
    public synchronized String addRule(String triggerName, String condition,
                                       String action, int cooldownMs) {
        ReflexRule.TriggerType type = parseTriggerType(triggerName);
        if (type == null) {
            throw new RuntimeException("未知触发器类型: " + triggerName);
        }
        cooldownMs = Math.max(cooldownMs, minCooldownMs);
        if (rules.size() >= maxRules) {
            throw new RuntimeException("反射规则数已达上限 " + maxRules);
        }
        String id = "r" + (nextId++);
        ReflexRule rule = new ReflexRule(id, type, condition, action, cooldownMs);
        rules.put(id, rule);
        return id;
    }

    /** 解析触发器名字为 TriggerType，不区分大小写，下划线可省略；未知返回 null */
    private ReflexRule.TriggerType parseTriggerType(String name) {
        if (name == null) return null;
        String norm = name.trim().toUpperCase().replace("_", "");
        for (ReflexRule.TriggerType t : ReflexRule.TriggerType.values()) {
            if (t.name().replace("_", "").equals(norm)) return t;
        }
        return null;
    }

    /** 删除规则，返回是否删除成功 */
    public synchronized boolean removeRule(String id) {
        return rules.remove(id) != null;
    }

    /** 清空所有规则并重置 ID 生成器 */
    public synchronized void clearRules() {
        rules.clear();
        nextId = 1;
    }

    /** 启用/禁用某条规则，返回是否存在该规则 */
    public synchronized boolean toggleRule(String id, boolean enabled) {
        ReflexRule rule = rules.get(id);
        if (rule == null) return false;
        rule.setEnabled(enabled);
        return true;
    }

    /** 列出所有规则（人类可读字符串，冷却用秒显示） */
    public synchronized String listRules() {
        if (rules.isEmpty()) return "（暂无反射规则）";
        StringBuilder sb = new StringBuilder();
        for (ReflexRule r : rules.values()) {
            sb.append("- [").append(r.getId()).append("] ")
              .append(r.getTriggerType()).append(" ")
              .append(r.getCondition() == null ? "" : r.getCondition())
              .append(" → ").append(r.getAction())
              .append(" (冷却").append(String.format("%.1f", r.getCooldownMs() / 1000.0)).append("秒, ")
              .append(r.isEnabled() ? "启用" : "已禁用").append(")\n");
        }
        return sb.toString().trim();
    }

    /** 生成 prompt 摘要供 system prompt 注入 */
    public synchronized String getPromptSummary() {
        if (rules.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("你当前已定义的反射规则（自动执行，无需再思考）：\n");
        for (ReflexRule r : rules.values()) {
            sb.append("- [").append(r.getId()).append("] ")
              .append(r.getTriggerType()).append(" ")
              .append(r.getCondition() == null ? "" : r.getCondition())
              .append(" → ").append(r.getAction())
              .append(" (冷却").append(r.getCooldownMs() / 1000.0).append("秒, ")
              .append(r.isEnabled() ? "启用" : "已禁用").append(")\n");
        }
        return sb.toString().trim();
    }

    /** 启动周期检查任务，若已启动则返回 */
    public synchronized void startCheckTask() {
        if (checkTask != null) return;
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                checkAll();
            }
        };
        checkTask = runnable.runTaskTimer(plugin, checkIntervalTicks, checkIntervalTicks);
    }

    /** 周期检查所有轮询型规则 */
    private void checkAll() {
        try {
            Player entity = owner.getEntity();
            if (entity == null || !entity.isValid()) return;
            long now = System.currentTimeMillis();
            // 拷贝一份避免并发修改
            for (ReflexRule rule : new ArrayList<>(rules.values())) {
                // 跳过事件型触发器，由 ReflexListener 触发
                ReflexRule.TriggerType t = rule.getTriggerType();
                if (t == ReflexRule.TriggerType.ON_DAMAGE
                        || t == ReflexRule.TriggerType.PLAYER_ATTACK
                        || t == ReflexRule.TriggerType.BLOCK_BREAK_NEARBY) {
                    continue;
                }
                checkRule(rule, entity, now);
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("ReflexManager.checkAll 异常: " + e.getMessage());
        }
    }

    /** 检查单条轮询型规则是否满足触发条件，满足则执行动作并标记触发时间 */
    private void checkRule(ReflexRule rule, Player entity, long now) {
        try {
            if (!rule.canTrigger(now)) return;
            switch (rule.getTriggerType()) {
                case PLAYER_NEARBY: {
                    double r = safeParseDouble(rule.getCondition(), 5);
                    Player nearest = findNearestPlayer(entity, r);
                    if (nearest != null) {
                        executeAction(rule, null, nearest.getName(), null);
                        rule.markTriggered(now);
                    }
                    break;
                }
                case MOB_NEARBY: {
                    double r = safeParseDouble(rule.getCondition(), 5);
                    Monster nearest = findNearestMonster(entity, r);
                    if (nearest != null) {
                        executeAction(rule, null, null, nearest.getName());
                        rule.markTriggered(now);
                    }
                    break;
                }
                case LOW_HEALTH: {
                    double p = safeParseDouble(rule.getCondition(), 30);
                    double maxHealth = readMaxHealth(entity);
                    if (maxHealth <= 0) maxHealth = 20.0;
                    double ratio = entity.getHealth() / maxHealth;
                    if (ratio * 100 < p) {
                        executeAction(rule, null, null, null);
                        rule.markTriggered(now);
                    }
                    break;
                }
                case LOW_FOOD: {
                    int f = safeParseInt(rule.getCondition(), 10);
                    if (entity.getFoodLevel() < f) {
                        executeAction(rule, null, null, null);
                        rule.markTriggered(now);
                    }
                    break;
                }
                case TIME_PERIOD: {
                    String cond = rule.getCondition() == null ? "" : rule.getCondition().trim().toLowerCase();
                    long time = entity.getWorld().getTime();
                    boolean match = false;
                    if ("day".equals(cond)) {
                        match = time >= 0 && time < 13000;
                    } else if ("night".equals(cond)) {
                        match = time >= 13000 && time < 24000;
                    }
                    if (match) {
                        executeAction(rule, null, null, null);
                        rule.markTriggered(now);
                    }
                    break;
                }
                default:
                    return;
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("ReflexManager.checkRule 异常 [rule=" + rule.getId()
                    + "]: " + e.getMessage());
        }
    }

    /** 在半径 r 内找最近的非自身 Player，找不到返回 null */
    private Player findNearestPlayer(Player self, double r) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : self.getWorld().getNearbyEntities(self.getLocation(), r, r, r)) {
            if (!(e instanceof Player)) continue;
            if (e.equals(self)) continue;
            double d = LocationUtil.safeDistance(e.getLocation(), self.getLocation());
            if (d < minDist) {
                minDist = d;
                nearest = (Player) e;
            }
        }
        return nearest;
    }

    /** 在半径 r 内找最近的 Monster，找不到返回 null */
    private Monster findNearestMonster(Player self, double r) {
        Monster nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : self.getWorld().getNearbyEntities(self.getLocation(), r, r, r)) {
            if (!(e instanceof Monster)) continue;
            double d = LocationUtil.safeDistance(e.getLocation(), self.getLocation());
            if (d < minDist) {
                minDist = d;
                nearest = (Monster) e;
            }
        }
        return nearest;
    }

    /** 读取实体真实 maxHealth，失败回退 20.0 */
    private double readMaxHealth(org.bukkit.entity.LivingEntity entity) {
        try {
            org.bukkit.attribute.Attribute attr = org.bukkit.Registry.ATTRIBUTE
                    .get(org.bukkit.NamespacedKey.minecraft("max_health"));
            if (attr != null) {
                var inst = entity.getAttribute(attr);
                if (inst != null) return inst.getValue();
            }
        } catch (Throwable ignored) {}
        return 20.0;
    }

    /**
     * 事件型触发器统一入口，由 ReflexListener 调用
     *
     * @param type          触发类型（ON_DAMAGE / PLAYER_ATTACK / BLOCK_BREAK_NEARBY）
     * @param attackerName  攻击者名字（可空）
     * @param nearestPlayer 最近玩家名（可空）
     * @param nearestMob    最近怪物名（可空）
     */
    public synchronized void triggerByEvent(ReflexRule.TriggerType type,
                                            String attackerName,
                                            String nearestPlayer,
                                            String nearestMob) {
        try {
            long now = System.currentTimeMillis();
            for (ReflexRule rule : new ArrayList<>(rules.values())) {
                if (rule.getTriggerType() == type && rule.canTrigger(now)) {
                    executeAction(rule, attackerName, nearestPlayer, nearestMob);
                    rule.markTriggered(now);
                }
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("ReflexManager.triggerByEvent 异常: " + e.getMessage());
        }
    }

    /**
     * 执行规则动作：替换占位符后通过 CommandExecutor 执行
     */
    private void executeAction(ReflexRule rule, String attackerName,
                               String nearestPlayer, String nearestMob) {
        try {
            String action = rule.getAction();
            if (action == null) return;
            if (attackerName != null) action = action.replace("<attacker>", attackerName);
            if (nearestPlayer != null) action = action.replace("<nearest_player>", nearestPlayer);
            if (nearestMob != null) action = action.replace("<nearest_mob>", nearestMob);
            action = action.replace("<self>", owner.getName());
            plugin.getCommandExecutor().execute(owner, "[COMMAND:" + action + "]");
        } catch (Throwable e) {
            plugin.getLogger().warning("ReflexManager.executeAction 异常 [rule=" + rule.getId()
                    + "]: " + e.getMessage());
        }
    }

    /** 取消检查任务并清空所有规则 */
    public synchronized void cancel() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        rules.clear();
        nextId = 1;
    }

    private double safeParseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }

    private int safeParseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
