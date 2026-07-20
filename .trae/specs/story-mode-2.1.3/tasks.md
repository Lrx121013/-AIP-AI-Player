# Tasks

## 阶段一：故事模式数据结构

- [x] Task 1: 创建 StoryPhase 枚举
  - [x] SubTask 1.1: 新建 `src/main/java/com/aip/story/StoryPhase.java`
  - [x] SubTask 1.2: 枚举值 DORMANT / AWAKENING / AERIAL_ASSAULT / PVP_DUEL / RULEBOOK / DICTATORSHIP / BETRAYAL / COMPLETED
  - [x] SubTask 1.3: 每个枚举值带 `displayName`（中文）和 `description`（剧情描述）
  - [x] SubTask 1.4: 静态方法 `isValidTransition(StoryPhase from, StoryPhase to)` —— 校验合法转移顺序（DORMANT→AWAKENING→AERIAL_ASSAULT→PVP_DUEL→RULEBOOK→DICTATORSHIP→BETRAYAL→COMPLETED）

- [x] Task 2: 创建 StoryState 数据类
  - [x] SubTask 2.1: 新建 `src/main/java/com/aip/story/StoryState.java`
  - [x] SubTask 2.2: 字段：`UUID ownerId / StoryPhase currentPhase / int aiDeathCount / int playerKillCount / long phaseStartTime / int aerialBombsRemaining / int dictatorshipOrdersGiven / boolean rulebookDelivered / boolean rulebookRead`
  - [x] SubTask 2.3: 构造方法初始化 currentPhase=DORMANT, aiDeathCount=0, playerKillCount=0
  - [x] SubTask 2.4: `transitionTo(StoryPhase next)`：检查合法转移、设置 phaseStartTime=System.currentTimeMillis()、打 log
  - [x] SubTask 2.5: getter/setter 全套

## 阶段二：阶段专属命令

- [ ] Task 3: CommandExecutor 新增 6 个故事模式命令
  - [ ] SubTask 3.1: `force_survival_player <player>` —— `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamemode survival " + player)`
  - [ ] SubTask 3.2: `tnt_strike_burst <player>` —— 在玩家头顶 8 格生成 TNT 方块，让 Minecraft 物理自然下落
  - [ ] SubTask 3.3: `fly_bomb_player <player>` —— 创造模式下朝玩家位置发射 TNT（带初速度 1.5），同时上调自己 Y 保持飞行高度 ≥ 玩家 + 8
  - [ ] SubTask 3.4: `equip_netherite_set` —— 依次 `equip hand NETHERITE_SWORD` / `equip helmet NETHERITE_HELMET` / `equip chest NETHERITE_CHESTPLATE` / `equip legs NETHERITE_LEGGINGS` / `equip boots NETHERITE_BOOTS` + `give shield`
  - [ ] SubTask 3.5: `give_rulebook <player>` —— 创建 BookMeta（title="AI 制度之书", author="Evil AI", 5 页制度文本），通过 `give <player> written_book` 放入物品栏
  - [ ] SubTask 3.6: `dictate_order <player> <order>` —— `say` 公告"你必须 {order}，否则会有惩罚" + 给玩家发红字消息
  - [ ] SubTask 3.7: 所有命令注册到 `CommandMap` 并加 `@AICommand` 注解

## 阶段三：StoryManager 中央调度

- [ ] Task 4: StoryManager 核心调度
  - [ ] SubTask 4.1: 新建 `src/main/java/com/aip/story/StoryManager.java`，单例（由 AIPlayerPlugin 持有）
  - [ ] SubTask 4.2: 字段：`Map<UUID, StoryState> states / AIPlayerPlugin plugin / BukkitTask aerialTask / BukkitTask pvpTask / BukkitTask dictatorshipTask`
  - [ ] SubTask 4.3: `init()` —— 启动 3 个调度器：
    - aerialTask 每 4000ms（80 tick）执行 `tickAerialAssault`
    - pvpTask 每 2000ms（40 tick）执行 `tickPvpDuel`
    - dictatorshipTask 每 30000ms（600 tick）执行 `tickDictatorship`
  - [ ] SubTask 4.4: `registerStory(AIPlayer ai)` —— 创建 StoryState 存入 states
  - [ ] SubTask 4.5: `unregisterStory(UUID ownerId)` —— 从 states 移除
  - [ ] SubTask 4.6: `onAiDeath(AIPlayer ai, Player killer)`：
    - aiDeathCount++
    - 若 currentPhase == DORMANT && aiDeathCount >= awakening-kills（3）→ transitionTo(AWAKENING)
    - 通过 ConversationManager.notifyReflexTrigger 推送剧情
  - [ ] SubTask 4.7: `onPlayerDeathByAi(AIPlayer ai, Player victim)`：
    - playerKillCount++
    - 若 currentPhase == AWAKENING && playerKillCount >= aerial-kills（3）→ transitionTo(AERIAL_ASSAULT)，执行 `force_survival_player victim` + `fly` + `gamemode creative`
    - 若 currentPhase == PVP_DUEL && playerKillCount >= pvp-player-deaths（2）→ transitionTo(RULEBOOK)，执行 `give_rulebook victim`
  - [ ] SubTask 4.8: `onRulebookRead(AIPlayer ai, Player reader)` —— rulebookRead=true，transitionTo(DICTATORSHIP)
  - [ ] SubTask 4.9: `tickAerialAssault` —— 检查 `now - phaseStartTime >= aerial-duration-ms`（210000）→ transitionTo(PVP_DUEL)，执行 `equip_netherite_set` + `fly off`；否则若 aerialBombsRemaining > 0 → 执行 `fly_bomb_player <最近玩家>`，aerialBombsRemaining--
  - [ ] SubTask 4.10: `tickPvpDuel` —— AI 血量 < 50% → `heal` 自我（每 30 秒一次，AIPlayer 字段 throttle）；距离玩家 > 4 格 → `walk <player>`；≤ 4 格 → `attack <player>`
  - [ ] SubTask 4.11: `tickDictatorship` —— dictatorshipOrdersGiven++，随机选命令模板（"挖 10 个钻石给我" / "去地图边界建塔" / "脱下所有装备"），执行 `dictate_order <player> <命令>`；当 ordersGiven >= 5 → transitionTo(BETRAYAL)
  - [ ] SubTask 4.12: `tickBetrayal` —— 每 5 秒执行 `attack <player>`；持续 30 秒 → transitionTo(COMPLETED)，停止所有 AI 行动

- [ ] Task 5: StageAction 辅助类
  - [ ] SubTask 5.1: 新建 `src/main/java/com/aip/story/StageAction.java`
  - [ ] SubTask 5.2: `static void say(AIPlayer ai, String text)` —— 调 `commandExecutor.execute(ai, "[COMMAND:say " + text + "]")`
  - [ ] SubTask 5.3: `static Player getNearestPlayer(AIPlayer ai)` —— 返回附近 32 格内最近玩家
  - [ ] SubTask 5.4: `static void runCommand(AIPlayer ai, String commandLine)` —— 调 `commandExecutor.execute(ai, "[COMMAND:" + commandLine + "]")`

## 阶段四：监听器

- [ ] Task 6: AiDeathListener（AI 死亡计数）
  - [ ] SubTask 6.1: 新建 `src/main/java/com/aip/listeners/AiDeathListener.java`
  - [ ] SubTask 6.2: 实现 `Listener`，注册到 `PluginManager`
  - [ ] SubTask 6.3: `@EventHandler EntityDeathEvent`：
    - 检查 entity 是 NPC（Citizens API）
    - 找到对应 AIPlayer
    - 找到 killer（lastDamageCause）
    - 若 killer 是 Player，调 `StoryManager.onAiDeath(ai, killer)`
  - [ ] SubTask 6.4: broadcast 击杀消息（可选）："Steve 击杀了 Evil（死亡次数：X）"

- [ ] Task 7: RulebookListener（玩家读制度之书）
  - [ ] SubTask 7.1: 新建 `src/main/java/com/aip/listeners/RulebookListener.java`
  - [ ] SubTask 7.2: 优先监听 `PlayerEditBookEvent`：
    - 检查 book meta.title == "AI 制度之书"
    - 找到 rulebookDelivered 的 AIPlayer（最近一个）
    - 调 `StoryManager.onRulebookRead(ai, player)`
  - [ ] SubTask 7.3: fallback 监听 `PlayerInteractEvent`（手持 written_book 右击空气）：
    - 检查 book title == "AI 制度之书"
    - 调 `StoryManager.onRulebookRead(ai, player)`

- [ ] Task 8: NpcKillListener 扩展
  - [ ] SubTask 8.1: 现有 v1.8.1 NpcKillListener 保留 `setLastKillName` 逻辑
  - [ ] SubTask 8.2: 在原 `PlayerDeathEvent` handler 末尾增加：若 victim 是 Player 且 killer 是 AI NPC，调 `StoryManager.onPlayerDeathByAi(ai, victim)`

## 阶段五：AIPlayer 字段扩展与移动 bug 修复

- [ ] Task 9: AIPlayer 新增字段
  - [ ] SubTask 9.1: 新增 `private StoryState storyState;`
  - [ ] SubTask 9.2: 新增 `private long lastJumpTime;`（初始 0）
  - [ ] SubTask 9.3: 提供 getter/setter

- [ ] Task 10: 修复 follow Y 轴 bug
  - [ ] SubTask 10.1: 改 `src/main/java/com/aip/ai/AIPlayer.java` 第 335 行 `next.setY(targetLoc.getY())` → `next.setY(myLoc.getY())`
  - [ ] SubTask 10.2: 改第 327-336 行回退逻辑：仅当 `Math.abs(targetLoc.getY() - myLoc.getY()) <= 3` 才回退 teleport
  - [ ] SubTask 10.3: Y 差 > 3 时调 `NpcHelper.setAiVelocity(v, new Vector(0, 0.5, 0))`（跳跃尝试）后下一 tick 继续寻路

- [ ] Task 11: 修复跳跃 cooldown
  - [ ] SubTask 11.1: `AIPlayer` 自动跳跃逻辑（若有 `autoJump` 方法）增加 `now - lastJumpTime >= 1500` 校验
  - [ ] SubTask 11.2: 跳跃成功后 `lastJumpTime = System.currentTimeMillis()`

- [ ] Task 12: CitizensBackend navigateTo speed
  - [ ] SubTask 12.1: 改 `src/main/java/com/aip/ai/npc/CitizensBackend.java` `navigateTo` 方法签名增加 `double speed` 参数
  - [ ] SubTask 12.2: `params.speed(speed)` 接受传入，默认 0.5
  - [ ] SubTask 12.3: 新增 `boolean forceJump(Player v, double power)` 方法：`entity.setVelocity(new Vector(0, power, 0))`

- [ ] Task 13: NpcHelper 扩展
  - [ ] SubTask 13.1: 改 `navigateTo(Player, Location, double speed)` 签名，向后端传 speed
  - [ ] SubTask 13.2: 新增 `static boolean setAiVelocity(Player v, Vector velocity)`，调 `CitizensBackend.forceJump`

## 阶段六：AIPlayerManager 集成

- [ ] Task 14: AIPlayerManager spawn 集成 StoryManager
  - [ ] SubTask 14.1: `spawn` / `spawnAt` 末尾调 `plugin.getStoryManager().registerStory(aiPlayer)`
  - [ ] SubTask 14.2: 若 `isStoryMode() == true` 且 aiPlayer.getPersonality() ∈ {VILLAIN, CONQUEROR, MANIPULATOR, STRATEGIST}：
    - `aiPlayer.setMainQuest(null)`
    - 不再启动 `MainQuestExecutor`
  - [ ] SubTask 14.3: `remove` 调 `plugin.getStoryManager().unregisterStory(ownerId)`
  - [ ] SubTask 14.4: `revive` 调 `registerStory`（重置 StoryState 为 DORMANT）

## 阶段七：ConfigManager 与配置

- [ ] Task 15: ConfigManager 迁移与扩展
  - [ ] SubTask 15.1: `isVillainMode()` 标记 `@Deprecated`，新增 `isStoryMode()` 优先读 `ai.story-mode` 回退 `ai.villain-mode`
  - [ ] SubTask 15.2: 新增 getter：`getAwakeningKills / getAerialKills / getAerialDurationMs / getPvpPlayerDeaths / getDictatorshipOrders / getBetrayalDurationMs / getAerialBombCount / getAerialTickMs`
  - [ ] SubTask 15.3: 在 `ConfigManager.init()` 增加自动迁移逻辑：若 `ai.villain-mode` 存在但 `ai.story-mode` 不存在，打 warning 并迁移

- [ ] Task 16: config.yml story-mode 块
  - [ ] SubTask 16.1: 新增 `ai.story-mode` 配置块（enabled / awakening-kills=3 / aerial-kills=3 / aerial-duration-ms=210000 / pvp-player-deaths=2 / dictatorship-orders=5 / betrayal-duration-ms=30000 / aerial-bomb-count=12 / aerial-tick-ms=4000）
  - [ ] SubTask 16.2: 旧 `ai.villain-mode` 标记 DEPRECATED，加注释
  - [ ] SubTask 16.3: system-prompt 末尾新增 `### 【故事模式】` 章节（6 阶段剧情弧线描述）

## 阶段八：玩家可见命令

- [ ] Task 17: AIPCommand 新增 story 子命令
  - [ ] SubTask 17.1: `AIPCommand.onCommand` 新增 `story` case
  - [ ] SubTask 17.2: 子命令 `show <ai>`：返回"Evil 的故事状态：当前阶段=觉醒（阶段 2/7），AI 死亡 3 次，玩家死亡 1 次，阶段开始 18 秒前"
  - [ ] SubTask 17.3: 子命令 `skip <ai> <phase>`（OP only）：强制把 AI 切到指定阶段
  - [ ] SubTask 17.4: `onTabComplete` 补全 `story` → `show/skip` → 在线 AI 名字 → 阶段枚举
  - [ ] SubTask 17.5: `plugin.yml` usage 追加 `/aip story show <name>` 和 `/aip story skip <name> <phase>`

## 阶段九：ConversationManager 注入故事阶段

- [ ] Task 18: ConversationManager 注入故事阶段摘要
  - [ ] SubTask 18.1: `chat` 方法构建 system prompt 时，若 `aiPlayer.getStoryState() != null`：
    - 追加 `### 【故事模式】\n当前阶段：{displayName}（阶段 {n}/{total}）\n剧情：{description}\n下一阶段：{nextDescription}`
  - [ ] SubTask 18.2: 阶段为 DORMANT 或 COMPLETED 时跳过注入

## 阶段十：MainQuestFactory 调整（邪恶 AI 改走 StoryManager）

- [ ] Task 19: MainQuestFactory 邪恶 AI 改返回 null
  - [ ] SubTask 19.1: `MainQuestFactory.create(VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST, ai)` 返回 null（v2.1.3 起邪恶 AI 改走 StoryManager）
  - [ ] SubTask 19.2: 普通 AI（BRAVE/TIMID/GRUMPY/GENTLE）的 MainQuest 保留不变
  - [ ] SubTask 19.3: 注释说明"v2.1.3 邪恶 AI 主线任务改由 StoryManager 推进"

## 阶段十一：构建与发布

- [ ] Task 20: 版本号升级与发布
  - [ ] SubTask 20.1: `pom.xml` version 1.8.1 → 2.1.3
  - [ ] SubTask 20.2: `MODRINTH.md` 添加 v2.1.3 更新日志（6 阶段故事模式 + 移动 bug 修复 + 阶段专属命令）
  - [ ] SubTask 20.3: **必须删除旧 config.yml**（villain-mode → story-mode 迁移）
  - [ ] SubTask 20.4: `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
  - [ ] SubTask 20.5: git add 改动文件 && git commit -m "feat: v2.1.3 故事模式（邪恶AI）完整重写 + 移动优化"
  - [ ] SubTask 20.6: git push origin main
  - [ ] SubTask 20.7: git tag v2.1.3 推送
  - [ ] SubTask 20.8: `gh release create v2.1.3` 发布，上传 target/*.jar

# Task Dependencies
- Task 2 依赖 Task 1（StoryState 引用 StoryPhase）
- Task 4 依赖 Task 1 + Task 2（StoryManager 用 StoryPhase 和 StoryState）
- Task 4 依赖 Task 3（StoryManager 调用阶段专属命令）
- Task 5 依赖 Task 3（StageAction 封装 CommandExecutor）
- Task 6 依赖 Task 4（AiDeathListener 调 StoryManager.onAiDeath）
- Task 7 依赖 Task 4（RulebookListener 调 StoryManager.onRulebookRead）
- Task 8 依赖 Task 4（NpcKillListener 调 StoryManager.onPlayerDeathByAi）
- Task 9 与 Task 1-8 可并行（AIPlayer 字段独立）
- Task 10-13 与 Task 9 可并行（移动优化独立）
- Task 14 依赖 Task 4 + Task 9（spawn 集成）
- Task 15 与 Task 1-14 可并行（ConfigManager 独立）
- Task 16 依赖 Task 15（yml 配置依赖 getter）
- Task 17 依赖 Task 4（story 命令读 StoryState）
- Task 18 依赖 Task 9（ConversationManager 调 getStoryState）
- Task 19 依赖 Task 14（MainQuestFactory 改动配合 spawn 集成）
- Task 20 依赖 Task 1-19 全部完成
