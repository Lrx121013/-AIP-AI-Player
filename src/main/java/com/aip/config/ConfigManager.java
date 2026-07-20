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
        return plugin.getConfig().getDouble("provider.temperature", 0.7);
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
}
