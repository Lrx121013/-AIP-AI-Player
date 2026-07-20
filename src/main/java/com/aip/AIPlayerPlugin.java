package com.aip;

import com.aip.ai.AIPlayerManager;
import com.aip.ai.AllyManager;
import com.aip.ai.CommandDocsProvider;
import com.aip.ai.CommandExecutor;
import com.aip.ApprovalManager;
import com.aip.ai.GameDataCollector;
import com.aip.ai.LLMClient;
import com.aip.ai.NpcAnimator;
import com.aip.ai.NpcHelper;
import com.aip.ai.PlayerProfileManager;
import com.aip.ai.RelationManager;
import com.aip.ai.StrategyEngine;
import com.aip.ai.TaskManager;
import com.aip.ai.TeamManager;
import com.aip.commands.AIPCommand;
import com.aip.commands.GuiCommand;
import com.aip.config.ConfigManager;
import com.aip.gui.GuiManager;
import com.aip.listeners.ChatListener;
import com.aip.listeners.GuiListener;
import com.aip.listeners.NpcDamageListener;
import com.aip.listeners.NpcDeathListener;
import com.aip.listeners.NpcKillListener;
import com.aip.listeners.ReflexListener;
import org.bukkit.Bukkit;
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
    private GuiManager guiManager;

    // ===== v2.2.0：高威胁命令的威胁台词模板 =====
    private CommandDocsProvider commandDocsProvider;

    // ===== 新增管理器（功能 4/5/6） =====
    private TeamManager teamManager;
    private TaskManager taskManager;
    private RelationManager relationManager;

    // ===== P3 新增管理器 =====
    private PlayerProfileManager playerProfileManager;
    private StrategyEngine strategyEngine;

    // ===== P4 新增管理器 =====
    private ApprovalManager approvalManager;

    // ===== v2.1.3 故事模式管理器 =====
    private com.aip.story.StoryManager storyManager;

    // ===== v2.2.0 盟军管理器 =====
    private AllyManager allyManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 加载配置（首次运行会自动生成 config.yml）
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // P4：初始化审批管理器（需在 configManager 之后）
        this.approvalManager = new ApprovalManager(this);

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
        this.gameDataCollector = new GameDataCollector(this);
        this.commandExecutor = new CommandExecutor(this);
        // v2.2.0：注入威胁台词模板
        this.commandDocsProvider = new CommandDocsProvider();
        this.commandExecutor.setCommandDocsProvider(commandDocsProvider);
        this.npcAnimator = new NpcAnimator(this);
        this.aiPlayerManager = new AIPlayerManager(this);
        this.guiManager = new GuiManager(this);

        // 初始化新增管理器
        this.teamManager = new TeamManager();
        this.relationManager = new RelationManager();
        this.taskManager = new TaskManager(this);

        // P3：初始化玩家档案与策略引擎
        this.playerProfileManager = new PlayerProfileManager();
        this.strategyEngine = new StrategyEngine(this);

        // v2.1.3 故事模式（邪恶AI）管理器
        this.storyManager = new com.aip.story.StoryManager(this);
        this.storyManager.init();

        // v2.2.0 盟军管理器
        this.allyManager = new AllyManager(this);

        // v2.2.0 启动自言自语调度器（每 10-20 秒每 AIP 一次 OS）
        if (aiPlayerManager != null) {
            aiPlayerManager.startMonologueTask();
        }

        // v2.2.1 启动虚空保护任务（每 1 秒扫描，y<0 回出生点 + 饱食度 < 6 补充）
        if (aiPlayerManager != null) {
            aiPlayerManager.startVoidGuardTask();
        }

        // 4. 注册命令
        AIPCommand aipCommand = new AIPCommand(this);
        if (getCommand("aip") != null) {
            getCommand("aip").setExecutor(aipCommand);
            getCommand("aip").setTabCompleter(aipCommand);
        } else {
            getLogger().severe("无法注册 /aip 命令，plugin.yml 配置错误！");
        }

        if (getCommand("k") != null) {
            getCommand("k").setExecutor(new GuiCommand(this));
        } else {
            getLogger().severe("无法注册 /k 命令，plugin.yml 配置错误！");
        }

        // 5. 注册事件监听器
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcDamageListener(this), this);
        // 击杀追踪：AI 杀死玩家时设置 lastKillName 让主线任务 KILL_TARGET 可被满足
        getServer().getPluginManager().registerEvents(new NpcKillListener(this), this);
        // 反射规则监听器：监听伤害 / 玩家攻击 / 方块破坏等事件型触发器
        getServer().getPluginManager().registerEvents(new ReflexListener(this), this);
        // v2.1.3 故事模式监听器
        getServer().getPluginManager().registerEvents(new com.aip.listeners.AiDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new com.aip.listeners.RulebookListener(this), this);
        // v2.2.2：故事模式命令拦截器（禁止觉醒后玩家切游戏模式/fly）
        getServer().getPluginManager().registerEvents(new StoryModeCommandInterceptor(this), this);
        if (guiManager != null) {
            getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        } else {
            getLogger().severe("guiManager 未初始化，跳过 GUI 监听器注册！");
        }

        // 6. 启动自动活动任务
        aiPlayerManager.startAutonomousTask();
        // 始终启动环境感知任务（让 NPC 对附近威胁/玩家立刻反应）
        aiPlayerManager.startEnvironmentTask();
        // 启动空闲漫游任务（让 AI 不依赖 LLM 也能到处走动，避免原地不动）
        aiPlayerManager.startIdleWalkTask();
        // 启动自动转头任务（玩家靠近 4 格内时 AI 自动转头看玩家）
        aiPlayerManager.startFacePlayerTask();
        // 启动卡住检查任务（连续 stuck-threshold-ms 未移动则强制 idleWalk）
        aiPlayerManager.startStuckCheckTask();
        // 启动长期任务调度器（功能 5）
        taskManager.start();

        // 立即同步检查一次后端，避免环境任务用错后端
        try {
            NpcHelper.recheckBackend();
        } catch (Throwable ignored) {}
        // 5 秒后再确认一次（Citizens 可能晚于本插件 enable）
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                NpcHelper.recheckBackend();
            } catch (Throwable ignored) {}
        }, 100L);

        getLogger().info("AI Player 插件已启用。使用 /aip spawn <名字> 来生成 AI 玩家。");
    }

    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.stop();
        }
        if (aiPlayerManager != null) {
            aiPlayerManager.removeAll();
        }
        if (allyManager != null) {
            allyManager.removeAll();
        }
        // 取消本插件启动的所有 BukkitRunnable 任务（walk/follow/combo/look_at_player 等）
        Bukkit.getScheduler().cancelTasks(this);
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
            aiPlayerManager.startEnvironmentTask();
            aiPlayerManager.startIdleWalkTask();
            aiPlayerManager.startFacePlayerTask();
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

    public GuiManager getGuiManager() {
        return guiManager;
    }

    /** 队伍管理器（功能 4） */
    public TeamManager getTeamManager() {
        return teamManager;
    }

    /** 长期任务管理器（功能 5） */
    public TaskManager getTaskManager() {
        return taskManager;
    }

    /** 关系图谱管理器（功能 6） */
    public RelationManager getRelationManager() {
        return relationManager;
    }

    /** P3：玩家档案管理器 */
    public PlayerProfileManager getPlayerProfileManager() {
        return playerProfileManager;
    }

    /** P3：策略引擎 */
    public StrategyEngine getStrategyEngine() {
        return strategyEngine;
    }

    /** P4：审批管理器 */
    public ApprovalManager getApprovalManager() {
        return approvalManager;
    }

    /** v2.1.3 故事模式管理器 */
    public com.aip.story.StoryManager getStoryManager() {
        return storyManager;
    }

    /** v2.2.0：威胁台词模板 */
    public CommandDocsProvider getCommandDocsProvider() {
        return commandDocsProvider;
    }

    /** v2.2.0 盟军管理器 */
    public AllyManager getAllyManager() {
        return allyManager;
    }
}
