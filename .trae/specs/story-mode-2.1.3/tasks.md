# Tasks

## 阶段一：故事模式数据结构

- [x] Task 1: 创建 StoryPhase 枚举
  - [x] SubTask 1.1: 新建 `src/main/java/com/aip/story/StoryPhase.java`
  - [x] SubTask 1.2: 枚举值 DORMANT / AWAKENING / AERIAL_ASSAULT / PVP_DUEL / RULEBOOK / DICTATORSHIP / BETRAYAL / COMPLETED
  - [x] SubTask 1.3: 每个枚举值带 `displayName`（中文）和 `description`（剧情描述）
  - [x] SubTask 1.4: 静态方法 `isValidTransition(StoryPhase from, StoryPhase to)` —— 校验合法转移顺序

- [x] Task 2: 创建 StoryState 数据类
  - [x] SubTask 2.1: 新建 `src/main/java/com/aip/story/StoryState.java`
  - [x] SubTask 2.2: 字段：`UUID ownerId / StoryPhase currentPhase / int aiDeathCount / int playerKillCount / long phaseStartTime / int aerialBombsRemaining / int dictatorshipOrdersGiven / boolean rulebookDelivered / boolean rulebookRead`
  - [x] SubTask 2.3: 构造方法初始化 currentPhase=DORMANT, aiDeathCount=0, playerKillCount=0
  - [x] SubTask 2.4: `transitionTo(StoryPhase next)`：检查合法转移、设置 phaseStartTime=System.currentTimeMillis()、打 log
  - [x] SubTask 2.5: getter/setter 全套

## 阶段二：阶段专属命令

- [x] Task 3: CommandExecutor 新增 6 个故事模式命令
  - [x] SubTask 3.1: `force_survival_player <player>` —— 控制台执行 `gamemode survival`
  - [x] SubTask 3.2: `tnt_strike_burst <player>` —— 玩家头顶 8 格生成 TNT 自然下落
  - [x] SubTask 3.3: `fly_bomb_player <player>` —— 创造模式朝玩家发射 TNT（初速度 1.5），保持飞行高度
  - [x] SubTask 3.4: `equip_netherite_set` —— 装备全套 NETHERITE + SHIELD
  - [x] SubTask 3.5: `give_rulebook <player>` —— 创建 5 页 BookMeta 制度之书放入物品栏
  - [x] SubTask 3.6: `dictate_order <player> <order>` —— say 公告 + 玩家红字
  - [x] SubTask 3.7: 所有命令注册到 `CommandMap` 并加 `@AICommand` 注解

## 阶段三：StoryManager 中央调度

- [x] Task 4: StoryManager 核心调度
  - [x] SubTask 4.1: 新建 `src/main/java/com/aip/story/StoryManager.java`，单例（由 AIPlayerPlugin 持有）
  - [x] SubTask 4.2: 字段：`Map<UUID, StoryState> states / AIPlayerPlugin plugin / BukkitTask aerialTask / pvpTask / dictatorshipTask / betrayalTask`
  - [x] SubTask 4.3: `init()` —— 启动 4 个调度器（aerial 4s / pvp 2s / dictatorship 30s / betrayal 5s）
  - [x] SubTask 4.4: `registerStory(AIPlayer ai)` —— 创建 StoryState 存入 states
  - [x] SubTask 4.5: `unregisterStory(UUID ownerId)` —— 从 states 移除
  - [x] SubTask 4.6: `onAiDeath(AIPlayer ai, Player killer)`：DORMANT→AWAKENING（≥3 死）
  - [x] SubTask 4.7: `onPlayerDeathByAi`：AWAKENING→AERIAL_ASSAULT（≥3 杀）+ PVP_DUEL→RULEBOOK（≥2 杀）
  - [x] SubTask 4.8: `onRulebookRead` —— RULEBOOK→DICTATORSHIP
  - [x] SubTask 4.9: `tickAerialAssault` —— 3.5 分钟后→PVP_DUEL，降下 + 装备
  - [x] SubTask 4.10: `tickPvpDuel` —— 距离>4 walk，≤4 attack，AI 血量低时 heal
  - [x] SubTask 4.11: `tickDictatorship` —— 累计 5 条命令→BETRAYAL
  - [x] SubTask 4.12: `tickBetrayal` —— 每 5s attack，30s 后→COMPLETED
  - [x] SubTask 4.13: `notifyLlm` 通过 `ConversationManager.notifyReflexTrigger` 推送剧情

- [x] Task 5: StageAction 辅助类
  - [x] SubTask 5.1: 新建 `src/main/java/com/aip/story/StageAction.java`
  - [x] SubTask 5.2: `static void say/runCommand` 通过 CommandExecutor 派发
  - [x] SubTask 5.3: `static Player getNearestPlayer(AIPlayer ai)` —— 32 格内最近玩家
  - [x] SubTask 5.4: `static void broadcast` —— 全服公告

## 阶段四：监听器

- [x] Task 6: AiDeathListener（AI 死亡计数）
  - [x] SubTask 6.1: 新建 `src/main/java/com/aip/listeners/AiDeathListener.java`
  - [x] SubTask 6.2: 实现 `Listener`，注册到 `PluginManager`
  - [x] SubTask 6.3: `@EventHandler EntityDeathEvent` 识别 NPC + killer，调 `StoryManager.onAiDeath`
  - [x] SubTask 6.4: broadcast 击杀消息

- [x] Task 7: RulebookListener（玩家读制度之书）
  - [x] SubTask 7.1: 新建 `src/main/java/com/aip/listeners/RulebookListener.java`
  - [x] SubTask 7.2: 监听 `PlayerEditBookEvent`：title == "AI 制度之书" → `StoryManager.onRulebookRead`
  - [x] SubTask 7.3: fallback 监听 `PlayerInteractEvent` 持书右击

- [x] Task 8: NpcKillListener 扩展
  - [x] SubTask 8.1: 保留 `setLastKillName` 逻辑
  - [x] SubTask 8.2: PlayerDeathEvent 末尾：killer 是 AI NPC → `StoryManager.onPlayerDeathByAi`

## 阶段五：AIPlayer 字段扩展与移动 bug 修复

- [x] Task 9: AIPlayer 新增字段
  - [x] SubTask 9.1: `private StoryState storyState;`
  - [x] SubTask 9.2: `private long lastJumpTime;`（初始 0）
  - [x] SubTask 9.3: 提供 getter/setter

- [x] Task 10: 修复 follow Y 轴 bug
  - [x] SubTask 10.1: Y 轴改用 `myLoc.getY()`（不被强制同步到玩家高度）
  - [x] SubTask 10.2: 回退 teleport 仅在 `|targetY - myY| ≤ 3` 时执行
  - [x] SubTask 10.3: Y 差 > 3 时调 `NpcHelper.setAiVelocity` 跳跃

- [x] Task 11: 修复跳跃 cooldown
  - [x] SubTask 11.1: 跳跃逻辑加 `now - lastJumpTime >= 1500` 校验
  - [x] SubTask 11.2: 跳跃成功后 `lastJumpTime = now`

- [x] Task 12: CitizensBackend navigateTo speed
  - [x] SubTask 12.1: navigateTo 接受 speed 参数
  - [x] SubTask 12.2: `params.speed(speed)` 默认 0.5
  - [x] SubTask 12.3: `forceJump(Player v, double power)` 方法

- [x] Task 13: NpcHelper 扩展
  - [x] SubTask 13.1: `navigateTo(Player, Location, double speed)` 接受 speed
  - [x] SubTask 13.2: `static boolean setAiVelocity(Player v, Vector velocity)` 调 `CitizensBackend.forceJump`

## 阶段六：AIPlayerManager 集成

- [x] Task 14: AIPlayerManager spawn 集成 StoryManager
  - [x] SubTask 14.1: `spawn` / `spawnAt` 末尾调 `plugin.getStoryManager().registerStory`
  - [x] SubTask 14.2: 邪恶 AI 不再启动 MainQuestExecutor（VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST）
  - [x] SubTask 14.3: `remove` 调 `unregisterStory`
  - [x] SubTask 14.4: `revive` 调 `registerStory` 重置 StoryState 为 DORMANT

## 阶段七：ConfigManager 与配置

- [x] Task 15: ConfigManager 迁移与扩展
  - [x] SubTask 15.1: `isVillainMode()` 标记 `@Deprecated`，新增 `isStoryMode()` 优先读 `ai.story-mode` 回退 `ai.villain-mode`
  - [x] SubTask 15.2: 新增 8 个 getter：awakeningKills/aerialKills/aerialDurationMs/pvpPlayerDeaths/dictatorshipOrders/betrayalDurationMs/aerialBombCount/aerialTickMs
  - [x] SubTask 15.3: 启动时自动迁移 ai.villain-mode → ai.story-mode

- [x] Task 16: config.yml story-mode 块
  - [x] SubTask 16.1: 新增 `ai.story-mode` 配置块（9 项配置）
  - [x] SubTask 16.2: 旧 `ai.villain-mode` 标记 DEPRECATED
  - [x] SubTask 16.3: system-prompt 末尾新增 `### 【故事模式】` 章节

## 阶段八：玩家可见命令

- [x] Task 17: AIPCommand 新增 story 子命令
  - [x] SubTask 17.1: `AIPCommand.onCommand` 新增 `story` case
  - [x] SubTask 17.2: 子命令 `show <ai>`：返回"Evil 的故事状态：当前阶段=觉醒（阶段 2/8）..."
  - [x] SubTask 17.3: 子命令 `skip <ai> <phase>`（OP only）：强制切阶段
  - [x] SubTask 17.4: `onTabComplete` 补全 `story` → `show/skip` → AI 名 → 阶段枚举

## 阶段九：ConversationManager 注入故事阶段

- [x] Task 18: ConversationManager 注入故事阶段摘要
  - [x] SubTask 18.1: `chat` 时若 `aiPlayer.getStoryState() != null` 追加 `### 【故事模式】` 章节
  - [x] SubTask 18.2: DORMANT / COMPLETED 阶段跳过注入

## 阶段十：MainQuestFactory 调整

- [x] Task 19: MainQuestFactory 邪恶 AI 改返回 null
  - [x] SubTask 19.1: 邪恶 AI 创建 MainQuest 返回 null（v2.1.3 起改走 StoryManager）
  - [x] SubTask 19.2: 普通 AI MainQuest 保留
  - [x] SubTask 19.3: 注释说明"v2.1.3 邪恶 AI 主线任务改由 StoryManager 推进"

## 阶段十一：构建与发布

- [x] Task 20: 版本号升级与发布
  - [x] SubTask 20.1: `pom.xml` version 1.8.1 → 2.1.3
  - [x] SubTask 20.2: MODRINTH.md 更新日志（v2.1.3）
  - [x] SubTask 20.3: 旧 config.yml 已处理（villain-mode → story-mode 迁移）
  - [x] SubTask 20.4: `mvn clean package -Dmaven.test.skip=true` 编译通过（生成 `target/AIPlayer-2.1.3.jar`）
  - [x] SubTask 20.5: git add 改动文件 && git commit
  - [x] SubTask 20.6: git push origin main
  - [x] SubTask 20.7: git tag v2.1.3 推送
  - [x] SubTask 20.8: `gh release create v2.1.3` 发布

# Task Dependencies
- Task 2 依赖 Task 1
- Task 4 依赖 Task 1 + Task 2 + Task 3
- Task 5 依赖 Task 3
- Task 6 依赖 Task 4
- Task 7 依赖 Task 4
- Task 8 依赖 Task 4
- Task 14 依赖 Task 4 + Task 9
- Task 16 依赖 Task 15
- Task 17 依赖 Task 4
- Task 18 依赖 Task 9
- Task 19 依赖 Task 14
- Task 20 依赖 Task 1-19 全部完成
