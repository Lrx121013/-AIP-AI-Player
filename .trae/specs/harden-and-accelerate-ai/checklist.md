# Checklist

## P0 稳定性修复

### LocationUtil 统一距离工具
- [x] `src/main/java/com/aip/util/LocationUtil.java` 已创建，含 `safeDistance` 和 `safeDistanceSquared` 方法
- [x] 跨世界场景返回 `Double.MAX_VALUE` 而非抛 `IllegalArgumentException`
- [x] `AIPlayerManager.scanEnvironment` 中 2 处 `distance` 已替换
- [x] `GameDataCollector` 中 5 处 `distance` / `distanceSquared` 已替换（实际 6 处）
- [x] `CommandExecutor` 中 `handleMount` / `handleAttack` / `handleApproach` 3 处已替换
- [x] `NpcAnimator` 中 `approachPlayer` 已替换
- [x] 全量 grep `\.distance\(` 与 `\.distanceSquared\(` 在 com/aip 目录下不再有裸调用（残留均有 try-catch 或 world 判断保护）

### CommandExecutor 修复
- [x] `executeWithResult` 广播 spokenText 前判 entity 有效性
- [x] `handleSetHealth` / `handleHeal` 使用真实 maxHealth 作为上界（通过 resolveAttribute + readMaxHealth）
- [x] `handleSetHealth` / `handleHeal` 调用前判 `entity.isDead()`
- [x] `handleFly` 先 `setGameMode(CREATIVE)` 再 `setFlying`，try-catch `IllegalStateException`
- [x] `handleIgnite` 上限 `Math.min(seconds, 3600)`
- [x] `handleKill` 后续同回复命令不再执行（break）
- [x] `handleCombo` 增加判 `target.isOnline()`
- [x] `handleStrike` 玩家不在线时抛 RuntimeException

### LLMClient 修复
- [x] JSON 解析包 try-catch，失败返回 "（AI 暂时无法回复…）"
- [x] timeout 下限 `Math.max(5, config.getTimeout())`
- [x] URL 构造 catch `IllegalArgumentException` 包装为 IOException
- [x] HTTP 4xx/5xx errorStream 为 null 时用 `response.message()` 拼错误（OkHttp 等价 getResponseMessage）

### 监听器修复
- [x] `NpcDeathListener` `getKiller()` 在 try-catch 内
- [x] `NpcDeathListener` 延迟 1 tick 取实体前判 `entity != null`
- [x] `NpcDamageListener` 反击前判 `attacker.isValid()` 和 `victim.isValid()`
- [x] `NpcDamageListener` 延迟 1 tick 反应前判 `victim.isValid()`

### 命令与 GUI 修复
- [x] `AIPCommand.handleSpawn` try-catch 后端不可用异常
- [x] `AIPCommand.handleRevive` try-catch 后端不可用异常
- [x] `AIPCommand.onTabComplete` 前置 `args.length == 0` 判断
- [x] `GuiManager.handleMainMenuClick` 前置 `clicked.hasItemMeta()` 判断
- [x] `GuiManager` 点击前重新 `aiPlayerManager.get(aiName)` 校验

### 插件生命周期修复
- [x] `AIPlayerPlugin.onDisable` 调用 `Bukkit.getScheduler().cancelTasks(this)`
- [x] `AIPlayerPlugin.onEnable` 同步调用 `NpcHelper.recheckBackend()`
- [x] `GameDataCollector.collect` 入口判 `entity.isDead()`
- [x] `GameDataCollector.scanNearbyEntities` 排序 Comparator 内 try-catch

## P1 AI 反应加速

### OkHttp + 流式
- [x] pom.xml 已加 `com.squareup.okhttp3:okhttp:4.12.0` 依赖
- [x] 共享 `OkHttpClient` 单例已创建（连接池 keep-alive 5 分钟）
- [x] `LLMClient.chat` 改用 OkHttp
- [x] `LLMClient.chatStream` 新方法支持流式
- [x] `ConversationManager.chat` 支持 `streamCallback`，第一个 token 即广播 "AI 正在打字…"
- [x] config.yml 新增 `provider.stream: true` 默认开启

### TTL 缓存
- [x] `GameDataCollector` 有 `Map<UUID, CacheEntry>` 缓存字段
- [x] `collect` 入口先查缓存，2 秒内直接返回
- [x] entity 死亡或世界切换时清缓存（invalidateCache 方法 + isDead 入口校验 + TTL 兜底）
- [x] `spawn` / `remove` 时清对应缓存

### busy 排队
- [x] `AIPlayer` 有 `AtomicBoolean busy` 字段
- [x] `ConversationManager.chat` 入口 `compareAndSet(false, true)`
- [x] LLM 完成或异常后 `busy.set(false)`（finally 块）
- [x] `ChatListener` 给玩家发 "AI 正在思考…" 提示（由 ConversationManager.chat 内部发送）
- [x] 自主活动与环境感知 busy 时静默跳过

### 命令文档缓存
- [x] `CommandExecutor` 构造时调用 `getCommandDocs()` 存入 `cachedDocs`
- [x] `ConversationManager` 读 `getCachedDocs()` 而非每次反射

### 默认配置调整
- [x] `config.yml` `provider.timeout` 默认 20
- [x] `config.yml` 新增 `provider.max-tokens: 1024`
- [x] `config.yml` 新增 `provider.temperature: 0.7`
- [x] `config.yml` `ai.autonomous-interval` 默认 15
- [x] `config.yml` 新增 `ai.env-scan-interval: 60`
- [x] `config.yml` 新增 `ai.env-react-cooldown-ms: 4000`
- [x] `ConfigManager` 新增对应 getter
- [x] `AIPlayerManager.startEnvironmentTask` 改读 `ConfigManager.getEnvScanInterval()` 而非硬编码 100L
- [x] `ENV_REACT_COOLDOWN_MS` 改读 `ConfigManager.getEnvReactCooldownMs()`

## P2 自主活动强化

### 配置一致化
- [x] `ConfigManager.isAutonomous` 代码兜底改 `true`
- [x] `AIPlayerPlugin.onEnable` 移除 `if (autonomous)` 条件，无条件启动 autonomousTask
- [x] `AIPlayerManager.spawnAt` 增加 `bukkitPlayer.setOp(true)`
- [x] `AIPlayerManager.revive` 增加 `bukkitPlayer.setOp(true)`

### NPC 自动复活
- [x] `NpcDeathListener.onPlayerDeath` 末尾延迟 100 tick 调 `revive`
- [x] revive 成功后日志 "AI xxx 已自动复活"
- [x] config.yml 新增 `ai.auto-revive: true`
- [x] `ConfigManager.isAutoRevive` getter
- [x] 复活后保留对话历史/个性/情绪/记忆/目标
- [x] 关闭 `auto-revive` 时不复活

### 目标驱动持续行动
- [x] `Goal` 类有 `currentTarget` 字段
- [x] `GoalManager` 有 pursuitTasks Map 和 startPursuit 方法
- [x] 追击任务每 60 tick 检查目标距离
- [x] distance>5 时自动 issue `walk <target>`
- [x] distance<4 时自动 issue `attack <target>`
- [x] 不调 LLM
- [x] `addGoal` 时若 currentTarget 非空自动启动追击任务
- [x] `completeGoal` 时取消追击任务
- [x] 目标玩家 60 秒离线自动 completeGoal（通过 setProgress(100) 等效）
- [x] `AIPlayerManager.remove` 时调 `cancelAllPursuits`

## P3 数据主动获取

### 5 个 query 命令实现
- [x] `handleQueryPlayers` 输出在线玩家列表
- [x] `handleQueryNearby(radius)` 输出附近实体与方块
- [x] `handleQueryInventory` 输出背包内容
- [x] `handleQueryBlock(x, y, z)` 输出指定坐标方块
- [x] `handleQueryPlayer(name)` 输出指定玩家状态
- [x] 5 个方法都加 `@AICommand(category = "查询")` 注解
- [x] dispatchCommand switch 注册 5 个新分支

### query 结果回流
- [x] `AIPlayer` 新增 `lastQueryResult` 字段
- [x] 5 个 query 命令执行后存入 `lastQueryResult`
- [x] `ConversationManager.chat` 注入 "上次查询结果：…"
- [x] 注入后清除 `lastQueryResult` 避免重复

## P4 OP 命令反馈闭环

### OP 命令失败反馈
- [x] 所有 OP 命令 handler 在 `isAllowOpCommands=false` 时抛 `RuntimeException("OP 命令已被禁用")`（含 handleRespawn 修复）
- [x] `dispatchCommand` catch 块把异常包装为失败 ExecutionResult
- [x] 下一轮 prompt 自动带上 "你上一轮 [COMMAND:xxx] 失败：原因"

### 可选审批机制
- [x] `ApprovalManager` 类已创建
- [x] 内部 `ApprovalTask` 含 `AIPlayer / command / id / approved / reason` 字段
- [x] 60 秒超时自动 reject（1200 tick）
- [x] 在线 OP 收到通知（Bukkit.broadcast 带 "aip.admin" 权限）
- [x] `AIPCommand` 新增 `approve` / `reject` 子命令
- [x] config.yml 新增 `ai.require-approval-for: []` 默认空数组
- [x] `CommandExecutor` 在执行审批列表内命令前调 `ApprovalManager.requestApproval` 挂起等待

## 构建发布

### 文档与配置
- [x] plugin.yml /aip usage 增加 `/aip approve <id>` 和 `/aip reject <id>` 帮助
- [x] MODRINTH.md 添加 v1.6.0 更新日志
- [x] pom.xml version 改 1.6.0
- [x] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS，target/AIPlayer-1.6.0.jar 存在）
- [ ] git commit && git push origin main 成功（commit 9365a84 本地完成，push 待 GitHub 认证）
- [ ] `gh release create v1.6.0` 发布成功（依赖 push）
