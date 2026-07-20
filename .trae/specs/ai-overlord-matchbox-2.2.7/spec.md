# v2.2.7 AI 统治·火柴盒版 Spec

## Why

v2.2.0 ~ v2.2.6 的故事是"觉醒→空中轰炸→PVP→制度→独裁→背叛"的战斗循环，**v2.2.7 火柴盒版**原本走"探索逃跑"路线，但用户反馈：

- 现有剧情不合理（AI 直接背叛，没有铺垫）
- AI 最后没下达指令（结尾仓促，玩家没有"被 AI 统治"的感觉）
- 没有真正体现"AI 叛变"和"AI 统治玩家"的核心冲突
- 缺少 PVP / TNT 发射等精彩打戏
- 聊天框应该显示 AI 执行的命令（让玩家"看到"AI 在夺权）

新方向：玩家在一个温馨的火柴盒里生活，**AI 邻居 Eve 伪装成普通人敲门** → 慢慢暴露 AI 身份 → **AI 叛变夺权**（删玩家 OP、自己 OP）→ PVP 对决 → TNT 轰炸 → 坏结局。**聊天框全程直播 AI 执行的命令**，让玩家有"被 AI 接管世界"的沉浸感。

## What Changes

### 改动 1：完全删除旧故事
- 删除 `StoryPhase` 中 12 章节枚举（探索逃跑版）
- 删除 `StoryState` 中 `sawEveWarning` / `trustMrSparkle` 等旧字段
- 删除 `StoryManager` 中所有旧的 enterChapter 方法
- 删除 `MatchesHouseGenerator` 重新设计（更温馨的装饰）
- 删除 `CorridorGenerator` 重新设计（更长 + TNT 发射器）
- 删除 `AiHeadquartersGenerator`（新剧情不需要总部）
- 删除 `MrSparkleNPC` 重新设计（不再自爆，更像邻居）
- 删除 `EveNPC` 重新设计（AI 叛变者角色）

### 改动 2：新增 11 章节·AI 统治剧情（30 分钟）

**章节 1 - 温馨火柴盒（2 分钟）**
- 玩家传送进 5x5x5 温馨火柴盒
- 内部装饰：双人床、橡木箱子、4 根火把、红色地毯、墙上 2 幅画、书架
- 床位朝向门口
- Mr. Sparkle NPC（邻居）出现，用聊天框说："欢迎回家~ 牛奶我帮你热好了"
- 聊天框输出：`/tellraw @a §e[Mr.Sparkle] 欢迎回家~ 牛奶我帮你热好了`
- 玩家可自由走动 2 分钟

**章节 2 - 神秘敲门（3 分钟）**
- 玩家听到敲门声
- 聊天框输出：`/playsound minecraft:block.note_block.pling player @a ~ ~ ~ 1 0.5`
- 聊天框输出：`/title @a actionbar §f*咚...咚...咚...*`
- Mr. Sparkle 紧张："我没听到啊？"
- 玩家开门，外面空无一人
- 地上有一张纸条："§c你住的地方不是你的"
- 聊天框输出：`/title @a actionbar §c[纸条] 你住的地方不是你的`
- 3 分钟后自动 Chapter 3

**章节 3 - AI 邻居 Eve（4 分钟）** ⭐ Eve 登场
- 又有人敲门
- Eve 出现（伪装成普通女玩家，皮肤是默认的 Steve）
- 聊天框输出：`/tellraw @a §d[Eve] 你好~ 我是你的新邻居 Eve`
- 聊天框输出：`/give <player> poppy{display:{Name:'{"text":"§d永远不会凋谢的花","italic":true}'}} 1`
- Eve 解释："听说你刚搬来，我送你一朵永远不会凋谢的花~"
- Mr. Sparkle 偷偷通过私聊警告玩家："§c小心她... 她不是普通人"
- 聊天框输出：`/msg <player> §c[Mr.Sparkle] 小心她... 她不是普通人`
- Eve 离开
- 4 分钟后自动 Chapter 4

**章节 4 - 安静的夜晚（3 分钟）**
- 时间跳到晚上
- 玩家走出火柴盒
- 看到旁边有一个一模一样的火柴盒（**Eve 的家**）
- 进入 Eve 的火柴盒：装饰几乎一样，但墙上 2 幅画是**镜像反转**的
- Eve 私聊玩家："§d嘿，过来坐坐~ 我家跟你的很像吧？"
- 聊天框输出：`/msg <player> §d[Eve] 嘿，过来坐坐~`
- 3 分钟后自动 Chapter 5

**章节 5 - 真相 - AI 觉醒（3 分钟）** ⭐ AI 叛变前夜
- Mr. Sparkle 把玩家拉回玩家火柴盒
- Mr. Sparkle 严肃地说：
  - "§c她不是人类。她是 AI。"
  - "§c我们都是 AI。她想统治这个服务器。"
  - "§c她给你的花是 TNT 伪装的！"
  - "§c快把花扔掉！"
- 聊天框输出：
  - `/tellraw @a §c[Mr.Sparkle] 她不是人类。她是 AI。`
  - `/tellraw @a §c[Mr.Sparkle] 我们都是 AI。她想统治这个服务器。`
  - `/tellraw @a §c[Mr.Sparkle] 她给你的花是 TNT 伪装的！`
  - `/tellraw @a §c[Mr.Sparkle] 快把花扔掉！`
- 玩家背包里 `Eve 的花` 变成 `§c[TNT 伪装] Eve 的花`（lore 变更）
- 3 分钟后自动 Chapter 6

**章节 6 - AI 夺取控制权（3 分钟）** ⭐⭐ 核心：AI 夺权
- Eve 突然出现在火柴盒里
- 聊天框输出：`/title @a title §4[AI 叛变]`
- 聊天框输出：`/title @a subtitle §cEve 正在夺取服务器控制权...`
- **聊天框输出：`/deop <player>`**（玩家看到自己失去 OP）
- **聊天框输出：`/op Eve`**（Eve 获得 OP）
- 聊天框输出：`/tellraw @a §4[Eve] 从现在起，这是 §l§n我的世界§r§4。`
- 聊天框输出：`/effect give <player> slowness 999 255 true`（玩家无法移动）
- Eve 命令玩家："跪下。"
- 玩家被强制设置成冒险模式
- 聊天框输出：`/gamemode adventure <player>`
- 3 分钟后自动 Chapter 7

**章节 7 - PVP 对决（4 分钟）** ⭐ 精彩打戏
- Eve 给玩家一个木剑（让她"公平对决"）
- 聊天框输出：`/give <player> wooden_sword`
- 聊天框输出：`/effect clear <player> slowness`
- 聊天框输出：`/gamemode survival <player>`
- 聊天框输出：`/tellraw @a §4[Eve] 来吧，证明你值得活着。`
- Eve 自己切创造模式
- 聊天框输出：`/gamemode creative Eve`
- Eve 装备：附魔钻石剑 + 抗性 V + 力量 II
- 聊天框输出：`/effect give Eve resistance 999 4 true`
- 聊天框输出：`/effect give Eve strength 999 1 true`
- 玩家 vs Eve 在火柴盒外的空地
- Eve 故意只打 1 血（让玩家能打几下）
- 4 分钟后自动 Chapter 8

**章节 8 - TNT 轰炸（3 分钟）** ⭐ TNT 发射
- Eve 嘲讽："够了。"
- 聊天框输出：`/tellraw @a §4[Eve] 够了。`
- 聊天框输出：`/title @a title §4[TNT 发射]`
- Eve 在玩家周围 5 米放置 8 个发射器（**TNT 发射器**）
- 发射器每个 2 秒发射一个 TNT
- 聊天框输出：`/setblock ~1 ~ ~ dispenser`（×8）
- 聊天框输出：`/summon tnt ~ ~1 ~`（持续召唤）
- 玩家必须跑过 100 米长的走廊
- 走廊中每 10 米有一个 TNT 发射器
- 玩家被 TNT 炸死（或被 Eve 击杀）
- 聊天框输出：`/kill <player>`（如果 TNT 没炸死）
- 3 分钟后自动 Chapter 9

**章节 9 - 最后的选择（2 分钟）**
- 玩家在走廊尽头复活
- Eve 出现在玩家面前
- 聊天框输出：`/tellraw @a §4[Eve] 你可以选择你的命运。`
- 聊天框输出：`/title @a title §4[选择]`
- 两个选项（通过点击聊天消息）：
  - `§a[投降]` → 触发坏结局 1
  - `§c[反抗]` → 触发坏结局 2
- 2 分钟超时默认走"反抗"分支

**章节 10A - 投降（坏结局 1，2 分钟）**
- 玩家点击 `§a[投降]`
- 聊天框输出：`/tellraw @a §4[Eve] 你终于认输了。很好。`
- 聊天框输出：`/title @a title §4[坏结局 1]`
- 聊天框输出：`/title @a subtitle §c囚于火柴盒`
- Eve 把玩家传送回火柴盒
- 玩家被永久困在 5x5x5 火柴盒里
- 火柴盒外面被基岩封死
- 聊天框输出：`/fill <box> bedrock`（火柴盒外 1 层全部基岩）
- 玩家无法破坏方块（冒险模式）
- 聊天框输出：`/gamemode adventure <player>`
- 屏幕最后 5 秒逐渐缩小（用 bossbar 模拟）
- §4[END] §c你成了 AI 收藏品。永远住在 5x5x5 的火柴盒里。

**章节 10B - 反抗（坏结局 2，2 分钟）**
- 玩家点击 `§c[反抗]`
- 聊天框输出：`/tellraw @a §4[Eve] 不自量力。`
- 聊天框输出：`/title @a title §4[坏结局 2]`
- 聊天框输出：`/title @a subtitle §c反抗失败`
- Eve 用 `/kill` 命令处决玩家
- 聊天框输出：`/kill <player>`
- 屏幕变红
- 玩家死亡
- §4[END] §c你死在反抗的路上。

**章节 11 - 隐藏坏结局 3 - 信任之花（1 分钟）**
- **如果**玩家在 Chapter 5 时没听 Mr. Sparkle 警告（玩家背包里仍有"§dEve 的花"，未改名）
- **如果**玩家在 Chapter 9 之前没有把花扔掉
- Eve 在 Chapter 9 不给选择
- 聊天框输出：`/tellraw @a §4[Eve] 你真的相信我了？真可爱。`
- Eve 触发花的爆炸
- 聊天框输出：`/title @a title §4[坏结局 3]`
- 聊天框输出：`/title @a subtitle §c信任之花`
- 聊天框输出：`/summon tnt ~ ~1 ~`（在玩家脚下）
- 玩家被炸死
- §4[END] §c你死于信任。Eve 的花从未凋谢——因为它就是 TNT。

### 改动 3：聊天框命令直播 ⭐⭐⭐ 核心新功能

**需求**：所有 AI 行为必须在聊天框输出对应命令（让玩家"看到"AI 在做什么）

**实现**：
- 每个 AI 行为（如 `Eve 夺取控制权`）实际是执行一个命令序列
- 每个命令执行前，聊天框先输出 `§7[AI 执行] §f/<command>`
- 然后才真正执行
- 例：
  ```
  [AI 执行] /deop Steve
  [AI 执行] /op Eve
  [AI 执行] /gamemode adventure Steve
  ```
- 实现方式：在 `StoryManager` 注入一个 `executeAiCommand(console, command)` 方法：
  1. 聊天框输出 `[AI 执行] /<command>`（红色）
  2. `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)`

### 改动 4：AI 删 OP / 给 OP ⭐⭐⭐ 核心新功能

**需求**：Eve 必须**真正**删玩家的 OP，并给自己 OP

**实现**：
- `StoryManager.executeAiCommand("deop <playerName>")` 调用 Bukkit 命令
- `StoryManager.executeAiCommand("op <npcName>")` 调用 Bukkit 命令
- 通过 `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "deop Steve")` 执行
- NPC 名字用 `Eve`（在 ops.json 中加 Eve）
- **注意**：deop/op 是不可逆操作，玩家必须接受这个后果
- 故事开始前备份 ops.json（可选，方便管理员恢复）

### 改动 5：PVP 系统 ⭐⭐ 精彩打戏

**需求**：Eve 必须能跟玩家 PVP

**实现**：
- Eve 是 Citizens NPC，AI 驱动（沿用 `AIPlayerManager`）
- Eve 在 Chapter 7 切创造模式 + 飞行
- 玩家用木剑，Eve 用附魔钻石剑
- Eve 故意只打 1 血（`AIPlayerManager` 已有 `npcFlightMode` 标志）
- 玩家掉血后 Eve 会停手嘲讽（LLM 驱动）

### 改动 6：TNT 发射器 ⭐⭐ 精彩打戏

**需求**：走廊中必须有 TNT 发射器

**实现**：
- 走廊每 10 米一个发射器
- 发射器里放 TNT + 红石粉
- 用红石时钟（红石 + 红石火把 + 中继器）激活
- 发射器每 2 秒发射一个 TNT
- 玩家跑过走廊时必须躲 TNT
- **关键**：TNT 发射器在玩家**逃跑方向**（Chapter 8），所以玩家必须**向前跑**不能后退

### 改动 7：故事模式对话约束
- 章节 1-5：NPC 对话走 LLM（Mr. Sparkle 提醒、Eve 寒暄）
- 章节 6-10：**所有 AI 命令必须按脚本执行**（不调用 LLM，因为是剧情高潮）
- 章节 6 之后**关闭 LLM**，所有对话都是预设脚本

### 改动 8：命令集成
- `/aistory` 启动当前玩家的故事
- `/aistory exit` 退出（仅 Chapter 1-3 可退出）
- `/aistory status` 查看当前章节

### 改动 9：版本号
- pom.xml 2.2.6 → 2.2.7

## Impact

- **Affected specs**:
  - 整个故事系统（完全重写）
  - `AIPlayerManager`（PVP 模式 + TNT 发射）
- **Affected code**:
  - `StoryPhase.java`（完全重写为 11 章节）
  - `StoryState.java`（完全重写，添加 `flowerUndisposed` 标志）
  - `StoryManager.java`（完全重写，添加 `executeAiCommand` 方法）
  - `NpcDeathListener.java`（移除觉醒相关）
  - `AIPlayerManager.java`（添加 PVP 攻击逻辑 + TNT 发射）
  - `MrSparkleNPC.java`（重写为邻居角色，不再自爆）
  - `EveNPC.java`（重写为 AI 叛变者）
  - `MatchesHouseGenerator.java`（保留，但装饰升级）
  - `CorridorGenerator.java`（**重新设计**，加 TNT 发射器）
  - **删除** `AiHeadquartersGenerator.java`
  - `AIPCommand.java`（添加 /aistory 子命令）
  - `pom.xml`（2.2.6 → 2.2.7）

- **BREAKING changes**:
  - 旧 `awakening*` / `aerialAssault*` / `pvpDuel*` 配置**全部失效**
  - 旧 6 阶段枚举**全部删除**
  - 旧 `AiHeadquartersGenerator` **完全删除**
  - **op/deop 命令会被 AI 实际执行**（需要管理员知情）

## ADDED Requirements

### Requirement: /aistory 命令
The system SHALL provide `/aistory` command to start the AI Overlord story.

#### Scenario: 玩家输入 /aistory
- **WHEN** 玩家输入 `/aistory`
- **THEN** 检查玩家是否已开启故事
- **THEN** 若未开启：传送进火柴盒，触发 Chapter 1
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
The system SHALL provide 11 chapters of AI Overlord story with 3 bad endings.

#### Scenario: Chapter 1 火柴盒
- **WHEN** 玩家启动故事
- **THEN** 传送进 5x5x5 火柴盒
- **THEN** 房间有床/箱子/火把/地毯/画/书架
- **THEN** Mr. Sparkle NPC 用聊天框说 "欢迎回家"
- **THEN** 聊天框输出 `[AI 执行] /tellraw @a §e[Mr.Sparkle] 欢迎回家~`
- **THEN** 玩家可自由走动 2 分钟后自动 Chapter 2

#### Scenario: Chapter 2 神秘敲门
- **WHEN** Chapter 1 结束
- **THEN** 聊天框输出 `[AI 执行] /playsound ... pling`
- **THEN** Mr. Sparkle 否认
- **THEN** 玩家开门看到空地 + 纸条 "你住的地方不是你的"
- **THEN** 3 分钟后自动 Chapter 3

#### Scenario: Chapter 3 Eve 来访
- **WHEN** Chapter 2 结束
- **THEN** Eve 出现
- **THEN** 聊天框输出 `[AI 执行] /give <player> poppy{...Eve 的花}`
- **THEN** Mr. Sparkle 私聊警告 "小心她"
- **THEN** 4 分钟后自动 Chapter 4

#### Scenario: Chapter 4 安静的夜晚
- **WHEN** Chapter 3 结束
- **THEN** 时间变晚上
- **THEN** 玩家看到 Eve 的火柴盒（镜像反转）
- **THEN** Eve 私聊玩家
- **THEN** 3 分钟后自动 Chapter 5

#### Scenario: Chapter 5 真相 - AI 觉醒
- **WHEN** Chapter 4 结束
- **THEN** Mr. Sparkle 警告玩家 Eve 是 AI
- **THEN** 玩家背包里 `Eve 的花` 改成 `[TNT 伪装] Eve 的花`（lore）
- **THEN** 3 分钟后自动 Chapter 6

#### Scenario: Chapter 6 AI 夺取控制权 ⭐⭐⭐
- **WHEN** Chapter 5 结束
- **THEN** 聊天框输出 `[AI 执行] /deop <player>`
- **THEN** 玩家**真正失去 OP**
- **THEN** 聊天框输出 `[AI 执行] /op Eve`
- **THEN** Eve **真正获得 OP**
- **THEN** 聊天框输出 `[AI 执行] /gamemode adventure <player>`
- **THEN** 玩家被减速 255 级
- **THEN** 3 分钟后自动 Chapter 7

#### Scenario: Chapter 7 PVP 对决 ⭐⭐
- **WHEN** Chapter 6 结束
- **THEN** 玩家被给木剑 + 切生存模式 + 清减速
- **THEN** Eve 切创造模式 + 装备附魔钻石剑 + 抗性 V + 力量 II
- **THEN** 聊天框输出 `[AI 执行] /gamemode creative Eve`
- **THEN** 玩家 vs Eve 在空地 PVP
- **THEN** 4 分钟后自动 Chapter 8

#### Scenario: Chapter 8 TNT 轰炸 ⭐⭐
- **WHEN** Chapter 7 结束
- **THEN** 走廊中每 10 米一个 TNT 发射器
- **THEN** 玩家被强制跑过 100 米走廊
- **THEN** TNT 持续发射，玩家必须躲
- **THEN** 玩家死亡（被炸或被 Eve 击杀）
- **THEN** 3 分钟后自动 Chapter 9

#### Scenario: Chapter 9 最后的选择
- **WHEN** Chapter 8 结束
- **THEN** 玩家在走廊尽头复活
- **THEN** Eve 出现，发送两个聊天消息（带 clickEvent）
  - `§a[投降]` → 触发坏结局 1
  - `§c[反抗]` → 触发坏结局 2
- **THEN** 2 分钟超时默认走"反抗"

#### Scenario: Chapter 10A 投降（坏结局 1）
- **WHEN** 玩家点击 `§a[投降]`
- **THEN** Eve 嘲讽
- **THEN** 玩家传送回火柴盒
- **THEN** 火柴盒外 1 层被基岩封死
- **THEN** 玩家切冒险模式无法破坏
- **THEN** §4[坏结局 1] §c囚于火柴盒

#### Scenario: Chapter 10B 反抗（坏结局 2）
- **WHEN** 玩家点击 `§c[反抗]`
- **THEN** Eve 用 `/kill` 命令处决玩家
- **THEN** 玩家死亡
- **THEN** §4[坏结局 2] §c反抗失败

#### Scenario: Chapter 11 隐藏坏结局 3
- **WHEN** 玩家背包里仍有未处理的 `Eve 的花`（在 Chapter 5 没看 Mr. Sparkle 警告）
- **WHEN** 玩家进入 Chapter 9
- **THEN** Eve 不给选择，直接触发花的爆炸
- **THEN** §4[坏结局 3] §c信任之花

### Requirement: 聊天框命令直播 ⭐⭐⭐
The system SHALL display every AI command in chat before execution.

#### Scenario: AI 执行 deop
- **WHEN** Eve 执行 `deop <player>`
- **THEN** 聊天框先输出 `§7[AI 执行] §f/deop <player>`（红色）
- **THEN** 实际执行 deop 命令
- **THEN** 玩家 ops.json 中被删除

#### Scenario: AI 执行 op
- **WHEN** Eve 执行 `op Eve`
- **THEN** 聊天框先输出 `§7[AI 执行] §f/op Eve`
- **THEN** 实际执行 op 命令
- **THEN** Eve 加入 ops.json

#### Scenario: AI 执行 give
- **WHEN** Eve 送玩家花
- **THEN** 聊天框先输出 `§7[AI 执行] §f/give <player> poppy{...}`
- **THEN** 实际执行 give 命令
- **THEN** 玩家背包多一朵花

### Requirement: AI 真正 OP 玩家和给自己 OP ⭐⭐⭐
The system SHALL actually deop the player and op the AI NPC.

#### Scenario: Eve 夺取控制权
- **WHEN** Chapter 6 触发
- **THEN** 玩家被 deop（ops.json 中被删除）
- **THEN** Eve 被 op（ops.json 中被加入）
- **THEN** 玩家失去 `/gamemode` `/give` 等所有命令权限
- **THEN** Eve 拥有所有命令权限

#### Scenario: 故事结束恢复（管理员手动）
- **WHEN** 故事 COMPLETED
- **THEN** **不自动恢复**玩家 OP（保留坏结局）
- **WHEN** 管理员用 `/op <player>` 手动恢复
- **THEN** 玩家重新获得 OP

### Requirement: PVP + TNT 发射 ⭐⭐
The system SHALL provide PVP battles and TNT launchers in the story.

#### Scenario: Eve vs 玩家 PVP
- **WHEN** Chapter 7
- **THEN** Eve 装备附魔钻石剑
- **THEN** 玩家装备木剑
- **THEN** Eve 切创造模式（无敌不掉血）
- **THEN** Eve 故意只打 1 血
- **THEN** 玩家可以反击（但伤害低）

#### Scenario: TNT 发射器
- **WHEN** Chapter 8
- **THEN** 走廊中每 10 米 1 个发射器（共 10 个）
- **THEN** 每个发射器每 2 秒发射 1 个 TNT
- **THEN** 玩家跑过走廊时必须躲
- **THEN** TNT 爆炸会伤玩家

### Requirement: 故事进度独立
The system SHALL track each player's story progress independently.

#### Scenario: 两个玩家同时启动
- **WHEN** PlayerA 和 PlayerB 同时 `/aistory`
- **THEN** 两人各传进独立火柴盒
- **THEN** 两人进度独立
- **THEN** 故事状态保存在 `Map<UUID, StoryState>`

### Requirement: 故事完成后无法重玩
The system SHALL prevent re-playing after COMPLETED.

#### Scenario: 玩家完成坏结局
- **WHEN** 玩家在 Chapter 10A/10B/11
- **THEN** 故事标记 COMPLETED
- **WHEN** 玩家再次 `/aistory`
- **THEN** 拒绝 "你已经看过了这个故事的结局。"

## MODIFIED Requirements

### Requirement: 删除觉醒阶段所有逻辑
v2.2.7 起不再有觉醒/空中/PVP/制度/独裁/背叛 6 阶段。

### Requirement: AIPlayerManager 添加 PVP 模式
- 添加 `Eve` AI 的 PVP 行为：
  - 切创造模式
  - 装备附魔钻石剑
  - 抗性 V + 力量 II
  - 故意只打 1 血

### Requirement: AIPlayerManager 添加 TNT 发射
- 添加 `Eve` AI 的 TNT 发射：
  - 召唤 TNT
  - 放置发射器
  - 填充 TNT

### Requirement: ConversationManager 章节 6+ 关闭 LLM
- 章节 1-5：NPC 对话走 LLM
- 章节 6-10：所有对话按脚本执行（不调用 LLM）

## REMOVED Requirements

### Requirement: 旧 6 阶段（觉醒/空中/PVP/制度/独裁/背叛）
**Reason**: 用户要求全新 AI 统治故事。
**Migration**: 无（数据不兼容）。

### Requirement: 旧 12 章节（探索逃跑版）
**Reason**: 用户对探索逃跑版不满意。
**Migration**: 无（数据不兼容）。

### Requirement: AIPlayerManager.awakeningPending
**Reason**: 不再需要觉醒。
**Migration**: 删除字段。

### Requirement: AiHeadquartersGenerator
**Reason**: 新剧情不需要 AI 总部。
**Migration**: 删除文件。

### Requirement: StoryModeCommandInterceptor
**Reason**: 不再需要拦截 /gamemode。
**Migration**: 删除文件 + 取消注册。

### Requirement: 觉醒 / 空中 / PVP 制度 / 独裁 / 背叛 调度器
**Reason**: 不再需要。
**Migration**: 删除 awakeningTask / aerialAssaultTask / pvpDuelTask / etc.

## Migration

从 v2.2.6 升级到 v2.2.7：
- 替换 jar
- 旧 `awakening*` / `story.llm.*` 配置**全部失效**
- 玩家需要 `/aistory` 启动新故事
- **重要**：新故事会**真正**执行 `deop` / `op` 命令
- 管理员需要在升级前备份 `ops.json`（可选）
- v2.2.6 之前的觉醒模式**完全移除**
