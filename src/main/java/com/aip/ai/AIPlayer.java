package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 玩家实例：用真正的玩家实体（NPC）作为物理表现，并维护对话历史与 AI 状态
 */
public class AIPlayer {

    private final AIPlayerPlugin plugin;
    private final String name;
    private UUID entityId;
    private final List<Map<String, String>> conversationHistory;
    private final List<String> pendingActions;
    private boolean activated;
    private Location lastLocation;
    private double health;
    private int foodLevel;
    private String following; // 正在跟随的玩家名

    // ===== 新增字段（功能 1/3/7/8/9/10） =====
    /** 统计数据（功能 1） */
    private final AIStats stats = new AIStats();
    /** 个性（功能 3） */
    private Personality personality = Personality.GENTLE;
    /** 反派模式开启前的原人格，关闭后恢复（P2） */
    private Personality originalPersonality = null;
    /** 情绪值 0-100，默认 50（功能 9） */
    private int mood = 50;
    /** 死亡位置，用于复活（功能 7） */
    private Location deathLocation;
    /** 日程列表（功能 8） */
    private final List<Schedule> schedules = new ArrayList<>();
    /** 死亡日志（功能 10） */
    private final List<DeathRecord> deathLog = new ArrayList<>();
    /** 上一轮命令执行结果，用于回流给 LLM（P1：执行结果回流） */
    private ExecutionResult lastCommandResult;
    /** P3：上次 query 命令的查询结果，下一轮 prompt 注入后清除 */
    private String lastQueryResult;
    /** P2：长期目标管理器 */
    private GoalManager goalManager;
    /** 反射规则管理器（条件反射式自动动作） */
    private ReflexManager reflexManager;
    /** P3：长期记忆系统 */
    private final LongTermMemory memory = new LongTermMemory();
    /** P1：AI 正在思考的忙标记（防重入），原子操作 */
    private final AtomicBoolean busy = new AtomicBoolean(false);
    /** 主线任务（可为 null，未启用或无匹配 personality 时） */
    private MainQuest mainQuest;
    /** 最近发送的消息记录：消息内容（小写、去空格） → 时间戳，用于 30 秒去重 */
    private final Map<String, Long> recentMessages = new LinkedHashMap<>();
    /** 上次移动时间戳（ms），卡住检测用 */
    private long lastMoveTime;
    /** 上次移动位置，卡住检测用 */
    private Location lastMoveLoc;
    /** 被攻击后启动的追击任务（可取消重置） */
    private BukkitTask pursuitTask;
    /** 最近杀死的玩家名（主线任务 KILL_TARGET 判定用） */
    private String lastKillName;
    /** 当前主线任务阶段开始时间（ms），ELAPSE_TIME 完成条件用 */
    private long stageStartTime;
    /** 对话管理器（构造时初始化，提供给 MainQuestExecutor 等模块复用） */
    private ConversationManager conversationManager;
    /** 主线任务执行器（spawn/revive 时由 AIPlayerManager 创建并设置，remove 时 cancel） */
    private MainQuestExecutor mainQuestExecutor;
    /** v2.1.3 故事模式状态（可为 null，未启用或非邪恶 AI 时） */
    private com.aip.story.StoryState storyState;
    /** 上次跳跃时间戳（ms），用于跳跃 cooldown 防止蹦蹦跳跳 */
    private long lastJumpTime;

    public AIPlayer(AIPlayerPlugin plugin, String name, UUID entityId) {
        this.plugin = plugin;
        this.name = name;
        this.entityId = entityId;
        this.conversationHistory = new CopyOnWriteArrayList<>();
        this.pendingActions = new ArrayList<>();
        this.activated = false;
        this.health = 20.0;
        this.foodLevel = 20;
        this.goalManager = new GoalManager(plugin, this);
        this.reflexManager = new ReflexManager(plugin, this);
        // 初始化对话管理器，供 MainQuestExecutor 等模块复用
        this.conversationManager = new ConversationManager(plugin, this);
    }

    public String getName() { return name; }
    public UUID getEntityId() { return entityId; }
    /** 复活时更新实体 UUID（功能 7） */
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public List<Map<String, String>> getConversationHistory() { return conversationHistory; }

    // ===== 新增 getter / setter =====

    /** 统计数据（功能 1） */
    public AIStats getStats() { return stats; }

    /** 个性（功能 3） */
    public Personality getPersonality() { return personality; }
    public void setPersonality(Personality personality) {
        if (personality != null) this.personality = personality;
    }

    /** 反派模式开启前的原人格（P2），可能为 null */
    public Personality getOriginalPersonality() { return originalPersonality; }
    public void setOriginalPersonality(Personality originalPersonality) {
        this.originalPersonality = originalPersonality;
    }

    /** 情绪（功能 9）：0-100，自动 clamp */
    public int getMood() { return mood; }
    public void setMood(int mood) {
        this.mood = Math.max(0, Math.min(100, mood));
    }
    public void adjustMood(int delta) {
        setMood(this.mood + delta);
    }

    /** 死亡位置 / 复活用（功能 7） */
    public Location getDeathLocation() { return deathLocation; }
    public void setDeathLocation(Location deathLocation) { this.deathLocation = deathLocation; }

    /** 日程列表（功能 8） */
    public List<Schedule> getSchedules() { return schedules; }

    /** 死亡日志（功能 10） */
    public List<DeathRecord> getDeathLog() { return deathLog; }

    /** 上一轮命令执行结果（P1：执行结果回流，可能为 null） */
    public ExecutionResult getLastCommandResult() { return lastCommandResult; }
    public void setLastCommandResult(ExecutionResult lastCommandResult) {
        this.lastCommandResult = lastCommandResult;
    }

    /** P3：上次 query 命令的查询结果（可能为 null） */
    public String getLastQueryResult() { return lastQueryResult; }
    public void setLastQueryResult(String lastQueryResult) {
        this.lastQueryResult = lastQueryResult;
    }

    /** P2：长期目标管理器 */
    public GoalManager getGoalManager() { return goalManager; }

    /** 反射规则管理器 */
    public ReflexManager getReflexManager() { return reflexManager; }

    /** P3：长期记忆系统 */
    public LongTermMemory getMemory() { return memory; }

    /** P1：忙标记（用于 chat 防重入） */
    public AtomicBoolean getBusy() { return busy; }

    /** 主线任务（可能为 null） */
    public MainQuest getMainQuest() { return mainQuest; }
    public void setMainQuest(MainQuest mainQuest) { this.mainQuest = mainQuest; }

    /** 最近发送消息记录（用于去重检查） */
    public Map<String, Long> getRecentMessages() { return recentMessages; }

    /** 上次移动时间戳 */
    public long getLastMoveTime() { return lastMoveTime; }
    public void setLastMoveTime(long lastMoveTime) { this.lastMoveTime = lastMoveTime; }

    /** 上次移动位置 */
    public Location getLastMoveLoc() { return lastMoveLoc; }
    public void setLastMoveLoc(Location lastMoveLoc) { this.lastMoveLoc = lastMoveLoc; }

    /** 被攻击后追击任务 */
    public BukkitTask getPursuitTask() { return pursuitTask; }
    public void setPursuitTask(BukkitTask pursuitTask) { this.pursuitTask = pursuitTask; }

    /** 最近杀死的玩家名 */
    public String getLastKillName() { return lastKillName; }
    public void setLastKillName(String lastKillName) { this.lastKillName = lastKillName; }

    /** 当前阶段开始时间 */
    public long getStageStartTime() { return stageStartTime; }
    public void setStageStartTime(long stageStartTime) { this.stageStartTime = stageStartTime; }

    /** 对话管理器（用于反射规则通知 / 主线任务阶段完成通知） */
    public ConversationManager getConversationManager() { return conversationManager; }
    public void setConversationManager(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    /** 主线任务执行器 */
    public MainQuestExecutor getMainQuestExecutor() { return mainQuestExecutor; }
    public void setMainQuestExecutor(MainQuestExecutor mainQuestExecutor) {
        this.mainQuestExecutor = mainQuestExecutor;
    }

    /** v2.1.3 故事模式状态 */
    public com.aip.story.StoryState getStoryState() { return storyState; }
    public void setStoryState(com.aip.story.StoryState storyState) { this.storyState = storyState; }

    /** 上次跳跃时间戳（ms） */
    public long getLastJumpTime() { return lastJumpTime; }
    public void setLastJumpTime(long lastJumpTime) { this.lastJumpTime = lastJumpTime; }

    /** 获取 AIPlayerPlugin 引用（供 StageAction 等辅助类使用） */
    public AIPlayerPlugin getPlugin() { return plugin; }

    public Player getEntity() {
        org.bukkit.entity.Entity ent = Bukkit.getEntity(entityId);
        return ent instanceof Player ? (Player) ent : null;
    }

    public Location getLocation() {
        Player p = getEntity();
        if (p != null) {
            lastLocation = p.getLocation();
            return lastLocation;
        }
        return lastLocation;
    }

    public World getWorld() {
        Location loc = getLocation();
        return loc != null ? loc.getWorld() : null;
    }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }

    public double getHealth() {
        Player p = getEntity();
        if (p != null) return p.getHealth();
        return health;
    }

    public int getFoodLevel() { return foodLevel; }

    public void setFollowing(String playerName) { this.following = playerName; }
    public String getFollowing() { return following; }

    /**
     * 添加一条对话历史（保留最大数量限制）
     */
    public void addHistory(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        conversationHistory.add(msg);
        ConfigManager cfg = plugin.getConfigManager();
        int max = cfg.getMaxHistory();
        while (conversationHistory.size() > max) {
            conversationHistory.remove(0);
        }
    }

    /**
     * 重置对话历史
     */
    public void resetHistory() {
        conversationHistory.clear();
    }

    /**
     * v2.2.1：返回对话历史最后 n 个 assistant 消息的 content，用于 prompt 注入"不要重复"。
     * @param n 最多返回几条（<= 0 返回空 list）
     * @return 消息 content 列表（按时间正序）
     */
    public java.util.List<String> getRecentMessages(int n) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (n <= 0) return result;
        java.util.List<java.util.Map<String, String>> hist = getConversationHistory();
        if (hist == null || hist.isEmpty()) return result;
        // 从尾部往回取最多 n 条 assistant
        int count = 0;
        for (int i = hist.size() - 1; i >= 0 && count < n; i--) {
            java.util.Map<String, String> m = hist.get(i);
            if (m == null) continue;
            String role = m.get("role");
            String content = m.get("content");
            if ("assistant".equals(role) && content != null && !content.isEmpty()) {
                result.add(0, content);  // 倒序插回正序
                count++;
            }
        }
        return result;
    }

    /**
     * 把消息以聊天框形式广播（只显示文字，不含命令）。
     * <p>
     * 30 秒内重复的消息（忽略大小写、首尾空格）会被拒绝广播，避免 AI 重复刷屏。
     *
     * @param message 要广播的消息
     * @return true=已广播；false=被去重拒绝
     */
    public boolean sayInChat(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String trimmed = message.trim();
        String key = trimmed.toLowerCase();
        long now = System.currentTimeMillis();
        // 检查 30 秒内是否已发送过相同消息
        Long lastTs = recentMessages.get(key);
        if (lastTs != null && now - lastTs < 30000L) {
            plugin.getLogger().fine("拒绝重复消息：" + trimmed);
            return false;
        }
        // 广播并记录
        String formatted = "<" + name + "> " + trimmed;
        Bukkit.broadcastMessage(formatted);
        recentMessages.put(key, now);
        // 超过 20 条移除最旧
        while (recentMessages.size() > 20) {
            String oldest = recentMessages.keySet().iterator().next();
            recentMessages.remove(oldest);
        }
        return true;
    }

    /**
     * 跟随任务
     */
    public void startFollowTask() {
        if (following == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (following == null) {
                    cancel();
                    return;
                }
                Player v = getEntity();
                if (v == null || !v.isValid()) {
                    cancel();
                    return;
                }
                org.bukkit.entity.Player target = Bukkit.getPlayerExact(following);
                if (target == null || !target.isOnline()) {
                    following = null;
                    cancel();
                    return;
                }
                Location targetLoc = target.getLocation();
                Location myLoc = v.getLocation();
                double dist;
                try {
                    dist = myLoc.distance(targetLoc);
                } catch (Exception e) {
                    return;
                }
                // 距离 > 3 才需要追
                if (dist <= 3) {
                    // 已靠近，取消寻路
                    NpcHelper.cancelNavigation(v);
                    return;
                }
                // 优先用后端寻路
                final AIPlayer self = AIPlayer.this;
                boolean navigated = NpcHelper.navigateTo(v, targetLoc,
                        plugin.getConfigManager().getMoveSpeed());
                if (!navigated) {
                    // 回退：分帧 teleport
                    // v2.1.3 修复：仅在 Y 差 ≤ 3 时回退 teleport，且 Y 保持 myLoc.getY()
                    // 防止"追随玩家时直接传送到玩家高度"的 bug
                    double yDiff = Math.abs(targetLoc.getY() - myLoc.getY());
                    if (yDiff <= 3.0) {
                        Location dir = targetLoc.clone().subtract(myLoc).toVector().normalize()
                                .multiply(plugin.getConfigManager().getMoveSpeed())
                                .toLocation(myLoc.getWorld());
                        Location next = myLoc.clone().add(dir);
                        // v2.1.3 关键修复：Y 轴用 myLoc.getY() 而非 targetLoc.getY()
                        next.setY(myLoc.getY());
                        v.teleport(next);
                    } else {
                        // Y 差太大，跳着追（向上跳一格），下一 tick 继续寻路
                        long now = System.currentTimeMillis();
                        if (now - self.getLastJumpTime() >= 1500L) {
                            NpcHelper.setAiVelocity(v, new org.bukkit.util.Vector(0, 0.4, 0));
                            self.setLastJumpTime(now);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /**
     * v2.2.0：根据当前装备自动调整实体的 GENERIC_ATTACK_DAMAGE / GENERIC_ARMOR / ARMOR_TOUGHNESS。
     * 主手剑按 Material 查伤害表（木 4 / 石 5 / 铁 6 / 钻 7 / 下界合金 8 / 金 4），加 SHARPNESS 附魔加成（每级 +0.5×level+0.5）。
     * 4 件护甲按 Material 查护甲值（皮 1 / 锁 2 / 铁 2 / 钻 3 / 下界合金 3 / 金 1），加 PROTECTION 附魔加成（每级 +1）。
     * 主手空时攻击伤害重置为 1.0。
     */
    public void applyEquipmentAttributes() {
        Player ent = getEntity();
        if (ent == null || !ent.isValid()) return;
        try {
            // ===== 攻击伤害 =====
            double attackDamage = 1.0;
            ItemStack main = ent.getInventory().getItemInMainHand();
            if (main != null && !main.getType().isAir()) {
                Material mat = main.getType();
                attackDamage = switch (mat) {
                    case WOODEN_SWORD -> 4.0;
                    case STONE_SWORD -> 5.0;
                    case IRON_SWORD -> 6.0;
                    case DIAMOND_SWORD -> 7.0;
                    case NETHERITE_SWORD -> 8.0;
                    case GOLDEN_SWORD -> 4.0;
                    default -> 1.0;
                };
                // SHARPNESS 附魔加成
                try {
                    int sharp = main.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.SHARPNESS);
                    if (sharp > 0) attackDamage += 0.5 * sharp + 0.5;
                } catch (Throwable ignored) {}
            }
            // 设置 AttackDamage
            try {
                org.bukkit.attribute.Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("generic.attack_damage"));
                if (attr != null) {
                    var inst = ent.getAttribute(attr);
                    if (inst != null) inst.setBaseValue(attackDamage);
                }
            } catch (Throwable ignored) {}

            // ===== 护甲 =====
            double armor = 0;
            double toughness = 0;
            ItemStack helmet = ent.getInventory().getHelmet();
            ItemStack chest = ent.getInventory().getChestplate();
            ItemStack legs = ent.getInventory().getLeggings();
            ItemStack boots = ent.getInventory().getBoots();
            ItemStack[] armors = {helmet, chest, legs, boots};
            for (ItemStack armorItem : armors) {
                if (armorItem == null || armorItem.getType().isAir()) continue;
                Material m = armorItem.getType();
                double a = switch (m) {
                    case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS,
                         LEATHER_HORSE_ARMOR -> 1;
                    case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS -> 2;
                    case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> 2;
                    case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 3;
                    case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 3;
                    case GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS -> 1;
                    default -> 0;
                };
                armor += a;
                // 韧性：下界合金 3.0，其它 0
                if (m.name().contains("NETHERITE")) toughness += 3.0;
                // PROTECTION 附魔
                try {
                    int prot = armorItem.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.PROTECTION);
                    if (prot > 0) armor += prot;
                } catch (Throwable ignored) {}
            }
            // 设置 Armor
            try {
                org.bukkit.attribute.Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("generic.armor"));
                if (attr != null) {
                    var inst = ent.getAttribute(attr);
                    if (inst != null) inst.setBaseValue(armor);
                }
            } catch (Throwable ignored) {}
            // 设置 ArmorToughness
            try {
                org.bukkit.attribute.Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("generic.armor_toughness"));
                if (attr != null) {
                    var inst = ent.getAttribute(attr);
                    if (inst != null) inst.setBaseValue(toughness);
                }
            } catch (Throwable ignored) {}
        } catch (Exception e) {
            plugin.getLogger().warning("applyEquipmentAttributes 失败: " + e.getMessage());
        }
    }
}
