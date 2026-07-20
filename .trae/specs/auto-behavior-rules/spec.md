# Auto Behavior Rules Spec

## Why

当前 AI 的所有反应都依赖 LLM 思考流程（即使有 `GoalManager.startPursuit` 也只覆盖"追击目标"一种场景）。对于"玩家走进就攻击""血量低就逃跑""着火就灭火"这类**即时本能反应**，LLM 思考太慢（数百 ms 到数秒）、消耗 token、且不稳定。需要一个**规则引擎层**，让 AI 基于预定义或运行时定义的规则即时反应，**完全跳过 LLM**。

## What Changes

- 新增 `AutoBehaviorManager` 类：每个 `AIPlayer` 独立持有一个实例，周期性扫描游戏状态并触发匹配的规则
- 内置 7 条规则（覆盖战斗/生存/状态恢复三类场景）
- 规则触发时直接调 `CommandExecutor.execute(aiPlayer, "[COMMAND:xxx]")`，**不调 LLM**
- 每条规则独立冷却（默认 3 秒），避免刷屏
- 新增 AI 命令 `auto_behavior add/remove/list`：**AI 可通过 LLM 对话自主定义规则**
- 新增 `/aip behavior list/toggle/reset <ai> [rule]` 管理子命令
- `config.yml` 新增 `ai.auto-behavior` 配置段，可调整每条规则的启用状态与参数
- `AIPlayerManager` 启动全局扫描任务（默认每 20 tick = 1 秒一次）

## Impact

- **Affected code**:
  - 新增 `src/main/java/com/aip/ai/AutoBehaviorManager.java`（核心引擎）
  - 新增 `src/main/java/com/aip/ai/AutoBehaviorRule.java`（规则定义/枚举）
  - 修改 [AIPlayer.java](file:///workspace/src/main/java/com/aip/ai/AIPlayer.java)：新增 `autoBehaviorManager` 字段
  - 修改 [AIPlayerManager.java](file:///workspace/src/main/java/com/aip/ai/AIPlayerManager.java)：启动扫描任务、`remove` 时取消
  - 修改 [CommandExecutor.java](file:///workspace/src/main/java/com/aip/ai/CommandExecutor.java)：新增 `handleAutoBehavior` 分支
  - 修改 [AIPCommand.java](file:///workspace/src/main/java/com/aip/commands/AIPCommand.java)：新增 `behavior` 子命令
  - 修改 [config.yml](file:///workspace/src/main/resources/config.yml)：新增 `ai.auto-behavior` 配置段
  - 修改 [ConfigManager.java](file:///workspace/src/main/java/com/aip/config/ConfigManager.java)：新增 getter
  - 修改 [plugin.yml](file:///workspace/src/main/resources/plugin.yml)：usage 增加 `/aip behavior`

## ADDED Requirements

### Requirement: 自动行为规则引擎

The system SHALL provide an `AutoBehaviorManager` that scans each AI player's game state every N ticks (default 20 = 1 second) and triggers matching rules **without invoking the LLM**. Triggered rules directly call `CommandExecutor.execute` with a `[COMMAND:xxx]` string.

#### Scenario: 玩家靠近自动攻击

- **GIVEN** AI 启用 `player_approach_attack` 规则，参数 `distance=5`
- **WHEN** 任意玩家进入 AI 5 格范围内（同世界）
- **THEN** AI 立即执行 `[COMMAND:attack <玩家名>]`，不调 LLM
- **AND** 该规则进入 3 秒冷却，冷却期内不再触发

#### Scenario: 怪物靠近自动攻击

- **GIVEN** AI 启用 `monster_near_attack` 规则，参数 `distance=8`
- **WHEN** 任意 `Monster` 类型实体进入 AI 8 格内
- **THEN** AI 立即执行 `[COMMAND:attack nearest]`

#### Scenario: 低血量自动逃跑

- **GIVEN** AI 启用 `low_health_flee` 规则，参数 `threshold-percent=30`
- **WHEN** AI 当前血量低于 maxHealth 的 30%
- **THEN** AI 执行 `[COMMAND:walk_dir <远离最近威胁的方向> 10]`
- **AND** 冷却延长到 5 秒（避免逃跑抖动）

#### Scenario: 低血量自动吃东西

- **GIVEN** AI 启用 `low_health_eat` 规则，参数 `threshold-percent=50`
- **WHEN** AI 血量低于 maxHealth 的 50%
- **THEN** AI 执行 `[COMMAND:eat]`

#### Scenario: 低饱食度自动吃东西

- **GIVEN** AI 启用 `low_hunger_eat` 规则，参数 `threshold=20`
- **WHEN** AI 饱食度低于 20
- **THEN** AI 执行 `[COMMAND:eat]`

#### Scenario: 着火自动灭火

- **GIVEN** AI 启用 `on_fire_extinguish` 规则
- **WHEN** AI `getFireTicks() > 0`
- **THEN** AI 执行 `[COMMAND:extinguish]`

#### Scenario: 夜晚自动睡觉

- **GIVEN** AI 启用 `day_sleep` 规则
- **WHEN** 世界时间在 13000-23000 之间（夜晚）且 AI 未在睡觉
- **THEN** AI 执行 `[COMMAND:sleep]`

#### Scenario: 实体死亡或离线时跳过扫描

- **GIVEN** AI 实体已死亡、已失效或 `busy=true`
- **WHEN** 扫描周期到达
- **THEN** 跳过本次扫描，不触发任何规则

#### Scenario: AI 正在思考时跳过自动行为

- **GIVEN** AI 的 `busy` 标记为 true（LLM 调用进行中）
- **WHEN** 扫描周期到达
- **THEN** 跳过本次扫描，避免与 LLM 决策冲突

### Requirement: AI 自主管理规则

The system SHALL allow AI to dynamically add/remove/list its own auto-behavior rules via `[COMMAND:auto_behavior add|remove|list]`, so AI can define its own logic during conversation without OP intervention.

#### Scenario: AI 添加规则

- **WHEN** AI 在 LLM 回复中发出 `[COMMAND:auto_behavior add player_approach_attack 5]`
- **THEN** 该 AI 启用 `player_approach_attack` 规则，参数 distance=5
- **AND** 下一轮 prompt 注入 "你已启用自动行为规则：player_approach_attack（distance=5）"

#### Scenario: AI 移除规则

- **WHEN** AI 发出 `[COMMAND:auto_behavior remove player_approach_attack]`
- **THEN** 该 AI 的 `player_approach_attack` 规则被禁用
- **AND** 下一轮 prompt 注入 "你已禁用自动行为规则：player_approach_attack"

#### Scenario: AI 查询规则

- **WHEN** AI 发出 `[COMMAND:auto_behavior list]`
- **THEN** 结果存入 `lastQueryResult`，下一轮 prompt 注入当前启用的所有规则及参数

#### Scenario: 非法规则名

- **WHEN** AI 发出 `[COMMAND:auto_behavior add unknown_rule 10]`
- **THEN** 返回失败 ExecutionResult，reason="未知规则名：unknown_rule"
- **AND** 下一轮 prompt 注入失败原因，AI 能纠正

### Requirement: 规则冷却机制

The system SHALL enforce a per-rule cooldown (default 3000ms, configurable per rule) to prevent rapid-fire triggers.

#### Scenario: 冷却期内不重复触发

- **GIVEN** `player_approach_attack` 规则刚刚触发过
- **WHEN** 玩家仍在 5 格内，且冷却未过
- **THEN** 不再次触发该规则

#### Scenario: 不同规则独立冷却

- **GIVEN** `player_approach_attack` 刚触发，`low_health_eat` 也满足条件
- **WHEN** 扫描周期到达
- **THEN** `low_health_eat` 仍可触发，不受 `player_approach_attack` 冷却影响

### Requirement: 规则可配置

The system SHALL allow configuration of all rule parameters via `config.yml` under `ai.auto-behavior` section.

#### Scenario: 配置加载

- **GIVEN** `config.yml` 含 `ai.auto-behavior.enabled: true` 和各规则的 `enabled`/`distance`/`threshold` 参数
- **WHEN** 插件启动或 `/aip reload`
- **THEN** 所有 AI 的 `AutoBehaviorManager` 按配置初始化规则

#### Scenario: 全局禁用

- **GIVEN** `ai.auto-behavior.enabled: false`
- **WHEN** 插件启动
- **THEN** 不启动扫描任务，所有规则不触发

### Requirement: OP 管理命令

The system SHALL provide `/aip behavior list|toggle|reset <ai> [rule]` subcommands for OPs to manage AI rules.

#### Scenario: 列出规则

- **WHEN** OP 执行 `/aip behavior list 小明`
- **THEN** 显示小明当前所有规则的启用状态、参数、触发次数、最后触发时间

#### Scenario: 切换规则

- **WHEN** OP 执行 `/aip behavior toggle 小明 player_approach_attack`
- **THEN** 切换该规则的启用状态，返回新状态

#### Scenario: 重置规则

- **WHEN** OP 执行 `/aip behavior reset 小明`
- **THEN** 该 AI 规则恢复为 config.yml 的默认配置

## MODIFIED Requirements

### Requirement: AIPlayer 生命周期

`AIPlayer` SHALL hold an `AutoBehaviorManager` instance, initialized at construction. `AIPlayerManager.remove` SHALL call `autoBehaviorManager.cancel()` to clean up the scan task.

### Requirement: 系统提示词

The system prompt SHALL mention the auto-behavior feature so the LLM knows it can use `[COMMAND:auto_behavior add/remove/list]` to define its own logic.

## REMOVED Requirements

无。
