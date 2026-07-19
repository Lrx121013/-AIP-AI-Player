# 硬化稳定性 + 加速 AI 反应 + 强化自主性 Spec

## Why

v1.5.0 发布后通过全量代码体检与调用链路分析发现：插件存在大量「玩家正常使用就会报错」的风险点（20+ 处运行时异常），同时 AI 的反应速度受多重瓶颈拖累（最坏端到端 60 秒、环境威胁最坏 13 秒响应），且 AI「想拿数据时拿不到」「玩家不发话就不动」的体验与用户「有自我意识、有计谋、占领服务器」的目标相悖。本次 spec 一次性收敛稳定性、速度、自主性三条主线，让插件真正可在生产服务器长跑。

## What Changes

### P0：稳定性修复（消除运行时报错）
- **统一跨世界距离工具方法** `LocationUtil.safeDistance(a, b)`：返回 `Double.MAX_VALUE` 而非抛 `IllegalArgumentException`，替换所有 `Location.distance / distanceSquared` 裸调用（10+ 处）
- **`CommandExecutor.dispatchCommand` 入口校验**：entity 为空/失效时立即返回失败结果，避免后续命令静默丢弃刷日志
- **`CommandExecutor.executeWithResult` 入口校验**：广播文字前先判 entity 有效性，避免死亡 NPC 仍说话
- **`setHealth/setFoodLevel` 调用前读取真实 maxHealth**：用 `entity.getAttribute(GENERIC_MAX_HEALTH).getValue()` 做上界，NaN/Infinity 直接拒绝
- **`handleFly` 改为先 `setGameMode(CREATIVE)` 再 `setFlying`**：避免生存模式下抛 `IllegalStateException`
- **`handleIgnite` 上限 clamp**：`Math.min(seconds, 3600)` 防止整数溢出
- **`LLMClient.chat` JSON 解析加 try-catch**：解析失败返回 fallback 文本「（AI 暂时无法回复…）」而非冒泡
- **`LLMClient.chat` timeout 下限保护**：`Math.max(5, config.getTimeout())`，禁止 0 表示无限等待
- **`LLMClient.chat` URL 构造加 IllegalArgumentException 捕获**：包装为 IOException
- **`NpcDeathListener` `getKiller()` 移入 try-catch**：避免虚空/`/kill` 场景 NPE
- **`NpcDamageListener` 反击前校验 `attacker.isValid()` / `victim.isValid()`**
- **`AIPCommand.handleSpawn / handleRevive` 包 try-catch**：捕获后端不可用异常，发送友好提示而非 stacktrace
- **`AIPCommand.onTabComplete` 前置 `args.length == 0` 判断**
- **`GuiManager.handleMainMenuClick` 前置 `clicked.hasItemMeta()` 判断**
- **`GuiManager` 点击前重新 `aiPlayerManager.get(aiName)` 校验存在性**：避免选中已删除 AI
- **`AIPlayerPlugin.onDisable` 加 `Bukkit.getScheduler().cancelTasks(this)`**：清理所有 BukkitRunnable
- **`AIPlayerPlugin.onEnable` 末尾同步调用 `NpcHelper.recheckBackend()`**：避免环境任务用错后端
- **`GameDataCollector.collect` 入口判 `entity.isDead()`**：死亡 NPC 返回 `（AI 已死亡）`
- **`GameDataCollector.scanNearbyEntities` 排序 Comparator try-catch**：跨世界异常返回 `Double.MAX_VALUE`
- **`NpcAnimator.approachPlayer / lookAtPlayerTemporarily` 整个 run() 包 try-catch**

### P1：AI 反应加速
- **`LLMClient` 改用 OkHttp + 连接池 + keep-alive**（pom.xml 已有 Gson，加 OkHttp 依赖）：每次节省 100-300ms TLS 握手
- **`LLMClient` 支持流式响应**（OpenAI `stream: true`）：第一个 token 即可广播给玩家，体感延迟从 800ms 降到 200ms
- **`provider.timeout` 默认值改 20 秒**（原 60 秒，玩家会误以为崩服）
- **`provider.max-tokens` 与 `provider.temperature` 提到 config.yml**（原硬编码 1024 / 0.7）
- **`GameDataCollector.collect` 加 TTL 缓存**：2 秒内同一 NPC 多次 collect 返回缓存结果，环境感知 + @提及 + 自主活动三入口共享
- **`CommandExecutor.getCommandDocs()` 启动时缓存一次**（原每次 chat 反射扫描）
- **`AIPlayer` 加 `AtomicBoolean busy`**：LLM 调用期间拒绝新请求，给玩家「AI 正在思考…」提示，避免并发请求互相覆盖 lastCommandResult
- **环境感知间隔可配置**：新增 `ai.env-scan-interval`（默认 60 ticks = 3 秒，原硬编码 100 ticks）
- **环境感知冷却可配置**：新增 `ai.env-react-cooldown-ms`（默认 4000ms，原 8000ms）
- **自主活动间隔默认改为 15 秒**（原 30 秒，配合缓存后无主线程压力）

### P2：自主活动强化（用户不发话也行动）
- **`ConfigManager.isAutonomous` 代码兜底改 `true`**：与 `config.yml` 文件默认值一致（原 false）
- **NPC 死亡后自动复活**：监听 `NpcDeathListener` 死亡事件后延迟 100 tick 调 `AIPlayerManager.revive`，保留对话历史/个性/记忆
- **`AIPlayerManager.spawnAt` 给 NPC 设 OP**（原只有 `spawn` 设了 OP，`spawnAt` 和 `revive` 漏了）
- **`AIPlayerPlugin.onEnable` 移除 `if (autonomous)` 条件**：环境感知与自主活动任务均无条件启动（已由 isConfigured 控制）
- **目标驱动持续行动**：`GoalManager` 新增 `currentTarget` 字段，当 AI 设定「追杀玩家 X」目标时，每 60 tick 自动检查距离并触发 `walk`/`attack`，无需每轮重新调 LLM（节省 token）

### P3：数据主动获取（AI 想要数据就给）
- **新增 query 类命令到 `CommandExecutor`**：
  - `[COMMAND:query_players]` —— 查询所有在线玩家（名字/位置/血量/装备）
  - `[COMMAND:query_nearby <radius>]` —— 重新扫附近实体与方块
  - `[COMMAND:query_inventory]` —— 重新查自己背包
  - `[COMMAND:query_block <x> <y> <z>]` —— 查指定坐标方块
  - `[COMMAND:query_player <name>]` —— 查指定玩家状态
- **query 命令的输出写入 `aiPlayer.lastQueryResult`**：下一轮 prompt 自动带上「上次查询结果：…」（最省改动，延迟一轮但安全）
- **`@AICommand` 注解新增 `category = "查询"`**：自动注入 system prompt，AI 知道有这些查询命令可用

### P4：OP 命令反馈闭环
- **OP 命令被禁用时返回失败 `ExecutionResult`**：原静默 return，现返回 `(cmd, false, "OP 命令已被禁用，请联系管理员开启 allow-op-commands")`，下一轮 prompt 让 AI 知道尝试失败
- **新增审批机制（可选）**：`ai.require-approval-for: [op, ban, kick]`，AI 发这些命令时挂起 → 通知在线 OP → OP 输入 `/aip approve <id>` 才执行（默认空，不开启）

## Impact

- Affected specs:
  - `self-aware-ai-upgrade`（v1.5.0）：P0 修复了 v1.5.0 引入的多处稳定性问题；P1 加速了 v1.5.0 引入的 LLM 调用链路；P2 强化了 v1.5.0 引入的自主活动机制；P3 补全了 v1.5.0 缺失的主动查询能力；P4 闭环了 v1.5.0 引入的 OP 命令反馈
  - `refactor-and-extend`（v1.4.0）：P0 修复了 v1.4.0 引入的 GUI/动画稳定性问题
- Affected code:
  - `src/main/java/com/aip/ai/CommandExecutor.java` —— 入口校验、setHealth 上界、handleFly 修复、handleIgnite clamp、新增 5 个 query 命令、OP 命令失败反馈
  - `src/main/java/com/aip/ai/LLMClient.java` —— OkHttp + 连接池 + 流式 + JSON 解析兜底 + timeout 下限
  - `src/main/java/com/aip/ai/ConversationManager.java` —— busy 排队、注入查询结果
  - `src/main/java/com/aip/ai/AIPlayer.java` —— busy 字段、lastQueryResult 字段
  - `src/main/java/com/aip/ai/AIPlayerManager.java` —— spawnAt/revive 设 OP、env-scan 间隔可配置、目标驱动持续行动
  - `src/main/java/com/aip/ai/GameDataCollector.java` —— TTL 缓存、入口 isDead 判定、Comparator try-catch
  - `src/main/java/com/aip/ai/NpcAnimator.java` —— approachPlayer/lookAtPlayer try-catch
  - `src/main/java/com/aip/ai/GoalManager.java` —— currentTarget + 持续行动
  - `src/main/java/com/aip/listeners/NpcDeathListener.java` —— getKiller 移入 try、自动复活
  - `src/main/java/com/aip/listeners/NpcDamageListener.java` —— 反击前校验
  - `src/main/java/com/aip/commands/AIPCommand.java` —— handleSpawn/handleRevive try-catch、onTabComplete 边界、新增 approve 子命令
  - `src/main/java/com/aip/gui/GuiManager.java` —— hasItemMeta 校验、AI 存在性校验
  - `src/main/java/com/aip/AIPlayerPlugin.java` —— onDisable cancelTasks、onEnable 同步 recheckBackend、移除 autonomous 条件
  - `src/main/java/com/aip/config/ConfigManager.java` —— autonomous 兜底改 true、新增 env-scan-interval / env-react-cooldown-ms / max-tokens / temperature / require-approval-for
  - `src/main/java/com/aip/util/LocationUtil.java` —— **新增** safeDistance 工具方法
  - `src/main/resources/config.yml` —— 新增配置项 + 调整默认值
  - `pom.xml` —— 加 OkHttp 依赖、版本号 1.5.0 → 1.6.0
  - `MODRINTH.md` —— v1.6.0 更新日志

## ADDED Requirements

### Requirement: 统一跨世界距离工具方法
The system SHALL provide `LocationUtil.safeDistance(Location a, Location b)` that returns `Double.MAX_VALUE` instead of throwing `IllegalArgumentException` when locations are in different worlds. All existing `Location.distance` and `distanceSquared` calls in the plugin SHALL be replaced with this utility method.

#### Scenario: Cross-world distance
- **WHEN** an entity is teleported to another world while a scan loop is iterating
- **THEN** `safeDistance` returns `Double.MAX_VALUE` and the entity is sorted to the end of nearby entity list
- **AND** no `IllegalArgumentException` is thrown
- **AND** no log spam occurs

### Requirement: AI 忙碌排队机制
The system SHALL prevent concurrent LLM calls for the same AI player by adding an `AtomicBoolean busy` flag. When the AI is processing a request, new chat triggers SHALL be queued or rejected with a friendly "AI 正在思考…" message to the player.

#### Scenario: Player spams @AI during LLM call
- **WHEN** player sends 3 messages to an AI within 1 second while the AI is still processing the first message
- **THEN** only 1 LLM request is in flight at any time
- **AND** the player receives "AI 正在思考…" feedback for the 2nd and 3rd messages
- **AND** no concurrent overwrites of `lastCommandResult` occur

### Requirement: GameDataCollector TTL 缓存
The system SHALL cache `GameDataCollector.collect` results per NPC for 2 seconds. Subsequent collect calls within the TTL return the cached string. The cache invalidates immediately on entity death or world change.

#### Scenario: Concurrent triggers within TTL
- **WHEN** autonomous task and environment scan both call `collect` for the same NPC within 2 seconds
- **THEN** only 1 actual scan is performed on the main thread
- **AND** both callers receive the same cached result

### Requirement: LLM 流式响应
The system SHALL support OpenAI-compatible streaming responses (`stream: true`). The first token received SHALL be broadcast to nearby players as a partial message, with subsequent tokens appending to the same message. If streaming fails, the system SHALL fall back to non-streaming mode for the same request.

#### Scenario: Streaming success
- **WHEN** AI generates a reply with 50 tokens and LLM API supports streaming
- **THEN** the first token reaches players within 200ms of the request being sent
- **AND** the complete reply is broadcast within 3 seconds
- **AND** `[COMMAND:...]` blocks are only executed after the full reply is received

### Requirement: AI 主动查询命令
The system SHALL provide 5 query commands that AI can issue to fetch game data on demand:
- `query_players` — list all online players with name/location/health/equipment
- `query_nearby <radius>` — rescan nearby entities and blocks within radius
- `query_inventory` — list current inventory contents
- `query_block <x> <y> <z>` — query block type at specific coordinates
- `query_player <name>` — query specific player's state

Query results SHALL be stored in `aiPlayer.lastQueryResult` and injected into the next prompt as "上次查询结果：…" so AI can use the data in its next decision.

#### Scenario: AI queries player list
- **WHEN** AI issues `[COMMAND:query_players]`
- **THEN** all online players' names and locations are captured
- **AND** the result is stored in `aiPlayer.lastQueryResult`
- **AND** the next prompt contains "上次查询结果：在线玩家：Steve(x=100,y=64,z=200,health=20)..."
- **AND** the `@AICommand` annotation makes this command discoverable in the system prompt

### Requirement: NPC 死亡自动复活
The system SHALL automatically revive dead NPCs 5 seconds (100 ticks) after death, preserving conversation history, personality, mood, relationships, goals, and long-term memory. The revived NPC spawns at its death location, or world spawn if death location is invalid.

#### Scenario: NPC killed by monster
- **WHEN** a monster kills an NPC
- **THEN** a DEATH memory record is added to the NPC's long-term memory
- **AND** 100 ticks later `AIPlayerManager.revive` is called
- **AND** the NPC respawns at its death location with full health and food
- **AND** the NPC retains all conversation history and personality
- **AND** autonomous actions resume for the revived NPC

### Requirement: 目标驱动持续行动
The system SHALL allow `GoalManager` to track a `currentTarget` (player name or location). When a goal with a target is active, a background task (60 ticks interval) SHALL automatically issue `walk` or `attack` commands toward the target without requiring a full LLM round-trip. The task cancels when the goal is completed or the target becomes invalid.

#### Scenario: Pursuing a player target
- **WHEN** AI has an active goal "追杀玩家 Steve" with `currentTarget = "Steve"`
- **THEN** every 60 ticks the system checks distance to Steve
- **AND** if distance > 5, the system issues `[COMMAND:walk Steve]`
- **AND** if distance < 4, the system issues `[COMMAND:attack Steve]`
- **AND** no LLM call is made for these actions
- **AND** the goal is marked complete when Steve is dead or offline for 60+ seconds

### Requirement: OP 命令失败反馈
The system SHALL return a failure `ExecutionResult` with reason "OP 命令已被禁用" when AI issues an OP command while `allow-op-commands=false`. The failure SHALL be injected into the next prompt so AI knows to stop trying.

#### Scenario: AI tries to ban a player while OP commands are disabled
- **WHEN** AI issues `[COMMAND:ban Steve]` and `allow-op-commands=false`
- **THEN** `ExecutionResult("ban", false, "OP 命令已被禁用")` is stored in `aiPlayer.lastCommandResult`
- **AND** the next prompt contains "你上一轮 [COMMAND:ban] 失败：OP 命令已被禁用"
- **AND** AI can choose an alternative strategy (e.g., fake_friendly approach)

### Requirement: AI 命令审批机制（可选）
The system SHALL support an optional approval list `ai.require-approval-for`. When AI issues a command whose name is in this list, the command is held pending and a notification is broadcast to online OPs. OPs can approve with `/aip approve <id>` or reject with `/aip reject <id>`. Default value is an empty list (no approval required).

#### Scenario: AI tries to op itself with approval required
- **WHEN** `ai.require-approval-for: [op]` and AI issues `[COMMAND:op Evil]`
- **THEN** a pending approval task is created with a unique ID
- **AND** online OPs receive "AI Evil 试图执行 [op Evil]，输入 /aip approve <id> 同意，/aip reject <id> 拒绝"
- **AND** the command is not executed until approved
- **AND** if no OP approves within 60 seconds, the command is auto-rejected with reason "审批超时"

## MODIFIED Requirements

### Requirement: LLM 调用客户端
The system SHALL use OkHttp with a shared connection pool for all LLM HTTP requests. Connect timeout SHALL be `Math.max(5, config.getTimeout())` seconds. Read timeout SHALL be `config.getTimeout()` seconds. Write timeout SHALL be 30 seconds. JSON parsing SHALL be wrapped in try-catch and return fallback text "（AI 暂时无法回复…）" on failure. The client SHALL support both streaming and non-streaming modes, controlled by `provider.stream: true` config.

### Requirement: AI 自主活动配置
The system SHALL unconditionally start both the autonomous task and the environment scan task on plugin enable (subject to `isConfigured()` check). The `ai.autonomous` config now only controls whether the AI takes autonomous decisions vs. only reacting to environment events; both modes keep the environment scan running. Default value for `ai.autonomous` SHALL be `true` in both `config.yml` and `ConfigManager.java` code fallback. Default `ai.autonomous-interval` SHALL be 15 seconds (was 30). Default `ai.env-scan-interval` SHALL be 60 ticks (3 seconds, was hardcoded 100). Default `ai.env-react-cooldown-ms` SHALL be 4000 (was hardcoded 8000).

### Requirement: NPC OP 权限设置
The system SHALL set `bukkitPlayer.setOp(true)` for all three spawn paths: `spawn`, `spawnAt`, and `revive`. This ensures NPCs can execute OP-restricted commands via `Bukkit.dispatchCommand` regardless of how they were created.

### Requirement: 插件禁用清理
The system SHALL call `Bukkit.getScheduler().cancelTasks(this)` in `onDisable` to clean up all BukkitRunnable tasks created by the plugin, including walk/follow/combo/look_at_player tasks in `NpcAnimator` and `CommandExecutor`. This prevents "Plugin attempted to register task while disabled" errors after `/reload`.

## REMOVED Requirements

### Requirement: 硬编码环境感知间隔与冷却
**Reason**: 原硬编码 `100L, 100L` 和 `ENV_REACT_COOLDOWN_MS = 8000` 不可配置，无法根据服务器性能调整
**Migration**: 改为从 `ConfigManager` 读取 `ai.env-scan-interval`（默认 60 ticks）和 `ai.env-react-cooldown-ms`（默认 4000ms）
