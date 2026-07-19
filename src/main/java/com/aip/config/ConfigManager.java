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

        this.autonomous = cfg.getBoolean("ai.autonomous", false);
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
}
