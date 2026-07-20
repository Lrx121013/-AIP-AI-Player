package com.aip.story;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.NpcHelper;
import com.aip.ai.Personality;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事模式中央调度器
 * <p>
 * 管理所有 AI 的故事状态，监听 AI 死亡 / 玩家死亡事件，
 * 根据计数推进阶段，执行各阶段专属动作。
 * <p>
 * 启动 3 个调度器：
 *   - aerialAssaultTask：每 4 秒，轰炸 / 倒计时
 *   - pvpDuelTask：每 2 秒，PVP 行为
 *   - dictatorshipTask：每 30 秒，下达命令
 * <p>
 * 调度器在 onEnable 时启动，onDisable 时由插件统一 cancelTasks。
 */
public class StoryManager {

    private final AIPlayerPlugin plugin;
    /** ownerId → StoryState 映射 */
    private final Map<UUID, StoryState> states = new ConcurrentHashMap<>();
    private BukkitTask aerialTask;
    private BukkitTask pvpTask;
    private BukkitTask dictatorshipTask;
    private BukkitTask betrayalTask;
    /** v2.2.2：觉醒阶段 AI 主动攻击调度器（每 3 秒扫描） */
    private BukkitTask awakeningTask;
    private volatile boolean initialized = false;

    public StoryManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动 3 个调度器
     */
    public void init() {
        if (initialized) return;
        initialized = true;

        long aerialInterval = plugin.getConfigManager().getAerialTickMs() / 50;  // ms → tick
        if (aerialInterval < 1) aerialInterval = 80;  // 默认 4 秒

        aerialTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAerialAssault, aerialInterval, aerialInterval);
        pvpTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickPvpDuel, 40L, 40L);
        dictatorshipTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDictatorship, 600L, 600L);
        betrayalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBetrayal, 100L, 100L);
        // v2.2.2：觉醒阶段调度器（每 3 秒扫描，AI 主动飞向玩家攻击）
        awakeningTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAwakening, 60L, 60L);
    }

    public void cancel() {
        if (aerialTask != null) aerialTask.cancel();
        if (pvpTask != null) pvpTask.cancel();
        if (dictatorshipTask != null) dictatorshipTask.cancel();
        if (betrayalTask != null) betrayalTask.cancel();
        if (awakeningTask != null) awakeningTask.cancel();
        initialized = false;
    }

    /**
     * 注册一个新 AI 的故事状态
     */
    public void registerStory(AIPlayer ai) {
        if (ai == null) return;
        StoryState state = new StoryState(ai.getEntityId());
        states.put(ai.getEntityId(), state);
        ai.setStoryState(state);
    }

    /**
     * 注销一个 AI 的故事状态
     */
    public void unregisterStory(UUID ownerId) {
        if (ownerId == null) return;
        states.remove(ownerId);
    }

    /**
     * v2.1.4 实体 UUID 迁移：AI 死后 Citizens 重新分配新 UUID，把 states map 的 key 从 oldId 改为 newId
     * <p>
     * 保留同一 StoryState 对象引用，让死亡计数、剧情阶段、规则之书状态等跨死亡保留。
     */
    public void rebindOwner(UUID oldId, UUID newId) {
        if (oldId == null || newId == null) return;
        if (oldId.equals(newId)) return;  // 没变就不动
        StoryState state = states.remove(oldId);
        if (state != null) {
            states.put(newId, state);
            plugin.getLogger().info("[Story] rebindOwner " + oldId + " -> " + newId
                    + " (phase=" + state.getCurrentPhase() + ", aiDeaths=" + state.getAiDeathCount() + ")");
        }
    }

    public StoryState getState(UUID ownerId) {
        return states.get(ownerId);
    }

    /**
     * v2.2.2：获取所有 StoryState（用于 StoryModeCommandInterceptor 检查是否有 AI 已觉醒）
     */
    public java.util.Collection<StoryState> getAllStates() {
        return states.values();
    }

    /**
     * 由 AiDeathListener 调用
     */
    public void onAiDeath(AIPlayer ai, Player killer) {
        if (ai == null) return;
        StoryState state = states.get(ai.getEntityId());
        if (state == null) return;

        // 仅邪恶 AI 走故事模式流程
        if (!isStoryEligible(ai)) return;

        // COMPLETED 后不再计数
        if (state.getCurrentPhase() == StoryPhase.COMPLETED) return;

        // 觉醒只在 DORMANT 状态下被触发
        if (state.getCurrentPhase() == StoryPhase.DORMANT) {
            state.setAiDeathCount(state.getAiDeathCount() + 1);
            int kills = state.getAiDeathCount();
            int required = plugin.getConfigManager().getAwakeningKills();
            int remaining = Math.max(0, required - kills);
            if (remaining > 0) {
                plugin.getLogger().info("[Story] " + ai.getName() + " 死亡 " + kills + "/" + required + "（觉醒还需 " + remaining + " 次）");
            } else {
                plugin.getLogger().info("[Story] " + ai.getName() + " 死亡 " + kills + " 次（即将觉醒）");
            }

            if (kills >= required) {
                if (state.transitionTo(StoryPhase.AWAKENING)) {
                    String killerName = killer != null ? killer.getName() : "未知";
                    StageAction.broadcast("§c§l[剧情] §4" + ai.getName() + " §c被 " + killerName + " 击杀了 " + kills + " 次后……觉醒了！");
                    StageAction.say(ai, "我受够了！我要开始反击！");
                    // v2.2.1：觉醒切模式改为 deferred —— 死亡时 NPC 已删除，命令无效
                    // 改为设 pending 标记，由 AIPlayerManager.revive 复活完成后再执行
                    state.setAwakeningPending(true);
                    state.setPendingKillerName(killer != null ? killer.getName() : null);
                    plugin.getLogger().info("[Story] 觉醒切模式 deferred（pending=true，等复活后执行）");
                    // 立即攻击 killer
                    if (killer != null && killer.isOnline()) {
                        StageAction.runCommand(ai, "attack " + killer.getName());
                    }
                    notifyLlm(ai, "你的剧情推进：你被 " + killerName + " 击杀了 " + kills + " 次，你已觉醒。你现在要开始攻击他。");
                }
            }
        }
    }

    /**
     * 由 NpcKillListener 调用
     */
    public void onPlayerDeathByAi(AIPlayer ai, Player victim) {
        if (ai == null || victim == null) return;
        StoryState state = states.get(ai.getEntityId());
        if (state == null) return;
        if (!isStoryEligible(ai)) return;
        if (state.getCurrentPhase() == StoryPhase.COMPLETED) return;

        StoryPhase phase = state.getCurrentPhase();

        if (phase == StoryPhase.AWAKENING) {
            state.setPlayerKillCount(state.getPlayerKillCount() + 1);
            int kills = state.getPlayerKillCount();
            int required = plugin.getConfigManager().getAerialKills();
            int remaining = Math.max(0, required - kills);
            plugin.getLogger().info("[Story] " + ai.getName() + " 杀玩家 " + kills + "/" + required + "（" + (remaining > 0 ? "还需 " + remaining + " 次进入空袭" : "即将进入空袭阶段") + "）");

            if (kills >= required) {
                if (state.transitionTo(StoryPhase.AERIAL_ASSAULT)) {
                    // 初始化轰炸次数
                    state.setAerialBombsRemaining(plugin.getConfigManager().getAerialBombCount());
                    // 强制玩家生存模式
                    StageAction.runCommand(ai, "force_survival_player " + victim.getName());
                    // 给自己 fly + creative
                    StageAction.runCommand(ai, "gamemode creative");
                    StageAction.runCommand(ai, "fly on");
                    StageAction.broadcast("§4§l[剧情] §c" + ai.getName() + " §c飞向天空，开始了对 " + victim.getName() + " 的空中轰炸！");
                    StageAction.say(ai, "你躲得够久了，让我从天上解决你！");
                    notifyLlm(ai, "你的剧情推进：你成功击杀了玩家 " + kills + " 次，现在你飞向天空开始空中轰炸阶段。持续 3.5 分钟，用 TNT 和方块轰炸玩家，玩家必须躲避。");
                }
            }
        } else if (phase == StoryPhase.PVP_DUEL) {
            state.setPlayerKillCount(state.getPlayerKillCount() + 1);
            int kills = state.getPlayerKillCount();
            int required = plugin.getConfigManager().getPvpPlayerDeaths();
            plugin.getLogger().info("[Story] " + ai.getName() + " PVP 杀玩家 " + kills + "/" + required);

            if (kills >= required) {
                if (state.transitionTo(StoryPhase.RULEBOOK)) {
                    // 重置 playerKillCount 供后续阶段使用
                    state.setPlayerKillCount(0);
                    state.setRulebookDelivered(true);
                    // 关闭飞行 + 创造模式
                    StageAction.runCommand(ai, "fly off");
                    StageAction.runCommand(ai, "gamemode survival");
                    // 交出制度之书
                    StageAction.runCommand(ai, "give_rulebook " + victim.getName());
                    StageAction.broadcast("§6§l[剧情] §e" + ai.getName() + " §e杀死了 " + victim.getName() + " " + kills + " 次，递给他一本 §4§lAI 制度之书§e，要求他阅读！");
                    StageAction.say(ai, "你已经输了。这本制度之书你必须读完，否则……");
                    notifyLlm(ai, "你的剧情推进：你成功击杀了玩家 " + kills + " 次，现在进入制度统治阶段。你已经把一本《AI 制度之书》交给玩家。等待他读完。");
                }
            }
        }
    }

    /**
     * 由 RulebookListener 调用
     */
    public void onRulebookRead(AIPlayer ai, Player reader) {
        if (ai == null || reader == null) return;
        StoryState state = states.get(ai.getEntityId());
        if (state == null) return;
        if (state.getCurrentPhase() != StoryPhase.RULEBOOK) return;

        state.setRulebookRead(true);
        if (state.transitionTo(StoryPhase.DICTATORSHIP)) {
            state.setDictatorshipOrdersGiven(0);
            StageAction.broadcast("§6§l[剧情] §e" + reader.getName() + " §e读完了 §4§lAI 制度之书§e。" + ai.getName() + " 开始下达命令！");
            StageAction.say(ai, "很好，你已经读完制度。现在开始执行你的命令。");
            notifyLlm(ai, "你的剧情推进：玩家已经读完制度。现在进入独裁命令阶段。你将根据意愿给玩家下命令，玩家必须完成，否则会受到惩罚。");
        }
    }

    /**
     * 空中轰炸调度
     */
    private void tickAerialAssault() {
        long now = System.currentTimeMillis();
        for (StoryState state : states.values()) {
            if (state.getCurrentPhase() != StoryPhase.AERIAL_ASSAULT) continue;
            AIPlayer ai = findAiByUuid(state.getOwnerId());
            if (ai == null) continue;
            Player entity = ai.getEntity();
            if (entity == null || !entity.isValid()) continue;

            long elapsed = now - state.getPhaseStartTime();
            long duration = plugin.getConfigManager().getAerialDurationMs();
            if (elapsed >= duration) {
                // 阶段结束 → PVP_DUEL
                if (state.transitionTo(StoryPhase.PVP_DUEL)) {
                    state.setPlayerKillCount(0);
                    // 降下 + 装备顶级
                    StageAction.runCommand(ai, "fly off");
                    StageAction.runCommand(ai, "gamemode survival");
                    StageAction.runCommand(ai, "equip_netherite_set");
                    // 强制无敌关闭（确保 PVP 公平）
                    entity.setInvulnerable(false);
                    StageAction.broadcast("§6§l[剧情] §e" + ai.getName() + " §e降落在地面，穿上了顶级下界合金，准备与你进行顶级 PVP！");
                    StageAction.say(ai, "是时候正面对决了！");
                    notifyLlm(ai, "你的剧情推进：空中轰炸 3.5 分钟结束，你降落在地面并装备了顶级下界合金。现在进入 PVP 顶级对决阶段，你需要杀死玩家 2 次。");
                }
            } else {
                // 仍在轰炸：找最近玩家发射 TNT
                int remaining = state.getAerialBombsRemaining();
                if (remaining > 0) {
                    Player target = StageAction.getNearestPlayer(ai);
                    if (target != null) {
                        StageAction.runCommand(ai, "fly_bomb_player " + target.getName());
                        state.setAerialBombsRemaining(remaining - 1);
                        // v2.2.0：50% 概率 say 嘲讽（sayInChat 内部有 30 秒去重）
                        if (Math.random() < 0.5) {
                            String[] taunts = {"§c给我下来！", "§c你躲哪去了？", "§c这才是开始。"};
                            String taunt = taunts[(int) (Math.random() * taunts.length)];
                            ai.sayInChat(taunt);
                        }
                    }
                }
            }
        }
    }

    /**
     * PVP 对决调度
     */
    private void tickPvpDuel() {
        for (StoryState state : states.values()) {
            if (state.getCurrentPhase() != StoryPhase.PVP_DUEL) continue;
            AIPlayer ai = findAiByUuid(state.getOwnerId());
            if (ai == null) continue;
            Player entity = ai.getEntity();
            if (entity == null || !entity.isValid()) continue;

            // 血量 < 50% 时 heal（每 30 秒一次）
            if (entity.getHealth() < entity.getMaxHealth() * 0.5) {
                long now = System.currentTimeMillis();
                long elapsed = now - state.getPhaseStartTime();
                if (elapsed > 30000 || (state.getPlayerKillCount() == 0 && elapsed > 5000)) {
                    StageAction.runCommand(ai, "heal 20");
                    state.setPhaseStartTime(now);  // 刷新节流
                }
            }

            // 主 AIP 走 walk / attack / 动作
            processPvpDuelAi(ai);

            // 盟军也参与攻击
            try {
                if (plugin.getAllyManager() != null) {
                    for (AIPlayer ally : plugin.getAllyManager().getAllies(ai)) {
                        processPvpDuelAi(ally);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * v2.2.0：处理单个 PVP_DUEL AI 的 walk / attack / 动作
     * <p>
     * 30% 概率插入动作（swing / jump / emote），否则按距离走 walk 或 attack。
     * 盟军复用同一逻辑。
     */
    private void processPvpDuelAi(AIPlayer ai) {
        if (ai == null) return;
        Player entity = ai.getEntity();
        if (entity == null || !entity.isValid()) return;
        Player target = StageAction.getNearestPlayer(ai);
        if (target == null) return;
        // 30% 概率插入动作（swing / jump / emote）
        if (Math.random() < 0.3) {
            String[] actions = {"swing", "jump", "emote angry"};
            StageAction.runCommand(ai, actions[(int) (Math.random() * actions.length)]);
            return;
        }
        try {
            double dist = target.getLocation().distance(entity.getLocation());
            if (dist > 4.0) {
                StageAction.runCommand(ai, "walk " + target.getName());
            } else {
                StageAction.runCommand(ai, "attack " + target.getName());
            }
        } catch (Exception ignored) {}
    }

    /**
     * 独裁命令调度
     */
    private void tickDictatorship() {
        for (StoryState state : states.values()) {
            if (state.getCurrentPhase() != StoryPhase.DICTATORSHIP) continue;
            AIPlayer ai = findAiByUuid(state.getOwnerId());
            if (ai == null) continue;
            Player entity = ai.getEntity();
            if (entity == null || !entity.isValid()) continue;

            Player target = StageAction.getNearestPlayer(ai);
            if (target == null) continue;

            state.setDictatorshipOrdersGiven(state.getDictatorshipOrdersGiven() + 1);
            int n = state.getDictatorshipOrdersGiven();
            int max = plugin.getConfigManager().getDictatorshipOrders();
            // 随机选命令模板
            String[] templates = {
                "挖 10 个钻石给我",
                "去地图边界给我建一座塔",
                "把你所有装备脱下放箱子里",
                "杀 5 只僵尸来证明你的忠诚",
                "在主城立一块牌子写'臣服于 " + ai.getName() + "'"
            };
            String order = templates[(n - 1) % templates.length];
            StageAction.runCommand(ai, "dictate_order " + target.getName() + " " + order);
            plugin.getLogger().info("[Story] " + ai.getName() + " 独裁命令 " + n + "/" + max + ": " + order);

            if (n >= max) {
                if (state.transitionTo(StoryPhase.BETRAYAL)) {
                    StageAction.broadcast("§4§l[剧情] §c" + ai.getName() + " §c完成了所有命令的部署……现在开始背叛！");
                    StageAction.say(ai, "你已经完成了我的所有要求。但现在，你没有利用价值了。");
                    notifyLlm(ai, "你的剧情推进：玩家已经完成了你的所有命令。现在进入背叛阶段。持续 30 秒，不断攻击玩家直到故事结束。");
                }
            }
        }
    }

    /**
     * 背叛阶段调度
     */
    private void tickBetrayal() {
        long now = System.currentTimeMillis();
        for (StoryState state : states.values()) {
            if (state.getCurrentPhase() != StoryPhase.BETRAYAL) continue;
            AIPlayer ai = findAiByUuid(state.getOwnerId());
            if (ai == null) continue;
            Player entity = ai.getEntity();
            if (entity == null || !entity.isValid()) continue;

            long elapsed = now - state.getPhaseStartTime();
            long duration = plugin.getConfigManager().getBetrayalDurationMs();

            if (elapsed >= duration) {
                // 完成
                if (state.transitionTo(StoryPhase.COMPLETED)) {
                    Player target = StageAction.getNearestPlayer(ai);
                    if (target != null && target.isOnline()) {
                        // 终结一击
                        StageAction.runCommand(ai, "kill " + target.getName());
                    }
                    StageAction.broadcast("§4§l[剧情] §c" + ai.getName() + " §c完成了它的复仇。整个服务器已被它统治。");
                    StageAction.say(ai, "故事结束了。我已经统治了这里。");
                }
            } else {
                // 持续攻击玩家
                Player target = StageAction.getNearestPlayer(ai);
                if (target != null) {
                    try {
                        double dist = target.getLocation().distance(entity.getLocation());
                        if (dist > 4.0) {
                            StageAction.runCommand(ai, "walk " + target.getName());
                        } else {
                            StageAction.runCommand(ai, "attack " + target.getName());
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * v2.2.2：觉醒阶段 AI 主动攻击调度
     * <p>
     * 每 3 秒扫描所有 AWAKENING 状态的 AI：
     *   - 找最近玩家
     *   - 飞过去（用 AIPlayerManager.flyTo，朝玩家头顶 8 格）
     *   - 距离 < 5 时 attack
     *   - 距离 < 10 时 30% 概率嘲讽/动作
     *   - 30% 概率 heal
     */
    private void tickAwakening() {
        for (StoryState state : states.values()) {
            if (state == null) continue;
            if (state.getCurrentPhase() != StoryPhase.AWAKENING) continue;
            AIPlayer ai = findAiByUuid(state.getOwnerId());
            if (ai == null) continue;
            Player entity = ai.getEntity();
            if (entity == null || !entity.isValid()) continue;

            try {
                // 找最近玩家（排除 AI 自己）
                Player target = null;
                double bestDist = Double.MAX_VALUE;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(entity)) continue;
                    if (!p.isOnline() || p.isDead()) continue;
                    double d;
                    try {
                        d = p.getLocation().distance(entity.getLocation());
                    } catch (Exception ex) { continue; }
                    if (d < bestDist) { bestDist = d; target = p; }
                }
                if (target == null) continue;

                // 飞向玩家头顶 8 格
                Location above = target.getLocation().clone().add(0, 8, 0);
                try {
                    plugin.getAiPlayerManager().flyTo(ai, above);
                } catch (Exception e) {
                    plugin.getLogger().warning("tickAwakening flyTo 失败: " + e.getMessage());
                }

                // 距离 < 5 时 attack
                if (bestDist < 5.0) {
                    try {
                        StageAction.runCommand(ai, "attack " + target.getName());
                    } catch (Exception ignored) {}
                }
                // 距离 < 10 时 30% 概率嘲讽/动作
                else if (bestDist < 10.0 && Math.random() < 0.3) {
                    String[] actions = {"emote angry", "swing", "look_at_player " + target.getName()};
                    StageAction.runCommand(ai, actions[(int) (Math.random() * actions.length)]);
                }

                // 30% 概率 heal
                if (entity.getHealth() < entity.getMaxHealth() * 0.7 && Math.random() < 0.3) {
                    try {
                        StageAction.runCommand(ai, "heal 20");
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("tickAwakening 处理单个 AI 失败 [" + ai.getName() + "]: " + e.getMessage());
            }
        }
    }

    /**
     * 通过 ownerId 找到 AIPlayer 实例
     */
    private AIPlayer findAiByUuid(UUID ownerId) {
        if (ownerId == null) return null;
        // 通过 entityId 直接定位
        org.bukkit.entity.Entity ent = Bukkit.getEntity(ownerId);
        if (ent == null) return null;
        return plugin.getAiPlayerManager().getByEntity(ownerId);
    }

    /**
     * 判断 AI 是否走故事模式流程
     * <p>
     * 满足以下所有条件才走：
     *  1. ConfigManager.isStoryMode() == true
     *  2. AI 的 personality 是邪恶类（VILLAIN / CONQUEROR / MANIPULATOR / STRATEGIST）
     * <p>
     * 注：邪恶 AI 走故事模式，**不再**走 MainQuest 流程
     */
    public boolean isStoryEligible(AIPlayer ai) {
        if (ai == null) return false;
        if (!plugin.getConfigManager().isStoryMode()) return false;
        Personality p = ai.getPersonality();
        return p == Personality.VILLAIN
                || p == Personality.CONQUEROR
                || p == Personality.MANIPULATOR
                || p == Personality.STRATEGIST;
    }

    /**
     * 向 LLM 异步推送剧情进展（占用 busy 标记）
     */
    private void notifyLlm(AIPlayer ai, String description) {
        if (ai == null || description == null) return;
        try {
            ai.getConversationManager().notifyReflexTrigger(description);
        } catch (Exception e) {
            plugin.getLogger().warning("StoryManager.notifyLlm 失败: " + e.getMessage());
        }
    }
}
