# 代码休整 + GUI 修复 + 10 新功能 Spec

## Why

用户反馈 v1.3.3 "GUI 打不开"，并且代码中存在大量缺陷（NPE 风险、命令未注册、权限处理错误、代码重复等）。需要在修复 GUI 的基础上休整所有代码，并新增 10 个实用功能以提升插件价值。

## What Changes

### GUI 修复（P0）
- **BREAKING** 在 `plugin.yml` 正式注册 `/k` 命令，废弃 `PlayerCommandPreprocessEvent` 黑魔法
- 修复 `GuiListener` 权限检查失败时未取消事件导致 "Unknown command" 提示
- 修复 `GuiManager.openPlayerList` 中 `entity.getHealth()` / `isInvisible()` 在实体失效时抛 `IllegalStateException`
- 将 `onInventoryClick` 的标题匹配 `title.startsWith("§6")` 改为 `InventoryHolder` 标识机制
- 修正"按 K 键"误导文案为"输入 /k"

### 代码休整
- 用 `ChatColor.stripColor()` 替换链式 `replace("§e","")` 调用
- 抽取 `fillEmpty(inv, from, to)` 公共方法消除三处重复代码
- 清理 `GuiListener` 中未使用的 import（`PlayerInteractEvent`、`PlayerToggleSneakEvent`）
- 修复 `"AI" + System.currentTimeMillis()` 调用两次导致名字不一致
- 在 `onInventoryClick` 方法入口立即 `setCancelled(true)`，防止 shift-click 背包物品
- 将 `executeCommand` 加 try-catch，命令失败时给玩家反馈
- 从 Inventory 标题反解析 `aiName` 改为 `Map<UUID, AIPlayer>` 上下文

### 新增 10 个功能
1. **AI 统计面板**（GUI 扩展）：展示对话次数/行走距离/击杀数/在线时长
2. **AI 对话历史查看**：`/aip history <ai> [page]` 分页查看对话记录
3. **AI 个性设置**：`/aip personality set <ai> <trait>` 配置勇敢/胆小/暴躁/温和
4. **AI 队伍系统**：`/aip team create/join/leave/disband` 多 AI 协同
5. **AI 长期任务指派**：`/aip task assign <ai> <type>` 持续执行建造/采集/巡逻
6. **AI 关系图谱**：`/aip relation set <ai1> <ai2> <friend|enemy|neutral>`
7. **AI 复活**：`/aip revive <ai>` 死亡后保留记忆复活
8. **AI 日程作息**：`/aip schedule <ai> <time-range> <action>` 昼夜节律
9. **AI 情绪系统**：`/aip mood <ai>` 查看情绪值，影响 Prompt
10. **AI 死亡日志**：`/aip deathlog <ai>` 记录死亡时间/原因/凶手

## Impact

- **Affected specs**: 无（首个 spec）
- **Affected code**:
  - `/workspace/src/main/resources/plugin.yml` —— 注册 `/k` 命令
  - `/workspace/src/main/java/com/aip/AIPlayerPlugin.java` —— 注册 `/k` 的 CommandExecutor
  - `/workspace/src/main/java/com/aip/listeners/GuiListener.java` —— 重写为命令执行器
  - `/workspace/src/main/java/com/aip/gui/GuiManager.java` —— 修复 NPE + 重构
  - `/workspace/src/main/java/com/aip/commands/AIPCommand.java` —— 新增 10 个子命令
  - `/workspace/src/main/java/com/aip/ai/AIPlayer.java` —— 新增个性/情绪/统计字段
  - 新增 `/workspace/src/main/java/com/aip/ai/AIStats.java` —— 统计数据
  - 新增 `/workspace/src/main/java/com/aip/ai/Personality.java` —— 个性枚举
  - 新增 `/workspace/src/main/java/com/aip/ai/TeamManager.java` —— 队伍管理
  - 新增 `/workspace/src/main/java/com/aip/ai/TaskManager.java` —— 长期任务
  - 新增 `/workspace/src/main/java/com/aip/ai/RelationManager.java` —— 关系图谱

## ADDED Requirements

### Requirement: GUI 可通过 /k 命令正常打开
The system SHALL register `/k` as a formal command in `plugin.yml` and bind it to a `CommandExecutor` that opens the AI player list GUI when executed by a player with `aip.admin` permission.

#### Scenario: OP 玩家打开 GUI
- **WHEN** OP 玩家执行 `/k`
- **THEN** AI 玩家列表 GUI 立即打开，显示所有 AI 玩家状态

#### Scenario: 普通玩家无权限
- **WHEN** 无 `aip.admin` 权限的玩家执行 `/k`
- **THEN** 提示"你没有权限"，且不再触发 "Unknown command" 提示

### Requirement: GUI 不会因实体失效而崩溃
The system SHALL check `entity.isValid()` before accessing `getHealth()` or `isInvisible()`, and skip invalid AI players in the list.

#### Scenario: AI 实体已卸载
- **WHEN** AI 玩家实体已卸载但仍在 aiPlayers Map 中
- **THEN** GUI 列表跳过该 AI，不抛异常

### Requirement: GUI 点击事件用 InventoryHolder 标识
The system SHALL use a custom `InventoryHolder` to identify AIPlayer GUIs instead of matching inventory title prefix.

#### Scenario: 点击其他插件 GUI
- **WHEN** 玩家点击非 AIPlayer 的 GUI
- **THEN** 事件不被 AIPlayer 处理

### Requirement: AI 统计面板
The system SHALL track per-AI stats: chat count, walk distance, kill count, online time, command success rate, and display them in a GUI stats page.

#### Scenario: 查看统计
- **WHEN** 玩家在 GUI 中点击"统计"按钮
- **THEN** 显示该 AI 的所有统计数据

### Requirement: AI 对话历史查看
The system SHALL provide `/aip history <ai> [page]` to paginate AI conversation history, 10 entries per page.

#### Scenario: 查看第一页
- **WHEN** 玩家执行 `/aip history Lrx`
- **THEN** 显示最近 10 条对话记录

### Requirement: AI 个性设置
The system SHALL allow setting AI personality via `/aip personality set <ai> <trait>` where trait ∈ {brave, timid, grumpy, gentle}, persisted to config and injected into LLM system prompt.

#### Scenario: 设置勇敢个性
- **WHEN** 玩家执行 `/aip personality set Lrx brave`
- **THEN** Lrx 的 system prompt 加入"你性格勇敢，面对危险不退缩"

### Requirement: AI 队伍系统
The system SHALL support `/aip team create <name>`, `/aip team join <team> <ai>`, `/aip team leave <ai>`, `/aip team disband <team>`, where teammates don't attack each other.

#### Scenario: 创建队伍
- **WHEN** 玩家执行 `/aip team create guards`
- **THEN** 创建名为 guards 的空队伍

### Requirement: AI 长期任务指派
The system SHALL support `/aip task assign <ai> <type>` where type ∈ {build, gather, patrol, escort, farm}, AI periodically executes task actions until canceled.

#### Scenario: 指派采集任务
- **WHEN** 玩家执行 `/aip task assign Lrx gather`
- **THEN** Lrx 每 60 秒自动寻找附近资源并采集

### Requirement: AI 关系图谱
The system SHALL maintain pairwise relationship values (-100 to +100) between AIs, affecting attack/follow behavior.

#### Scenario: 设置敌对
- **WHEN** 玩家执行 `/aip relation set Lrx Bob enemy`
- **THEN** Lrx 和 Bob 互相视为敌人，相遇时主动攻击

### Requirement: AI 复活
The system SHALL support `/aip revive <ai>` to respawn a dead AI at its last location or spawn point, preserving conversation history and personality.

#### Scenario: 复活死亡 AI
- **WHEN** 玩家执行 `/aip revive Lrx`
- **THEN** Lrx 在死亡位置重生，保留所有记忆

### Requirement: AI 日程作息
The system SHALL support `/aip schedule <ai> <time-range> <action>` to set time-based behavior, e.g. `/aip schedule Lrx 6:00-18:00 gather`.

#### Scenario: 设置白天采集
- **WHEN** 玩家执行 `/aip schedule Lrx 6:00-18:00 gather`
- **THEN** Lrx 在游戏时间 6:00-18:00 期间自动采集

### Requirement: AI 情绪系统
The system SHALL maintain a mood value (0-100) per AI, affected by events (attack decreases, chat increases), injected into LLM prompt.

#### Scenario: 查看情绪
- **WHEN** 玩家执行 `/aip mood Lrx`
- **THEN** 显示 Lrx 当前情绪值和状态（开心/平静/沮丧）

### Requirement: AI 死亡日志
The system SHALL record AI death events (time, cause, killer) and support `/aip deathlog <ai>` to view history.

#### Scenario: 查看死亡日志
- **WHEN** 玩家执行 `/aip deathlog Lrx`
- **THEN** 显示 Lrx 所有死亡记录

## MODIFIED Requirements

### Requirement: AI 玩家管理
原有 `/aip` 命令新增以下子命令：`history`, `personality`, `team`, `task`, `relation`, `revive`, `schedule`, `mood`, `deathlog`。

### Requirement: GUI 交互
GUI 主界面新增"统计"按钮；动作菜单新增"个性/情绪/任务"查看入口。

## REMOVED Requirements

### Requirement: PlayerCommandPreprocessEvent 触发 GUI
**Reason**: 改用正式注册的 `/k` 命令，更规范且避免与其他插件命令冲突。
**Migration**: `GuiListener.onKeyPress` 重构为 `GuiCommand` 实现 `CommandExecutor`。
