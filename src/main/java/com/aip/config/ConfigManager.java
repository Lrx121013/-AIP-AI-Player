package com.aip.config;

import com.aip.AIPlayerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 */
public class ConfigManager {

    private final AIPlayerPlugin plugin;

    private String baseUrl;
    private String apiKey;
    private String model;
    private int timeout;
    private Map<String, String> extraHeaders;

    private boolean autonomous;
    private int autonomousInterval;
    private int maxHistory;
    private double moveSpeed;
    private double attackDamage;
    private boolean allowOpCommands;
    private boolean invulnerable;
    private boolean counterattack;
    private int scanRadius;
    private int entityScanRadius;

    // v2.2.0：故事模式战斗增强
    private double temperature;
    // v2.2.1：复读机修复——频率/存在惩罚
    private double frequencyPenalty;
    // v2.2.2：复读机强化——独立的存在惩罚（presence_penalty）
    private double presencePenalty;
    // v2.2.5：故事模式 LLM hook 开关（全部默认 false，故事模式默认走纯预设）
    /** Hook 1: 局势分析 SITUATION */
    private boolean storyLlmSituation;
    /** Hook 2: 嘲讽 TAUNT */
    private boolean storyLlmTaunt;
    /** Hook 3: 对话 DIALOGUE（注：此钩子暂不接） */
    private boolean storyLlmDialogue;
    /** Hook 4: 生成支援 SUMMON_ALLY */
    private boolean storyLlmSummonAlly;
    /** Hook 5: 下达命令 DICTATE_ORDER */
    private boolean storyLlmDictateOrder;
    /** Hook 6: 判断命令执行 CHECK_ORDER */
    private boolean storyLlmCheckOrder;
    /** Hook 7: 杀玩家 KILL_PLAYER */
    private boolean storyLlmKillPlayer;
    private int idleMonologueMinSeconds;
    private int idleMonologueMaxSeconds;
    private boolean enableThreatTaunts;
    private int maxAlliesPerAi;
    private long allySummonCooldownSeconds;

    private String systemPromptTemplate;
    private boolean debug;

    public ConfigManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();

        this.baseUrl = cfg.getString("provider.base-url", "https://api.openai.com/v1");
        this.apiKey = cfg.getString("provider.api-key", "");
        this.model = cfg.getString("provider.model", "gpt-4o-mini");
        this.timeout = cfg.getInt("provider.timeout", 60);
        this.extraHeaders = new HashMap<>();
        if (cfg.isConfigurationSection("provider.extra-headers")) {
            for (String key : cfg.getConfigurationSection("provider.extra-headers").getKeys(false)) {
                extraHeaders.put(key, cfg.getString("provider.extra-headers." + key, ""));
            }
        }

        this.autonomous = cfg.getBoolean("ai.autonomous", true);
        this.autonomousInterval = cfg.getInt("ai.autonomous-interval", 30);
        this.maxHistory = cfg.getInt("ai.max-history", 20);
        this.moveSpeed = cfg.getDouble("ai.move-speed", 0.6);
        this.attackDamage = cfg.getDouble("ai.attack-damage", 5.0);
        this.allowOpCommands = cfg.getBoolean("ai.allow-op-commands", false);
        this.invulnerable = cfg.getBoolean("ai.invulnerable", false);
        this.counterattack = cfg.getBoolean("ai.counterattack", true);
        this.scanRadius = cfg.getInt("ai.scan-radius", 8);
        this.entityScanRadius = cfg.getInt("ai.entity-scan-radius", 16);

        this.systemPromptTemplate = cfg.getString("system-prompt", "");
        this.debug = cfg.getBoolean("debug", false);

        // v2.2.0 故事模式战斗增强
        this.temperature = cfg.getDouble("llm.temperature", 0.9);
        // v2.2.2 复读机强化
        this.frequencyPenalty = cfg.getDouble("llm.frequency-penalty", 0.7);
        this.presencePenalty = cfg.getDouble("llm.presence-penalty", 0.8);
        // v2.2.5：故事模式 LLM hook 开关
        this.storyLlmSituation = cfg.getBoolean("story.llm.situation", false);
        this.storyLlmTaunt = cfg.getBoolean("story.llm.taunt", false);
        this.storyLlmDialogue = cfg.getBoolean("story.llm.dialogue", false);
        this.storyLlmSummonAlly = cfg.getBoolean("story.llm.summon-ally", false);
        this.storyLlmDictateOrder = cfg.getBoolean("story.llm.dictate-order", false);
        this.storyLlmCheckOrder = cfg.getBoolean("story.llm.check-order", false);
        this.storyLlmKillPlayer = cfg.getBoolean("story.llm.kill-player", false);
        this.idleMonologueMinSeconds = cfg.getInt("idle-monologue-min-seconds", 10);
        this.idleMonologueMaxSeconds = cfg.getInt("idle-monologue-max-seconds", 20);
        this.enableThreatTaunts = cfg.getBoolean("enable-threat-taunts", true);
        this.maxAlliesPerAi = cfg.getInt("max-allies-per-ai", 2);
        this.allySummonCooldownSeconds = cfg.getLong("ally-summon-cooldown-seconds", 60L);
    }

    /**
     * 是否已完成模型提供商配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("sk-your-api-key-here")
                && baseUrl != null && !baseUrl.isEmpty()
                && model != null && !model.isEmpty();
    }

    /**
     * 渲染系统提示词（替换 {name} 占位符）
     */
    public String renderSystemPrompt(String aiName) {
        if (systemPromptTemplate == null || systemPromptTemplate.isEmpty()) {
            return "你是一个名为 " + aiName + " 的 Minecraft 玩家。";
        }
        return systemPromptTemplate.replace("{name}", aiName);
    }

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public int getTimeout() { return timeout; }
    public Map<String, String> getExtraHeaders() { return extraHeaders; }
    public boolean isAutonomous() { return autonomous; }
    public int getAutonomousInterval() { return autonomousInterval; }
    public int getMaxHistory() { return maxHistory; }
    public double getMoveSpeed() { return moveSpeed; }
    public double getAttackDamage() { return attackDamage; }
    public boolean isAllowOpCommands() { return allowOpCommands; }
    public boolean isInvulnerable() { return invulnerable; }
    public boolean isCounterattack() { return counterattack; }
    public int getScanRadius() { return scanRadius; }
    public int getEntityScanRadius() { return entityScanRadius; }
    public boolean isDebug() { return debug; }

    /**
     * 是否启用反派模式（实时读取配置，支持 /aip villain 动态切换）
     * @deprecated v2.1.3 起替换为 {@link #isStoryMode()}
     */
    @Deprecated
    public boolean isVillainMode() {
        // v2.1.3 优先读 story-mode，回退到 villain-mode
        if (plugin.getConfig().contains("ai.story-mode")) {
            return plugin.getConfig().getBoolean("ai.story-mode.enabled", false);
        }
        return plugin.getConfig().getBoolean("ai.villain-mode", false);
    }

    /**
     * v2.1.3：是否启用故事模式（邪恶AI 6 阶段叙事）
     * <p>
     * 优先读 ai.story-mode.enabled，回退到 ai.villain-mode
     */
    public boolean isStoryMode() {
        if (plugin.getConfig().contains("ai.story-mode")) {
            return plugin.getConfig().getBoolean("ai.story-mode.enabled", false);
        }
        return plugin.getConfig().getBoolean("ai.villain-mode", false);
    }

    /** 环境感知扫描间隔（ticks），实时读取配置 */
    public int getEnvScanInterval() {
        return plugin.getConfig().getInt("ai.env-scan-interval", 60);
    }

    /** 环境反应冷却（毫秒），实时读取配置 */
    public long getEnvReactCooldownMs() {
        return plugin.getConfig().getLong("ai.env-react-cooldown-ms", 4000L);
    }

    /** 是否自动复活死亡的 AI 玩家，实时读取配置 */
    public boolean isAutoRevive() {
        return plugin.getConfig().getBoolean("ai.auto-revive", true);
    }

    /** 每个 AI 最多反射规则数，实时读取配置 */
    public int getMaxReflexRules() {
        return plugin.getConfig().getInt("ai.max-reflex-rules", 8);
    }

    /** 反射规则冷却下限（毫秒），实时读取配置 */
    public int getReflexMinCooldownMs() {
        return plugin.getConfig().getInt("ai.reflex-min-cooldown-ms", 1000);
    }

    /** 反射规则周期检查间隔（ticks），必须 > 0，实时读取配置 */
    public int getReflexCheckInterval() {
        int v = plugin.getConfig().getInt("ai.reflex-check-interval", 20);
        return v > 0 ? v : 20;
    }

    /** 空闲漫游间隔（秒），实时读取配置 */
    public int getIdleWalkInterval() {
        int v = plugin.getConfig().getInt("ai.idle-walk-interval", 10);
        return v > 0 ? v : 10;
    }

    /** 空闲漫游半径（格），实时读取配置 */
    public int getIdleWalkRadius() {
        int v = plugin.getConfig().getInt("ai.idle-walk-radius", 8);
        return v > 0 ? v : 8;
    }

    public boolean isStream() {
        return plugin.getConfig().getBoolean("provider.stream", true);
    }

    public int getMaxTokens() {
        return plugin.getConfig().getInt("provider.max-tokens", 1024);
    }

    public double getTemperature() {
        return temperature;
    }

    /** v2.2.1：LLM 频率/存在惩罚（-2.0~2.0），抑制复读。默认 0.5 */
    public double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    /** v2.2.2：LLM 存在惩罚（-2.0~2.0），抑制主题重复。默认 0.8 */
    public double getPresencePenalty() {
        return presencePenalty;
    }

    /** v2.2.5：是否启用故事模式局势分析 LLM hook（默认 false） */
    public boolean isStoryLlmSituation() {
        return storyLlmSituation;
    }

    /** v2.2.5：是否启用故事模式嘲讽 LLM hook（默认 false） */
    public boolean isStoryLlmTaunt() {
        return storyLlmTaunt;
    }

    /** v2.2.5：是否启用故事模式对话 LLM hook（默认 false；此钩子暂未接） */
    public boolean isStoryLlmDialogue() {
        return storyLlmDialogue;
    }

    /** v2.2.5：是否启用故事模式生成支援 LLM hook（默认 false） */
    public boolean isStoryLlmSummonAlly() {
        return storyLlmSummonAlly;
    }

    /** v2.2.5：是否启用故事模式下达命令 LLM hook（默认 false） */
    public boolean isStoryLlmDictateOrder() {
        return storyLlmDictateOrder;
    }

    /** v2.2.5：是否启用故事模式判断命令执行 LLM hook（默认 false） */
    public boolean isStoryLlmCheckOrder() {
        return storyLlmCheckOrder;
    }

    /** v2.2.5：是否启用故事模式杀玩家 LLM hook（默认 false） */
    public boolean isStoryLlmKillPlayer() {
        return storyLlmKillPlayer;
    }

    /** 自言自语最小间隔（秒） */
    public int getIdleMonologueMinSeconds() {
        return idleMonologueMinSeconds;
    }

    /** 自言自语最大间隔（秒） */
    public int getIdleMonologueMaxSeconds() {
        return idleMonologueMaxSeconds;
    }

    /** 是否启用威胁嘲讽 */
    public boolean isEnableThreatTaunts() {
        return enableThreatTaunts;
    }

    /** 是否启用主线任务系统，实时读取配置 */
    public boolean isMainQuestEnabled() {
        return plugin.getConfig().getBoolean("ai.main-quest.enabled", true);
    }

    /** 主线任务执行器间隔（ticks），实时读取配置 */
    public int getQuestExecutorInterval() {
        return plugin.getConfig().getInt("ai.main-quest.executor-interval", 120);
    }

    /** 卡住检查间隔（ticks），实时读取配置 */
    public int getStuckCheckInterval() {
        return plugin.getConfig().getInt("ai.main-quest.stuck-check-interval", 80);
    }

    /** 多久不动算卡住（毫秒），实时读取配置 */
    public long getStuckThresholdMs() {
        return plugin.getConfig().getLong("ai.main-quest.stuck-threshold-ms", 12000L);
    }

    /** 被攻击后追击持续时长（毫秒），实时读取配置 */
    public long getPursuitDurationMs() {
        return plugin.getConfig().getLong("ai.main-quest.pursuit-duration-ms", 15000L);
    }

    /** spawn 后多久生成开场白（ticks），实时读取配置 */
    public int getIntroDelayTicks() {
        return plugin.getConfig().getInt("ai.main-quest.intro-delay-ticks", 20);
    }

    // ===== v2.1.3 故事模式（邪恶AI）配置 =====

    /** AI 死亡多少次后觉醒，默认 3 */
    public int getAwakeningKills() {
        return plugin.getConfig().getInt("ai.story-mode.awakening-kills", 3);
    }

    /** 觉醒后 AI 杀玩家多少次进入空中轰炸，默认 3 */
    public int getAerialKills() {
        return plugin.getConfig().getInt("ai.story-mode.aerial-kills", 3);
    }

    /** 空中轰炸持续时长（毫秒），默认 210000（3.5 分钟） */
    public long getAerialDurationMs() {
        return plugin.getConfig().getLong("ai.story-mode.aerial-duration-ms", 210000L);
    }

    /** PVP 阶段 AI 杀玩家多少次进入制度，默认 2 */
    public int getPvpPlayerDeaths() {
        return plugin.getConfig().getInt("ai.story-mode.pvp-player-deaths", 2);
    }

    /** 独裁阶段 AI 下达多少条命令后进入背叛，默认 5 */
    public int getDictatorshipOrders() {
        return plugin.getConfig().getInt("ai.story-mode.dictatorship-orders", 5);
    }

    /** 背叛阶段持续时长（毫秒），默认 30000（30 秒） */
    public long getBetrayalDurationMs() {
        return plugin.getConfig().getLong("ai.story-mode.betrayal-duration-ms", 30000L);
    }

    /** 空中阶段总轰炸次数，默认 12 */
    public int getAerialBombCount() {
        return plugin.getConfig().getInt("ai.story-mode.aerial-bomb-count", 12);
    }

    /** 空中轰炸间隔（毫秒），默认 4000（4 秒） */
    public long getAerialTickMs() {
        return plugin.getConfig().getLong("ai.story-mode.aerial-tick-ms", 4000L);
    }

    // ===== v2.2.0 盟军配置 =====

    /** 每个主 AIP 最多召唤几个盟军，默认 2 */
    public int getMaxAlliesPerAi() {
        return maxAlliesPerAi;
    }

    /** 盟军召唤节流（秒），默认 60 */
    public long getAllySummonCooldownSeconds() {
        return allySummonCooldownSeconds;
    }
}
