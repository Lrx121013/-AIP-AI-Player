package com.aip;

import com.aip.ai.AIPlayerManager;
import com.aip.ai.CommandExecutor;
import com.aip.ai.GameDataCollector;
import com.aip.ai.LLMClient;
import com.aip.ai.NpcAnimator;
import com.aip.commands.AIPCommand;
import com.aip.config.ConfigManager;
import com.aip.listeners.ChatListener;
import com.aip.listeners.NpcDeathListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AI Player 主插件类
 */
public class AIPlayerPlugin extends JavaPlugin {

    private static AIPlayerPlugin instance;

    private ConfigManager configManager;
    private LLMClient llmClient;
    private AIPlayerManager aiPlayerManager;
    private GameDataCollector gameDataCollector;
    private CommandExecutor commandExecutor;
    private NpcAnimator npcAnimator;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 加载配置（首次运行会自动生成 config.yml）
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // 2. 检查是否已配置 API Key（未配置则给出提示）
        if (!configManager.isConfigured()) {
            getLogger().warning("============================================");
            getLogger().warning(" AI Player 尚未完成模型提供商配置！");
            getLogger().warning(" 请打开 plugins/AIPlayer/config.yml 填写：");
            getLogger().warning("   - provider.base-url");
            getLogger().warning("   - provider.api-key");
            getLogger().warning("   - provider.model");
            getLogger().warning(" 然后重启服务器以启用 AI 功能。");
            getLogger().warning("============================================");
        }

        // 3. 初始化核心组件
        this.llmClient = new LLMClient(configManager);
        this.gameDataCollector = new GameDataCollector(configManager);
        this.commandExecutor = new CommandExecutor(this);
        this.npcAnimator = new NpcAnimator(this);
        this.aiPlayerManager = new AIPlayerManager(this);

        // 4. 注册命令
        AIPCommand aipCommand = new AIPCommand(this);
        if (getCommand("aip") != null) {
            getCommand("aip").setExecutor(aipCommand);
            getCommand("aip").setTabCompleter(aipCommand);
        } else {
            getLogger().severe("无法注册 /aip 命令，plugin.yml 配置错误！");
        }

        // 5. 注册事件监听器
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcDeathListener(this), this);

        // 6. 启动自动活动任务
        if (configManager.isAutonomous()) {
            aiPlayerManager.startAutonomousTask();
        }

        getLogger().info("AI Player 插件已启用。使用 /aip spawn <名字> 来生成 AI 玩家。");
    }

    @Override
    public void onDisable() {
        if (aiPlayerManager != null) {
            aiPlayerManager.removeAll();
        }
        getLogger().info("AI Player 插件已禁用。");
    }

    public void reloadAll() {
        reloadConfig();
        configManager.load();
        if (aiPlayerManager != null) {
            aiPlayerManager.stopAutonomousTask();
            if (configManager.isAutonomous()) {
                aiPlayerManager.startAutonomousTask();
            }
        }
    }

    public static AIPlayerPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LLMClient getLlmClient() {
        return llmClient;
    }

    public AIPlayerManager getAiPlayerManager() {
        return aiPlayerManager;
    }

    public GameDataCollector getGameDataCollector() {
        return gameDataCollector;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public NpcAnimator getNpcAnimator() {
        return npcAnimator;
    }
}
