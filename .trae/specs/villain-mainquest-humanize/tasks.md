# Tasks

## 阶段一：主线任务数据结构与工厂

- [x] Task 1: 创建 MainQuest 数据类
  - [ ] SubTask 1.1: 新建 `src/main/java/com/aip/ai/MainQuest.java`
  - [ ] SubTask 1.2: 字段：`id / title / List<QuestStage> stages / int currentStageIndex / UUID ownerId / boolean completed`
  - [ ] SubTask 1.3: 内部静态类 `QuestStage`：`description / CompletionCondition completionCondition / List<String> actions / int targetProgress / int currentProgress`
  - [ ] SubTask 1.4: 内部枚举 `CompletionCondition`：REACH_PLAYER / REACH_LOCATION / KILL_TARGET / COLLECT_ITEMS / ELAPSE_TIME / APPROACH_COUNT / NONE
  - [ ] SubTask 1.5: `getCurrentStage()` 返回 stages.get(currentStageIndex)（越界返回 null）
  - [ ] SubTask 1.6: `advanceStage()` —— currentStageIndex++，若越界则 completed=true；返回是否还有下一阶段
  - [ ] SubTask 1.7: `getPromptSummary()` —— 返回 "你的主线任务：{title}（阶段 {n}/{total}：{desc}）\n当前进度：{cur}/{target}\n下一阶段：{nextDesc}"；completed 或 null 时返回空串
  - [ ] SubTask 1.8: `incrementProgress(int delta)` —— currentProgress += delta，clamp 到 targetProgress

- [x] Task 2: 创建 MainQuestFactory 工厂类
  - [ ] SubTask 2.1: 新建 `src/main/java/com/aip/ai/MainQuestFactory.java`
  - [ ] SubTask 2.2: `create(Personality p, AIPlayer ai)` —— 按 personality 返回对应 MainQuest 模板
  - [ ] SubTask 2.3: VILLAIN → "潜入渗透" 3 阶段：
    - 阶段1 "伪装接近玩家"（actions: `approach_nearest_player` / `say hi`，condition=REACH_PLAYER，target=1）
    - 阶段2 "获取信任"（actions: `say friendly` / `gift_item`，condition=APPROACH_COUNT，target=3）
    - 阶段3 "背叛时机"（actions: `attack nearest_player`，condition=KILL_TARGET，target=1）
  - [ ] SubTask 2.4: CONQUEROR → "征服领土" 4 阶段：建立据点/扩张领土/攻击玩家/统治服务器
  - [ ] SubTask 2.5: MANIPULATOR → "欺骗操控" 3 阶段：假装友好/挑拨离间/收服玩家
  - [ ] SubTask 2.6: STRATEGIST → "战略布局" 4 阶段：侦察地形/建立联盟/布局陷阱/总攻
  - [ ] SubTask 2.7: BRAVE / GRUMPY → "自由探索" 2 阶段：探索地形/寻找资源
  - [ ] SubTask 2.8: TIMID → "安全求生" 2 阶段：收集物资/建立避难所
  - [ ] SubTask 2.9: GENTLE → "自由探索" 2 阶段：探索地形/帮助玩家
  - [ ] SubTask 2.10: 默认（兜底）→ null（不绑定主线任务）

- [x] Task 3: 创建 MainQuestExecutor 执行器
  - [ ] SubTask 3.1: 新建 `src/main/java/com/aip/ai/MainQuestExecutor.java`
  - [ ] SubTask 3.2: 字段：`AIPlayerPlugin plugin / AIPlayer owner / BukkitTask task`
  - [ ] SubTask 3.3: `startFor(AIPlayer ai)` —— 若 main-quest.enabled=false 或 owner.mainQuest=null 则直接返回；否则启动 BukkitRunnable 每 `executor-interval`（默认 120）tick 执行 `tick()`
  - [ ] SubTask 3.4: `tick()` 逻辑：
    - 若 owner.mainQuest == null 或 completed → 取消任务 return
    - 若 owner.busy=true 或 NpcHelper.isNavigating=true → 跳过本轮
    - 取 currentStage，遍历 stage.actions 逐条调 `plugin.getCommandExecutor().execute(owner, "[COMMAND:" + action + "]")`
    - 调 `checkCompletion(stage)`，满足则 `mainQuest.advanceStage()`
    - 推进后异步通知 LLM（"你的主线任务 [title] 阶段N已完成，进入阶段N+1：desc"），调用 `owner.getConversationManager().notifyReflexTrigger(...)` 复用异步通知通道
  - [ ] SubTask 3.5: `checkCompletion(QuestStage stage)` 按枚举分支：
    - REACH_PLAYER：附近 3 格内有玩家 → 满足
    - APPROACH_COUNT：累计接近玩家数 ≥ target → 满足
    - KILL_TARGET：owner.lastKillName 非空且与目标匹配 → 满足（简化：阶段期间杀死任意玩家即满足）
    - ELAPSE_TIME：阶段开始后经过 target*10 秒 → 满足
    - COLLECT_ITEMS：owner.getInventory 含 target 个任意 item → 满足
    - REACH_LOCATION：暂返回 false（无目标地点需求时不用）
    - NONE：永远满足（用于最后一阶段兜底，由 completed 处理）
  - [ ] SubTask 3.6: `cancel()` —— 取消 task 并置 null

## 阶段二：AIPlayer 字段扩展与去重

- [x] Task 4: AIPlayer 新增字段与去重逻辑
  - [ ] SubTask 4.1: 新增字段 `private MainQuest mainQuest;`（可空）
  - [ ] SubTask 4.2: 新增字段 `private final Map<String, Long> recentMessages = new LinkedHashMap<>();`（消息去重，最多 20 条）
  - [ ] SubTask 4.3: 新增字段 `private long lastMoveTime;` 和 `private Location lastMoveLoc;`
  - [ ] SubTask 4.4: 新增字段 `private BukkitTask pursuitTask;`（被攻击后追击任务引用，可取消重置）
  - [ ] SubTask 4.5: 新增字段 `private String lastKillName;`（主线任务 KILL_TARGET 判定用）
  - [ ] SubTask 4.6: 新增字段 `private long stageStartTime;`（当前阶段开始时间，ELAPSE_TIME 用）
  - [ ] SubTask 4.7: 提供 getter/setter
  - [ ] SubTask 4.8: 修改 `sayInChat(String message)` 返回 boolean：
    - 规范化 key：`message.trim().toLowerCase()`
    - 查 recentMessages，若 key 存在且 `now - ts < 30000` → 打 fine 日志 "拒绝重复消息：{message}"，return false
    - 否则广播、存入 recentMessages、超出 20 条移除最旧、return true

## 阶段三：AIPlayerManager 集成

- [x] Task 5: AIPlayerManager spawn 阶段集成主线任务
  - [ ] SubTask 5.1: 新增字段 `private BukkitTask stuckCheckTask;`
  - [ ] SubTask 5.2: `spawn` / `spawnAt` 末尾调 `bindMainQuest(aiPlayer)`：
    - 若 villainMode 或 main-quest.enabled：`aiPlayer.setMainQuest(MainQuestFactory.create(aiPlayer.getPersonality(), aiPlayer))`
    - 若 mainQuest 非空：`aiPlayer.setStageStartTime(System.currentTimeMillis())`、`new MainQuestExecutor(plugin, aiPlayer).startFor(aiPlayer)`
  - [ ] SubTask 5.3: `spawn` / `spawnAt` 调度 20 tick 后异步生成开场白：
    - Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> generateIntroLine(aiPlayer), intro-delay-ticks)
    - generateIntroLine：调 LLM（非流式），prompt 含 personality + spawn 位置 + "一句话，不超过 30 字，符合个性"，过滤 [COMMAND:...] 后回主线程 `aiPlayer.sayInChat(text)`，存入 history 作为 assistant 第一条
  - [ ] SubTask 5.4: `remove` 取消 mainQuest 执行器与追击任务（若 mainQuest 非空调 `mainQuestExecutor.cancel()`，pursuitTask 非空调 cancel）
  - [ ] SubTask 5.5: `revive` 时重新绑定 mainQuest（与 spawn 一致）

- [x] Task 6: AIPlayerManager 卡住检查任务
  - [ ] SubTask 6.1: 新增 `startStuckCheckTask()`：每 `stuck-check-interval`（默认 80）tick 执行
  - [ ] SubTask 6.2: `stuckCheck(aiPlayer)` 逻辑：
    - 若 busy=true 或 isNavigating=true 或 following != null → 更新 lastMoveTime/lastMoveLoc 为当前，return
    - 计算 `now - lastMoveTime`，若 ≥ `stuck-threshold-ms`（12000）且位置变化 < 1 格 → 调 `idleWalk(aiPlayer)`
    - 更新 lastMoveTime/lastMoveLoc
  - [ ] SubTask 6.3: `stopAutonomousTask` 取消 stuckCheckTask
  - [ ] SubTask 6.4: AIPlayerPlugin.onEnable 启动时调用 `startStuckCheckTask()`

## 阶段四：被攻击追击

- [x] Task 7: NpcDamageListener 增加追击逻辑
  - [ ] SubTask 7.1: 读取 `NpcDamageListener.java` 现有结构，定位玩家攻击分支
  - [ ] SubTask 7.2: 在玩家攻击 AI 分支末尾，启动 15 秒追击任务：
    - 若 `aiPlayer.getPursuitTask() != null` 先 cancel 旧任务（重置计时）
    - 创建 BukkitRunnable runTaskTimer(plugin, 0L, 20L)，记录 startTime
    - 每 tick：若 `now - startTime >= pursuit-duration-ms`（15000）→ cancel 自身 return
    - 计算 AI 与 attacker 距离：> 4 格调 `commandExecutor.execute(aiPlayer, "[COMMAND:walk " + attackerName + "]")`，≤ 4 格调 `[COMMAND:attack " + attackerName + "]`
    - attacker 离线 → cancel 自身
  - [ ] SubTask 7.3: 任务引用存入 `aiPlayer.setPursuitTask(task)`
  - [ ] SubTask 7.4: AIPlayerManager.remove 时取消 pursuitTask（已由 Task 5.4 处理）

## 阶段五：Prompt 注入与配置

- [x] Task 8: ConversationManager 注入主线任务摘要
  - [ ] SubTask 8.1: `chat` 方法构建 system prompt 时，在 teamInfo 之后追加：
    ```java
    String questSummary = aiPlayer.getMainQuest() != null ? aiPlayer.getMainQuest().getPromptSummary() : "";
    if (!questSummary.isEmpty()) {
        systemPromptBuilder.append("\n").append(questSummary);
    }
    ```
  - [ ] SubTask 8.2: 摘要为空时跳过（mainQuest null 或 completed）

- [x] Task 9: ConfigManager main-quest 配置
  - [ ] SubTask 9.1: `config.yml` 新增 `ai.main-quest` 块（enabled / executor-interval / stuck-check-interval / stuck-threshold-ms / pursuit-duration-ms / intro-delay-ticks）
  - [ ] SubTask 9.2: ConfigManager 新增 getter：`isMainQuestEnabled() / getQuestExecutorInterval() / getStuckCheckInterval() / getStuckThresholdMs() / getPursuitDurationMs() / getIntroDelayTicks()`
  - [ ] SubTask 9.3: MainQuestExecutor / AIPlayerManager 从 ConfigManager 读取间隔，不再硬编码

## 阶段六：玩家可见命令

- [x] Task 10: AIPCommand 新增 quest show 子命令
  - [ ] SubTask 10.1: `AIPCommand.onCommand` 新增 `quest` case，仅支持 `show <ai>` 子命令
  - [ ] SubTask 10.2: 输出格式：
    - 有主线任务进行中：`{ai} 的主线任务：{title}（阶段 {n}/{total}：{desc}，进度 {cur}/{target}，进行中）`
    - 无主线任务：`{ai} 当前没有进行中的主线任务`
  - [ ] SubTask 10.3: `onTabComplete` 补全 `quest` → `show` → 在线 AI 名字
  - [ ] SubTask 10.4: `plugin.yml` usage 追加 `/aip quest show <ai>`

## 阶段七：system-prompt 主线任务章节

- [x] Task 11: config.yml system-prompt 新增主线任务章节
  - [ ] SubTask 11.1: 在 system-prompt 末尾新增 `### 【主线任务】` 章节
  - [ ] SubTask 11.2: 章节内容说明"你有长线目标，每轮决策考虑主线进度"，给出 VILLAIN / CONQUEROR / MANIPULATOR / STRATEGIST 四种邪恶 AI 的剧情梗概
  - [ ] SubTask 11.3: 强调"主线任务阶段已由系统自动推进动作，你只需在对话中体现角色感，不要重复输出 [COMMAND:...]"

## 阶段八：构建与发布

- [ ] Task 12: 版本号升级与发布
  - [ ] SubTask 12.1: pom.xml version 1.7.4 → 1.8.0
  - [ ] SubTask 12.2: MODRINTH.md 添加 v1.8.0 更新日志（主线任务系统、6 项拟人化修复、/aip quest show 命令）
  - [ ] SubTask 12.3: **必须删除旧 config.yml**（system-prompt 大改），下次启动自动重建
  - [ ] SubTask 12.4: `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
  - [ ] SubTask 12.5: git add 改动文件 && git commit -m "feat: v1.8.0 邪恶 AIP 主线任务系统 + 拟人化修复"
  - [ ] SubTask 12.6: git push origin main
  - [ ] SubTask 12.7: git tag v1.8.0 推送
  - [ ] SubTask 12.8: `gh release create v1.8.0` 发布，上传 target/*.jar

# Task Dependencies

- Task 2 依赖 Task 1（Factory 用 MainQuest / QuestStage）
- Task 3 依赖 Task 1 + Task 2（Executor 用 MainQuest 和 Factory 模板）
- Task 4 依赖 Task 1（mainQuest 字段类型）
- Task 5 依赖 Task 3 + Task 4（spawn 启动 Executor、设置 mainQuest 字段）
- Task 6 依赖 Task 4（lastMoveTime/lastMoveLoc 字段）
- Task 7 依赖 Task 4（pursuitTask 字段）
- Task 8 依赖 Task 4（getMainQuest）
- Task 9 与 Task 3-8 可并行（ConfigManager 改动独立）
- Task 10 依赖 Task 4（getMainQuest）
- Task 11 与 Task 1-10 可并行（config.yml 文本改动）
- Task 12 依赖 Task 1-11 全部完成
