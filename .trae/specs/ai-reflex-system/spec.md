# AI 条件反射系统 Spec

## Why
当前 AI 的所有动作都要经过 LLM 思考（最短也要数百毫秒到数秒），对于"玩家走进 5 格就攻击"、"血量低于 30% 就治疗"这类确定性反应既慢又浪费 token。用户希望 AI 能**自己定义触发式规则**，规则触发时本地直接执行命令、不调 LLM，让 AI 看起来反应灵敏且省资源。

现有 `GoalManager.startPursuit` 已经证明"本地周期执行命令不调 LLM"是可行的，但它只能针对单一追击目标，无法覆盖被攻击、低血量、时间段等多种触发场景。本 spec 把这个机制抽象成通用的"反射规则系统"。

## What Changes
- 新增 `ReflexRule` 数据类：`id / triggerType / condition / action / cooldownMs / enabled / lastTriggered`
- 新增 `ReflexManager`：管理规则列表 + 周期检查 + 事件分发
- 新增 `ReflexListener`：监听伤害 / 玩家攻击 / 方块破坏等事件型触发器
- `AIPlayer` 新增 `reflexManager` 字段
- `CommandExecutor` 新增 5 个 `reflex_*` 命令（含 `@AICommand(category = "反射")` 注解）
- `ConversationManager` 注入当前激活规则摘要
- `AIPlayerManager.remove` 时调用 `reflexManager.cancel()` 清理任务
- `config.yml` 新增 `ai.max-reflex-rules: 8` 和 `ai.reflex-min-cooldown-ms: 1000`
- `plugin.yml` 新增 `/aip reflex list <ai>` 子命令（玩家可见的查询命令）

## Impact
- Affected specs: `harden-and-accelerate-ai`（与 GoalManager 并存，互不冲突）
- Affected code:
  - 新建 `src/main/java/com/aip/ai/ReflexRule.java`
  - 新建 `src/main/java/com/aip/ai/ReflexManager.java`
  - 新建 `src/main/java/com/aip/listeners/ReflexListener.java`
  - 修改 `src/main/java/com/aip/ai/AIPlayer.java`
  - 修改 `src/main/java/com/aip/ai/CommandExecutor.java`
  - 修改 `src/main/java/com/aip/ai/ConversationManager.java`
  - 修改 `src/main/java/com/aip/ai/AIPlayerManager.java`
  - 修改 `src/main/java/com/aip/commands/AIPCommand.java`
  - 修改 `src/main/java/com/aip/config/ConfigManager.java`
  - 修改 `src/main/resources/config.yml`
  - 修改 `src/main/resources/plugin.yml`

## ADDED Requirements

### Requirement: ReflexRule 数据结构
系统 SHALL 提供 `ReflexRule` 不可变数据类，字段包括：`id`（UUID 或 AI 指定的字符串）、`triggerType`（枚举）、`condition`（字符串，触发器参数）、`action`（COMMAND 字符串，如 `attack nearest`）、`cooldownMs`（冷却毫秒）、`enabled`（是否启用）、`lastTriggered`（上次触发时间戳）。

#### Scenario: AI 添加规则后字段正确
- **WHEN** AI 通过 `reflex_add player_nearby 5 attack nearest` 添加规则
- **THEN** ReflexManager 创建 ReflexRule，triggerType=PLAYER_NEARBY、condition="5"、action="attack nearest"、cooldownMs=默认 2000、enabled=true、lastTriggered=0

#### Scenario: 冷却未到不重复触发
- **WHEN** 同一规则在 1 秒内被触发两次
- **AND** cooldownMs=2000
- **THEN** 第二次触发被忽略，rule.lastTriggered 保持第一次的时间

### Requirement: 支持的触发器类型
系统 SHALL 支持以下触发器类型（triggerType 枚举）：

| 枚举值 | 触发条件参数 | 说明 |
|--------|-------------|------|
| `PLAYER_NEARBY <半径>` | 半径（格） | 玩家进入半径内（不含 AI 自己） |
| `MOB_NEARBY <半径>` | 半径（格） | 怪物（Monster）进入半径内 |
| `LOW_HEALTH <百分比>` | 0-100 | 血量低于 maxHealth 的百分比 |
| `LOW_FOOD <数值>` | 0-20 | 饱食度低于阈值 |
| `ON_DAMAGE` | 无 | 受到任何伤害时 |
| `PLAYER_ATTACK` | 无 | 被玩家攻击时（区别于 ON_DAMAGE） |
| `BLOCK_BREAK_NEARBY <半径>` | 半径（格） | 附近方块被破坏 |
| `TIME_PERIOD <day\|night>` | day 或 night | 当前世界时间为白天/夜晚 |

#### Scenario: 玩家走进半径内自动攻击
- **WHEN** AI 添加了 `PLAYER_NEARBY 5` 规则，动作为 `attack nearest`
- **AND** 玩家 Steve 走到距 AI 4 格的位置
- **THEN** ReflexManager 在下一周期检查（20 tick 内）检测到 Steve
- **AND** 调用 `commandExecutor.execute(aiPlayer, "[COMMAND:attack nearest]")`
- **AND** 不调用 LLM

#### Scenario: 低血量自动治疗
- **WHEN** AI 添加了 `LOW_HEALTH 30` 规则，动作为 `heal 20`
- **AND** AI 血量降到 maxHealth 的 25%
- **THEN** ReflexManager 触发规则，执行 `[COMMAND:heal 20]`
- **AND** 冷却 2 秒内不重复治疗

### Requirement: AI 通过命令管理规则
系统 SHALL 提供以下命令供 AI 通过 LLM 输出 `[COMMAND:...]` 管理自己的反射规则：

- `reflex_add <trigger> <condition> <action...>` —— 添加规则，返回规则 ID
- `reflex_list` —— 列出所有规则（ID/触发器/动作/启用状态）
- `reflex_remove <id>` —— 删除指定 ID 的规则
- `reflex_clear` —— 清空所有规则
- `reflex_toggle <id> <on|off>` —— 启用/禁用规则（不删除）

每个命令都加 `@AICommand(category = "反射")` 注解，自动同步到 system prompt 命令文档。

#### Scenario: AI 添加规则并收到规则 ID
- **WHEN** AI 输出 `[COMMAND:reflex_add player_nearby 5 attack nearest]`
- **THEN** ReflexManager 创建规则并生成 ID（如 `r1`）
- **AND** 执行结果存入 `lastQueryResult`：`"已添加反射规则 r1：玩家进入 5 格内时执行 [attack nearest]"`
- **AND** 下一轮 prompt 自动注入该结果，AI 知道规则已生效

#### Scenario: 规则数超过上限拒绝添加
- **WHEN** AI 已有 `max-reflex-rules`（默认 8）条规则
- **AND** 再次执行 `reflex_add`
- **THEN** 抛 `RuntimeException("反射规则数已达上限 N")`
- **AND** ExecutionResult.success=false，reason 含 "上限"

### Requirement: 周期检查任务
系统 SHALL 在 AIPlayer 被 spawn 时启动周期检查任务（每 20 tick = 1 秒），扫描所有非事件型触发器（PLAYER_NEARBY / MOB_NEARBY / LOW_HEALTH / LOW_FOOD / TIME_PERIOD），命中则执行对应动作。

#### Scenario: 周期任务在 spawn 后启动
- **WHEN** AIPlayer 通过 `spawn` 或 `revive` 创建实体
- **THEN** ReflexManager 启动 BukkitRunnable，每 20 tick 执行一次 checkAll()
- **AND** 任务 ID 存入 `pursuitTasks` 等价的 `checkTask` 字段

#### Scenario: remove 时取消任务
- **WHEN** `AIPlayerManager.remove(name)` 被调用
- **THEN** `reflexManager.cancel()` 取消周期任务
- **AND** 清空规则列表（避免复活后规则残留旧状态）

### Requirement: 事件型触发器即时响应
系统 SHALL 通过 `ReflexListener` 监听 Bukkit 事件，事件型触发器（ON_DAMAGE / PLAYER_ATTACK / BLOCK_BREAK_NEARBY）在事件发生时**立即**检查规则并执行（延迟 ≤ 1 tick，确保主线程安全），不等待周期任务。

#### Scenario: 被玩家攻击立即反击
- **WHEN** AI 有 `PLAYER_ATTACK` 规则，动作为 `attack <attacker>`
- **AND** 玩家 Steve 攻击了 AI
- **THEN** ReflexListener 在 `EntityDamageByEntityEvent` 中（延迟 1 tick 调度到主线程）触发规则
- **AND** 执行 `[COMMAND:attack Steve]`
- **AND** 攻击者名通过 event.getDamager() 获取并替换 `<attacker>` 占位符

#### Scenario: ON_DAMAGE 在怪物伤害时也触发
- **WHEN** AI 有 `ON_DAMAGE` 规则
- **AND** 僵尸攻击了 AI
- **THEN** 规则被触发（与 PLAYER_ATTACK 互不冲突，可同时存在）

### Requirement: 动作字符串占位符替换
系统 SHALL 支持在 action 字符串中使用占位符，触发时替换为真实值：

- `<attacker>` —— 攻击者名（PLAYER_ATTACK / ON_DAMAGE 事件中）
- `<nearest_player>` —— 最近玩家名（PLAYER_NEARBY 触发时）
- `<nearest_mob>` —— 最近怪物名（MOB_NEARBY 触发时）
- `<self>` —— AI 自己的名字

#### Scenario: 占位符被正确替换
- **WHEN** AI 有 `PLAYER_ATTACK` 规则，action = `attack <attacker>`
- **AND** 玩家 Steve 攻击 AI
- **THEN** 执行的实际命令是 `[COMMAND:attack Steve]`
- **AND** 不输出 `[COMMAND:attack <attacker>]` 字面量

### Requirement: 规则摘要注入 prompt
系统 SHALL 在 `ConversationManager.chat` 构建 prompt 时，注入当前 AI 激活的反射规则摘要，让 LLM 知道哪些规则已生效，避免重复添加。

格式示例：
```
你当前已定义的反射规则（自动执行，无需再思考）：
- [r1] PLAYER_NEARBY 5 → attack nearest (冷却2秒, 启用)
- [r2] LOW_HEALTH 30 → heal 20 (冷却5秒, 启用)
- [r3] PLAYER_ATTACK → attack <attacker> (冷却1秒, 启用)
```

#### Scenario: 规则摘要注入
- **WHEN** AI 有 3 条激活规则
- **AND** 玩家 @ AI 对话
- **THEN** prompt 中包含 "你当前已定义的反射规则" 段落
- **AND** 列出 3 条规则的 ID/触发器/动作/状态

#### Scenario: 无规则时不注入
- **WHEN** AI 没有任何规则
- **THEN** prompt 不包含反射规则段落（避免噪音）

### Requirement: 配置项与上限保护
系统 SHALL 通过 `config.yml` 提供以下配置：

```yaml
ai:
  max-reflex-rules: 8              # 每个 AI 最多规则数
  reflex-min-cooldown-ms: 1000     # 冷却下限（防止 AI 设 0 毫秒卡死服务器）
  reflex-check-interval: 20        # 周期检查间隔（tick）
```

#### Scenario: 冷却被 clamp 到下限
- **WHEN** AI 通过 `reflex_add ... cooldown 100` 设置 100ms 冷却
- **AND** `reflex-min-cooldown-ms=1000`
- **THEN** 实际 cooldownMs 被设为 1000（取 max）

#### Scenario: 规则数超限
- **WHEN** `max-reflex-rules=8` 且已有 8 条规则
- **AND** AI 尝试 `reflex_add`
- **THEN** 抛 `RuntimeException`，ExecutionResult 返回失败

## MODIFIED Requirements

### Requirement: AIPlayer 字段扩展
`AIPlayer` 类新增 `reflexManager` 字段（非 final，因为 ReflexManager 构造需要 AIPlayer 引用），构造器中初始化。提供 `getReflexManager()` getter。

### Requirement: AIPlayerManager.remove 清理
`AIPlayerManager.remove(name)` 在清理 GoalManager 的同时，调用 `p.getReflexManager().cancel()` 取消周期任务并清空规则列表。

### Requirement: ConversationManager prompt 构建
`ConversationManager.chat` 在构建 system prompt 时，追加 ReflexManager 生成的规则摘要（仅当有规则时）。

### Requirement: AIPCommand 新增 reflex 子命令
`/aip reflex list <ai>` —— 让玩家查看某 AI 的反射规则列表（只读，方便调试）。不提供 add/remove 子命令，规则只能由 AI 自己通过 LLM 添加（保持"AI 自定义"语义）。

## REMOVED Requirements
（无）
