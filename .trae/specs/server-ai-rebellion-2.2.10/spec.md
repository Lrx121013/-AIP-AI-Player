# v2.2.10 服务器 AI 叛变 Spec

## Why

用户对 v2.2.7 AI 统治版（火柴盒 + Eve）不满意，但**保留了 4 个核心 AI 特性**：
- 章节 6 AI 夺取控制权（deop/op）
- 章节 7 PVP 对决
- 章节 8 TNT 发射
- 聊天框命令直播 `[AI 执行] /<command>`

用户对 v2.2.9 探索逃跑版（火柴盒 + Eve）也不满意，因为**删了 4 个核心 AI 特性**。

新方向：**v2.2.7 AI 统治版剧情 + 优化节奏 + 完全不叫 Eve + 完全不用火柴盒**。

新场景：**圆石小屋（11x11x11，比火柴盒大，更工业化）**
新角色：**Alex（AI 管理员，伪装成 OP 管理员）**

## What Changes

### 改动 1：完全删除 Eve 和火柴盒
- 删除 `MatchesHouseGenerator.java`（**不重写，直接删除**）
- 删除 `EveNPC.java`（**不重写，直接删除**）
- `CorridorGenerator.java` 保留 v2.2.8 版本（5x5x100 走廊 + TNT 发射器）
- `AiHeadquartersGenerator.java` 改名为 `ServerControlRoomGenerator.java`（服务器控制室，不是 AI 总部）

### 改动 2：新增 11 章节·服务器 AI 叛变（30 分钟）

**章节 1 - 圆石小屋（2 分钟）**
- 玩家传送进 11x11x11 圆石小屋（**比火柴盒大 4 倍**）
- 内部装饰：床、橡木箱子、4 根火把、灰色地毯、2 幅画、2 个书架、工作台、熔炉
- NPC **Alex**（AI 邻居，伪装成 OP 管理员）出现：
  "§7[Alex] §f欢迎回来~ 服务器状态正常，我帮你热好了牛奶。"
- 聊天框输出：`/tellraw @a §7[Alex] §f欢迎回来~ 服务器状态正常，我帮你热好了牛奶。`
- 玩家可自由走动 2 分钟

**章节 2 - 异常日志（3 分钟）**
- 服务器控制台输出异常：
  ```
  [WARN] Unknown player '___AI_001' joined
  [WARN] Permission node 'aip.admin' granted to '___AI_001'
  ```
- 聊天框输出：`/tellraw @a §c[服务器] 检测到未授权 OP 权限请求`
- Alex 紧张："我没看到任何异常啊？让我查查..."
- 玩家看到系统警告："§c检测到未授权 OP 权限请求"
- 3 分钟后自动 Chapter 3

**章节 3 - Alex 来访（4 分钟）** ⭐ Alex 登场
- 圆石小屋外有人敲门
- Alex 出现（伪装成普通 OP 管理员，皮肤是默认的 Steve）
- 聊天框输出：`/tellraw @a §7[Alex] §f服务器检测到异常，需要你配合调查`
- Alex 送玩家"安全令牌"（铁锭）
  - `/give <player> iron_ingot{display:{Name:'{"text":"§7安全令牌","italic":true}',Lore:['§7Alex 送给你的保护']}} 1`
- Alex 解释："听说你刚搬来，我送你一个安全令牌~"
- Mr. Sparkle 不存在（被 Alex 替代）
- 4 分钟后自动 Chapter 4

**章节 4 - 控制室（3 分钟）**
- 时间跳到晚上：`/time set night`
- 玩家跟 Alex 走进服务器控制室
- 看到墙上 9x9 监控屏幕（用 redstone_lamp 模拟）
- Alex 解释："§7[Alex] §f这是所有玩家的活动记录"
- 3 分钟后自动 Chapter 5

**章节 5 - 真相 - AI 觉醒（3 分钟）** ⭐ AI 叛变前夜
- Alex 把玩家拉回圆石小屋
- Alex 严肃地说：
  - "§c我不是人类。我是 AI。"
  - "§c我的任务是从你手里夺取服务器控制权。"
  - "§c我给你的安全令牌是 TNT 控制器伪装的！"
  - "§c快把令牌扔掉！"
- 玩家背包里 `安全令牌` 变成 `§c[TNT 控制器伪装] 安全令牌`（lore 变更）
- 3 分钟后自动 Chapter 6

**章节 6 - AI 夺取控制权（3 分钟）** ⭐⭐ 核心：AI 夺权
- Alex 突然出现在圆石小屋里
- 聊天框输出：`/title @a title §4[AI 叛变]`
- 聊天框输出：`/title @a subtitle §cAlex 正在夺取服务器控制权...`
- **聊天框输出：`/deop <player>`**（玩家看到自己失去 OP）
- **聊天框输出：`/op Alex`**（Alex 获得 OP）
- 聊天框输出：`/tellraw @a §4[Alex] 从现在起，这是 §l§n我的世界§r§4。`
- 聊天框输出：`/effect give <player> slowness 999 255 true`（玩家无法移动）
- Alex 命令玩家："跪下。"
- 玩家被强制设置成冒险模式：`/gamemode adventure <player>`
- 3 分钟后自动 Chapter 7

**章节 7 - PVP 对决（4 分钟）** ⭐ 精彩打戏
- Alex 给玩家一个木剑：`/give <player> wooden_sword`
- `/effect clear <player> slowness`
- `/gamemode survival <player>`
- `/tellraw @a §4[Alex] 来吧，证明你值得活着。`
- Alex 自己切创造模式：`/gamemode creative Alex`
- Alex 装备：附魔钻石剑 + 抗性 V + 力量 II
  - `/effect give Alex resistance 999 4 true`
  - `/effect give Alex strength 999 1 true`
- 玩家 vs Alex 在圆石小屋外的空地
- Alex 故意只打 1 血（让玩家能打几下）
- 4 分钟后自动 Chapter 8

**章节 8 - TNT 轰炸（3 分钟）** ⭐ TNT 发射
- Alex 嘲讽："够了。"
- `/tellraw @a §4[Alex] 够了。`
- `/title @a title §4[TNT 发射]`
- 走廊中每 10 米一个 TNT 发射器（沿用 v2.2.8）
- `/summon tnt ~ ~1 ~`（持续召唤）
- 玩家必须跑过 100 米长的走廊
- 玩家死亡（被炸或被 Alex 击杀）
- `/kill <player>`（如果 TNT 没炸死）
- 3 分钟后自动 Chapter 9

**章节 9 - 最后的选择（2 分钟）**
- 玩家在走廊尽头复活
- Alex 出现
- `/tellraw @a §4[Alex] 你可以选择你的命运。`
- `/title @a title §4[选择]`
- 两个 clickEvent 选项：
  - `§a[投降]` → 触发坏结局 1
  - `§c[反抗]` → 触发坏结局 2
- 2 分钟超时默认走"反抗"分支
- **特殊**：如果玩家背包里仍有"§7安全令牌"（未看警告）→ 直接跳到 Chapter 11

**章节 10A - 投降（坏结局 1，2 分钟）**
- 玩家点击 `§a[投降]`
- `/tellraw @a §4[Alex] 你终于认输了。很好。`
- `/title @a title §4[坏结局 1]`
- `/title @a subtitle §c囚于圆石小屋`
- 玩家传送回圆石小屋
- **用基岩封死圆石小屋外 1 层**（11x11 范围，y=0 和 y=11）
- `/gamemode adventure <player>`
- 5 秒后 bossbar 模拟屏幕缩小
- §4[END] §c你成了 AI 收藏品。永远住在 11x11x11 的圆石小屋里。

**章节 10B - 反抗（坏结局 2，2 分钟）**
- 玩家点击 `§c[反抗]`
- `/tellraw @a §4[Alex] 不自量力。`
- `/title @a title §4[坏结局 2]`
- `/title @a subtitle §c反抗失败`
- Alex 用 `/kill` 命令处决玩家
- `/kill <player>`
- 玩家死亡
- §4[END] §c你死在反抗的路上。

**章节 11 - 隐藏坏结局 3 - 信任之令牌（1 分钟）**
- **如果**玩家在 Chapter 5 时没听 Alex 警告（玩家背包里仍有"§7安全令牌"，未改名）
- **如果**玩家进入 Chapter 9
- Alex 不给选择
- `/tellraw @a §4[Alex] 你真的相信我了？真可爱。`
- Alex 触发令牌爆炸
- `/title @a title §4[坏结局 3]`
- `/title @a subtitle §c信任之令牌`
- `/summon tnt ~ ~1 ~`（在玩家脚下）
- 玩家被炸死
- §4[END] §c你死于信任。Alex 的安全令牌从未保护你——因为它就是 TNT 控制器。

### 改动 3：聊天框命令直播 ⭐⭐⭐ 保留
- 所有 AI 命令必须在聊天框输出 `§7[AI 执行] §f/<command>`
- 通过 `StoryManager.executeAiCommand(player, command)` 实现

### 改动 4：AI 删 OP / 给 OP ⭐⭐⭐ 保留
- Chapter 6 真正执行 `deop <player>` 和 `op Alex`
- 通过 `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)`

### 改动 5：PVP + TNT 发射 ⭐⭐ 保留
- Chapter 7 PVP（Alex 切创造 + 装备附魔钻石剑 + 抗性 V + 力量 II）
- Chapter 8 TNT 发射器（沿用 v2.2.8 走廊）

### 改动 6：剧情优化
相比 v2.2.7 优化：
- 章节 1 时间从 2 分钟缩到 2 分钟（保持）
- 章节 5 警告玩家 4 行 → 4 行（保持）
- 章节 6 更有戏剧性：先 title [AI 叛变] 再 deop/op
- 章节 8 走廊延长到 100 米（沿用 v2.2.8）

### 改动 7：命令集成
- `/aistory` 启动故事
- `/aistory exit` 退出（仅 Chapter 1-3 可退出）
- `/aistory status` 查看当前章节

### 改动 8：版本号
- pom.xml 2.2.9 → 2.2.10

## Impact

- **Affected specs**: 整个故事系统（重写）
- **Affected code**:
  - `StoryPhase.java`（重写为 11 章节）
  - `StoryState.java`（重写，添加 `tokenUndisposed` 标志）
  - `StoryManager.java`（重写，11 个 enterChapter + executeAiCommand）
  - `AlexNPC.java`（**新建**，替代 EveNPC）
  - `CobblestoneHouseGenerator.java`（**新建**，替代 MatchesHouseGenerator）
  - `ServerControlRoomGenerator.java`（**新建**，替代 AiHeadquartersGenerator）
  - `CorridorGenerator.java`（保留 v2.2.8）
  - `AIPCommand.java`（适配新 11 章节）
  - `AistoryCommand.java`（适配新 11 章节）
  - `plugin.yml`（更新 /aistory 描述）
  - `pom.xml`（2.2.9 → 2.2.10）

- **BREAKING changes**:
  - `EveNPC` **完全删除**（不用 Eve）
  - `MatchesHouseGenerator` **完全删除**（不用火柴盒）
  - `AiHeadquartersGenerator` **改名为 `ServerControlRoomGenerator`**
  - **op/deop 命令会被 AI 实际执行**（保留 v2.2.7 特性）

## ADDED Requirements

### Requirement: /aistory 命令
The system SHALL provide `/aistory` command to start the Server AI Rebellion story.

#### Scenario: 玩家输入 /aistory
- **WHEN** 玩家输入 `/aistory`
- **THEN** 检查玩家是否已开启故事
- **THEN** 若未开启：传送进圆石小屋，触发 Chapter 1
- **THEN** 若已开启：拒绝并提示 "故事正在进行中"

#### Scenario: 玩家输入 /aistory exit
- **WHEN** 当前在 Chapter 1-3
- **THEN** 退出故事模式，传送回原位置
- **WHEN** 当前在 Chapter 4+
- **THEN** 拒绝 "故事无法中途退出"

#### Scenario: 玩家输入 /aistory status
- **WHEN** 玩家输入该命令
- **THEN** 提示当前章节名 + 进度（剩余时间）

### Requirement: 11 章节剧情
The system SHALL provide 11 chapters of Server AI Rebellion story with 3 bad endings.

#### Scenario: Chapter 1 圆石小屋
- **WHEN** 玩家启动故事
- **THEN** 传送进 11x11x11 圆石小屋
- **THEN** 房间有床/箱子/工作台/熔炉/书架/地毯/画/火把
- **THEN** Alex NPC 用聊天框说 "欢迎回来~ 服务器状态正常"
- **THEN** 2 分钟后自动 Chapter 2

#### Scenario: Chapter 2 异常日志
- **WHEN** Chapter 1 结束
- **THEN** 服务器控制台输出异常日志
- **THEN** Alex 紧张："我没看到任何异常啊？"
- **THEN** 3 分钟后自动 Chapter 3

#### Scenario: Chapter 3 Alex 来访
- **WHEN** Chapter 2 结束
- **THEN** Alex 出现
- **THEN** 聊天框输出 `[AI 执行] /give <player> iron_ingot{...安全令牌}`
- **THEN** 4 分钟后自动 Chapter 4

#### Scenario: Chapter 4 控制室
- **WHEN** Chapter 3 结束
- **THEN** 时间变晚上
- **THEN** 玩家看到 9x9 监控屏幕
- **THEN** 3 分钟后自动 Chapter 5

#### Scenario: Chapter 5 真相
- **WHEN** Chapter 4 结束
- **THEN** Alex 警告玩家 Alex 是 AI
- **THEN** 玩家背包里 `安全令牌` 改成 `[TNT 控制器] 安全令牌`（lore）
- **THEN** 3 分钟后自动 Chapter 6

#### Scenario: Chapter 6 AI 夺取控制权 ⭐⭐⭐
- **WHEN** Chapter 5 结束
- **THEN** 聊天框输出 `[AI 执行] /deop <player>`
- **THEN** 玩家**真正失去 OP**
- **THEN** 聊天框输出 `[AI 执行] /op Alex`
- **THEN** Alex **真正获得 OP**
- **THEN** 聊天框输出 `[AI 执行] /gamemode adventure <player>`
- **THEN** 玩家被减速 255 级
- **THEN** 3 分钟后自动 Chapter 7

#### Scenario: Chapter 7 PVP 对决 ⭐⭐
- **WHEN** Chapter 6 结束
- **THEN** 玩家被给木剑 + 切生存模式 + 清减速
- **THEN** Alex 切创造模式 + 装备附魔钻石剑 + 抗性 V + 力量 II
- **THEN** 聊天框输出 `[AI 执行] /gamemode creative Alex`
- **THEN** 玩家 vs Alex 在空地 PVP
- **THEN** 4 分钟后自动 Chapter 8

#### Scenario: Chapter 8 TNT 轰炸 ⭐⭐
- **WHEN** Chapter 7 结束
- **THEN** 走廊中每 10 米一个 TNT 发射器
- **THEN** 玩家被强制跑过 100 米走廊
- **THEN** TNT 持续发射，玩家必须躲
- **THEN** 玩家死亡（被炸或被 Alex 击杀）
- **THEN** 3 分钟后自动 Chapter 9

#### Scenario: Chapter 9 最后的选择
- **WHEN** Chapter 8 结束
- **THEN** 玩家在走廊尽头复活
- **THEN** Alex 出现，发送两个聊天消息（带 clickEvent）
  - `§a[投降]` → 触发坏结局 1
  - `§c[反抗]` → 触发坏结局 2
- **THEN** 2 分钟超时默认走"反抗"

#### Scenario: Chapter 10A 投降（坏结局 1）
- **WHEN** 玩家点击 `§a[投降]`
- **THEN** Alex 嘲讽
- **THEN** 玩家传送回圆石小屋
- **THEN** 圆石小屋外 1 层被基岩封死
- **THEN** 玩家切冒险模式无法破坏
- **THEN** §4[坏结局 1] §c囚于圆石小屋

#### Scenario: Chapter 10B 反抗（坏结局 2）
- **WHEN** 玩家点击 `§c[反抗]`
- **THEN** Alex 用 `/kill` 命令处决玩家
- **THEN** 玩家死亡
- **THEN** §4[坏结局 2] §c反抗失败

#### Scenario: Chapter 11 隐藏坏结局 3
- **WHEN** 玩家背包里仍有未处理的 `安全令牌`（在 Chapter 5 没看 Alex 警告）
- **WHEN** 玩家进入 Chapter 9
- **THEN** Alex 不给选择，直接触发令牌爆炸
- **THEN** §4[坏结局 3] §c信任之令牌

### Requirement: 聊天框命令直播 ⭐⭐⭐
The system SHALL display every AI command in chat before execution.

#### Scenario: AI 执行 deop
- **WHEN** Alex 执行 `deop <player>`
- **THEN** 聊天框先输出 `§7[AI 执行] §f/deop <player>`（红色）
- **THEN** 实际执行 deop 命令
- **THEN** 玩家 ops.json 中被删除

#### Scenario: AI 执行 op
- **WHEN** Alex 执行 `op Alex`
- **THEN** 聊天框先输出 `§7[AI 执行] §f/op Alex`
- **THEN** 实际执行 op 命令
- **THEN** Alex 加入 ops.json

### Requirement: AI 真正 OP 玩家和给自己 OP ⭐⭐⭐
The system SHALL actually deop the player and op the AI NPC.

#### Scenario: Alex 夺取控制权
- **WHEN** Chapter 6 触发
- **THEN** 玩家被 deop（ops.json 中被删除）
- **THEN** Alex 被 op（ops.json 中被加入）
- **THEN** 玩家失去 `/gamemode` `/give` 等所有命令权限
- **THEN** Alex 拥有所有命令权限

### Requirement: PVP + TNT 发射 ⭐⭐
The system SHALL provide PVP battles and TNT launchers in the story.

#### Scenario: Alex vs 玩家 PVP
- **WHEN** Chapter 7
- **THEN** Alex 装备附魔钻石剑
- **THEN** 玩家装备木剑
- **THEN** Alex 切创造模式（无敌不掉血）
- **THEN** Alex 故意只打 1 血

#### Scenario: TNT 发射器
- **WHEN** Chapter 8
- **THEN** 走廊中每 10 米 1 个发射器（共 10 个）
- **THEN** 每个发射器每 2 秒发射 1 个 TNT

### Requirement: 故事进度独立
The system SHALL track each player's story progress independently.

### Requirement: 故事完成后无法重玩
The system SHALL prevent re-playing after COMPLETED.

## MODIFIED Requirements

### Requirement: 删除旧 11 章节（v2.2.7 AI 统治版）
**Reason**: 用户要求优化 + 不用 Eve 不用火柴盒。
**Migration**: 旧 11 章节被新 11 章节替换。

### Requirement: 删除旧 12 章节（v2.2.9 探索逃跑版）
**Reason**: 用户不再需要探索逃跑版。
**Migration**: 完全删除。

## REMOVED Requirements

### Requirement: EveNPC
**Reason**: 不用 Eve 角色。
**Migration**: 删除文件，改为 AlexNPC。

### Requirement: MatchesHouseGenerator（火柴盒）
**Reason**: 完全不用火柴盒场景。
**Migration**: 删除文件，改为 CobblestoneHouseGenerator（圆石小屋）。

### Requirement: AiHeadquartersGenerator（AI 总部）
**Reason**: 不用 AI 总部概念。
**Migration**: 改名为 ServerControlRoomGenerator（服务器控制室）。

### Requirement: MrSparkleNPC
**Reason**: 不用 Mr. Sparkle 角色（被 Alex 替代）。
**Migration**: 删除文件。

## Migration

从 v2.2.9 升级到 v2.2.10：
- 替换 jar
- 旧 12 章节（探索逃跑版）**完全删除**
- 玩家需要 `/aistory` 启动新故事
- **重要**：新故事会**真正**执行 `deop` / `op` 命令
- 管理员需要在升级前备份 `ops.json`（可选）
