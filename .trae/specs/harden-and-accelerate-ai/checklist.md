# Checklist

## P0 稳定性修复

### LocationUtil 统一距离工具
- [ ] `src/main/java/com/aip/util/LocationUtil.java` 已创建，含 `safeDistance` 和 `safeDistanceSquared` 方法
- [ ] 跨世界场景返回 `Double.MAX_VALUE` 而非抛 `IllegalArgumentException`
- [ ] `AIPlayerManager.scanEnvironment` 中 2 处 `distance` 已替换
- [ ] `GameDataCollector` 中 5 处 `distance` / `distanceSquared` 已替换
- [ ] `CommandExecutor` 中 `handleMount` / `handleAttack` / `handleApproach` 3 处已替换
- [ ] `NpcAnimator` 中 `approachPlayer` / `lookAtPlayerTemporarily` 2 处已替换
- [ ] 全量 grep `\.distance\(` 与 `\.distanceSquared\(` 在 com/aip 目录下不再有裸调用

### CommandExecutor 修复
- [ ] `executeWithResult` 广播 spokenText 前判 entity 有效性
- [ ] `handleSetHealth` / `handleHeal` 使用真实 maxHealth 作为上界
- [ ] `handleSetHealth` / `handleHeal` 调用前判 `entity.isDead()`
- [ ] `handleFly` 先 `setGameMode(CREATIVE)` 再 `setFlying`，try-catch `IllegalStateException`
- [ ] `handleIgnite` 上限 `Math.min(seconds, 3600)`
- [ ] `handleKill` 后续同回复命令不再执行
- [ ] `handleInteractBlock` 修正 `endsWith("_FENCE_GATE")`
- [ ] `handleCombo` 增加判 `target.isOnline()`
- [ ] `handleStrike` 玩家不在线时返回失败 ExecutionResult

### LLMClient 修复
- [ ] JSON 解析包 try-catch，失败返回 "（AI 暂时无法回复…）"
- [ ] timeout 下限 `Math.max(5, config.getTimeout())`
- [ ] URL 构造 catch `IllegalArgumentException` 包装为 IOException
- [ ] HTTP 4xx/5xx errorStream 为 null 时用 `conn.getResponseMessage()` 拼错误

### 监听器修复
- [ ] `NpcDeathListener` `getKiller()` 在 try-catch 内
- [ ] `NpcDeathListener` 延迟 1 tick 取实体前判 `entity != null`
- [ ] `NpcDamageListener` 反击前判 `attacker.isValid()` 和 `victim.isValid()`
- [ ] `NpcDamageListener` 延迟 1 tick 反应前判 `victim.isValid()`

### 命令与 GUI 修复
- [ ] `AIPCommand.handleSpawn` try-catch 后端不可用异常
- [ ] `AIPCommand.handleRevive` try-catch 后端不可用异常
- [ ] `AIPCommand.onTabComplete` 前置 `args.length == 0` 判断
- [ ] `GuiManager.handleMainMenuClick` 前置 `clicked.hasItemMeta()` 判断
- [ ] `GuiManager` 点击前重新 `aiPlayerManager.get(aiName)` 校验

### 插件生命周期修复
- [ ] `AIPlayerPlugin.onDisable` 调用 `Bukkit.getScheduler().cancelTasks(this)`
- [ ] `AIPlayerPlugin.onEnable` 同步调用 `NpcHelper.recheckBackend()`
- [ ] `GameDataCollector.collect` 入口判 `entity.isDead()`
- [ ] `GameDataCollector.scanNearbyEntities` 排序 Comparator 内 try-catch

## P1 AI 反应加速

### OkHttp + 流式
- [ ] pom.xml 已加 `com.squareup.okhttp3:okhttp:4.12.0` 依赖
- [ ] 共享 `OkHttpClient` 单例已创建（连接池 keep-alive 5 分钟）
- [ ] `LLMClient.chat` 改用 OkHttp
- [ ] `LLMClient.chatStream` 新方法支持流式
- [ ] `ConversationManager.chat` 支持 `streamCallback`，第一个 token 即广播 "AI 正在打字…"
- [ ] config.yml 新增 `provider.stream: true` 默认开启

### TTL 缓存
- [ ] `GameDataCollector` 有 `Map<UUID, CacheEntry>` 缓存字段
- [ ] `collect` 入口先查缓存，2 秒内直接返回
- [ ] entity 死亡或世界切换时清缓存
- [ ] `spawn` / `remove` 时清对应缓存

### busy 排队
- [ ] `AIPlayer` 有 `AtomicBoolean busy` 字段
- [ ] `ConversationManager.chat` 入口 `compareAndSet(false, true)`
- [ ] LLM 完成或异常后 `busy.set(false)`
- [ ] `ChatListener` 给玩家发 "AI 正在思考…" 提示
- [ ] 自主活动与环境感知 busy 时静默跳过

### 命令文档缓存
- [ ] `CommandExecutor` 构造时调用 `getCommandDocs()` 存入 `cachedDocs`
- [ ] `ConversationManager` 读 `getCachedDocs()` 而非每次反射

### 默认配置调整
- [ ] `config.yml` `provider.timeout` 默认 20
- [ ] `config.yml` 新增 `provider.max-tokens: 1024`
- [ ] `config.yml` 新增 `provider.temperature: 0.7`
- [ ] `config.yml` `ai.autonomous-interval` 默认 15
- [ ] `config.yml` 新增 `ai.env-scan-interval: 60`
- [ ] `config.yml` 新增 `ai.env-react-cooldown-ms: 4000`
- [ ] `ConfigManager` 新增对应 getter
- [ ] `AIPlayerManager.startEnvironmentTask` 改读 `ConfigManager.getEnvScanInterval()` 而非硬编码 100L
- [ ] `ENV_REACT_COOLDOWN_MS` 改读 `ConfigManager.getEnvReactCooldownMs()`

## P2 自主活动强化

### 配置一致化
- [ ] `ConfigManager.isAutonomous` 代码兜底改 `true`
- [ ] `AIPlayerPlugin.onEnable` 移除 `if (autonomous)` 条件，无条件启动 autonomousTask
- [ ] `AIPlayerManager.spawnAt` 增加 `bukkitPlayer.setOp(true)`
- [ ] `AIPlayerManager.revive` 增加 `bukkitPlayer.setOp(true)`

### NPC 自动复活
- [ ] `NpcDeathListener.onPlayerDeath` 末尾延迟 100 tick 调 `revive`
- [ ] revive 成功后日志 "AI xxx 已自动复活"
- [ ] config.yml 新增 `ai.auto-revive: true`
- [ ] `ConfigManager.isAutoRevive` getter
- [ ] 复活后保留对话历史/个性/情绪/记忆/目标
- [ ] 关闭 `auto-revive` 时不复活

### 目标驱动持续行动
- [ ] `Goal` 类有 `currentTarget` 字段
- [ ] `GoalManager.getPursuitTask` 返回 BukkitRunnable
- [ ] 追击任务每 60 tick 检查目标距离
- [ ] distance>5 时自动 issue `walk <target>`
- [ ] distance<4 时自动 issue `attack <target>`
- [ ] 不调 LLM
- [ ] `addGoal` 时若 currentTarget 非空自动启动追击任务
- [ ] `completeGoal` 时取消追击任务
- [ ] 目标玩家 60 秒离线自动 completeGoal

## P3 数据主动获取

### 5 个 query 命令实现
- [ ] `handleQueryPlayers` 输出在线玩家列表
- [ ] `handleQueryNearby(radius)` 输出附近实体与方块
- [ ] `handleQueryInventory` 输出背包内容
- [ ] `handleQueryBlock(x, y, z)` 输出指定坐标方块
- [ ] `handleQueryPlayer(name)` 输出指定玩家状态
- [ ] 5 个方法都加 `@AICommand(category = "查询")` 注解
- [ ] dispatchCommand switch 注册 5 个新分支

### query 结果回流
- [ ] `AIPlayer` 新增 `lastQueryResult` 字段
- [ ] 5 个 query 命令执行后存入 `lastQueryResult`
- [ ] `ConversationManager.chat` 注入 "上次查询结果：…"
- [ ] 注入后清除 `lastQueryResult` 避免重复

## P4 OP 命令反馈闭环

### OP 命令失败反馈
- [ ] 所有 OP 命令 handler 在 `isAllowOpCommands=false` 时抛 `RuntimeException("OP 命令已被禁用")`
- [ ] `dispatchCommand` catch 块把异常包装为失败 ExecutionResult
- [ ] 下一轮 prompt 自动带上 "你上一轮 [COMMAND:xxx] 失败：OP 命令已被禁用"

### 可选审批机制
- [ ] `ApprovalManager` 类已创建
- [ ] `ApprovalTask` 含 `AIPlayer / command / createdAt / CompletableFuture<Boolean>`
- [ ] 60 秒超时自动 reject
- [ ] 在线 OP 收到通知
- [ ] `AIPCommand` 新增 `approve` / `reject` 子命令
- [ ] config.yml 新增 `ai.require-approval-for: []` 默认空数组
- [ ] `CommandExecutor` 在执行审批列表内命令前调 `ApprovalManager.request` 挂起等待

## 构建发布

### 文档与配置
- [ ] plugin.yml /aip usage 增加 `/aip approve <id>` 和 `/aip reject <id>` 帮助
- [ ] MODRINTH.md 添加 v1.6.0 更新日志
- [ ] pom.xml version 改 1.6.0
- [ ] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
- [ ] git commit && git push origin main 成功
- [ ] `gh release create v1.6.0` 发布成功
