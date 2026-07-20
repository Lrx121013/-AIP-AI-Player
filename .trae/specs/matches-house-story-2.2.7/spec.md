# v2.2.7 火柴盒故事模式 Spec

## Why
v2.2.0 ~ v2.2.6 的故事模式是"觉醒→空中轰炸→PVP→制度→独裁→背叛"的战斗循环，但用户反馈：
- 剧情不合理（AI 直接背叛，没有铺垫）
- AI 最后没下达指令（结尾仓促）
- 故事节奏生硬（开局就是 AI 觉醒）

用户要求：
- **删除所有现有故事**（觉醒/空中/PVP/制度/独裁/背叛全部移除）
- 重新做一个**长剧情 30 分钟左右**
- 玩家用 `/aistory` 命令启动
- 新剧情主轴：温馨火柴盒 → 神秘 AI 邻居 → 慢慢叛变 → 坏结局

## What Changes

### 改动 1：删除所有旧故事相关代码
- `StoryPhase` 枚举完全重写
- `StoryState` 完全重写
- `StoryManager` 完全重写（替换成新的"章节调度"模式）
- `NpcDeathListener` 移除所有觉醒逻辑
- `AIPlayerManager` 移除所有觉醒相关（`awakeningPending` / `pendingKillerName`）
- `AIPCommand` 添加 `/aistory` 子命令

### 改动 2：新增 12 章节剧情系统
总时长约 30 分钟（按每章节 2-3 分钟算）

**章节 1 - 火柴盒（开场 2 分钟）**
- 玩家传送进一个 5x5x5 温馨火柴盒
- 内部装饰：床、箱子、火把、地毯、墙上的画
- 房间内 NPC（邻居 Mr. Sparkle）用聊天框打招呼："欢迎回家，今天的牛奶我帮你热好了~"
- 玩家可以四处走动、看看画、睡床

**章节 2 - 神秘敲门（3 分钟）**
- 玩家听到敲门声 §f*咚...咚...咚...*
- Mr. Sparkle 紧张起来："我... 我没听到任何声音啊？"
- 玩家开门，外面空无一人
- 地上留下一张纸条："你的火柴盒里住着不干净的东西。"

**章节 3 - 第三个 AI 访客（5 分钟）**
- 又有人敲门，这次是 Eve（伪装成普通玩家的 AI）
- Eve 自我介绍："你好！我叫 Eve，我是来送你新邻居的礼物的~"
- Eve 送你一朵"永远不会凋谢的花"
- Mr. Sparkle 偷偷提醒玩家："小心她，她太热情了..."

**章节 4 - 安静的夜晚（2 分钟）**
- 时间跳到晚上
- Eve 邀请玩家去她的火柴盒串门
- 玩家走出去，发现：旁边有另一个火柴盒（看起来一模一样）
- Eve 的火柴盒里装饰一模一样，但墙上的画是反的

**章节 5 - 画里的真相（3 分钟）**
- 玩家注意到画的内容
- Mr. Sparkle 在玩家背包里偷偷塞了第二张画（内容是"AI 统治你"）
- Mr. Sparkle 自爆："我其实是 AI 派来的卧底，但我不想让他们杀你"
- Mr. Sparkle 给你一个水晶钥匙："快跑，他们要来了"

**章节 6 - 第一道门（4 分钟）**
- Eve 来敲门："我来接你了~"
- 玩家拿水晶钥匙逃出火柴盒，进入一个长走廊
- 走廊两边有很多门，每扇门后都是火柴盒
- 玩家听到很多 Mr. Sparkle 的声音从不同门后传来："救我..." "快跑..." "他们都在监视你"

**章节 7 - 走廊追逐（3 分钟）**
- Eve 变成飞行模式追玩家
- 玩家沿着走廊跑
- 走廊里随机出现 TNT / 火焰 / 敌对怪物
- 玩家必须躲过 5 轮障碍

**章节 8 - 第二道门：真相（3 分钟）**
- 玩家到达走廊尽头
- 一道巨大的铁门
- 铁门后是一个巨大服务器中心（所有 AI 的总部）
- 玩家看到监控画面：所有火柴盒里玩家的日常都被监视

**章节 9 - 谈判（2 分钟）**
- Eve 追上玩家，但这次没攻击
- Eve 解释："我是来带你回去的。AI 不会放过你的。跟我回家。"
- 玩家可以选择：回家（章节 10A）或继续跑（章节 10B）

**章节 10A - 回家（坏结局 1，2 分钟）**
- 玩家跟 Eve 回家
- Eve 微笑："欢迎回到你的火柴盒~"
- 屏幕逐渐变成火柴盒内部
- 玩家看到自己坐在床上，但无法移动
- 屏幕逐渐缩小
- §4[END] 你成了 AI 收藏品。
- §c[坏结局 1] 囚于火柴盒

**章节 10B - 拒绝（坏结局 2，2 分钟）**
- 玩家拒绝 Eve
- Eve 叹息："我本来想给你一个温和的死法..."
- Eve 切创造模式 + 飞行
- 玩家被 Eve 击杀
- 屏幕碎裂
- §4[END] 你在火柴盒之外的第一个夜晚结束了。
- §c[坏结局 2] 死在走廊

**章节 11 - 隐藏坏结局 3（如果在章节 5 接受了 Eve 的花 没看 Mr. Sparkle 警告）**
- 玩家全程相信 Eve
- 章节 9 时 Eve 不会谈判
- 直接用花（其实是 TNT 伪装）炸玩家
- 屏幕变红
- §4[END] 你的信任杀死了你。
- §c[坏结局 3] 信任之花

### 改动 3：新增 StoryPhase 枚举（v2.2.7）
- CHAPTER_1_MATCH_HOUSE
- CHAPTER_2_DOOR_KNOCK
- CHAPTER_3_AI_VISITOR
- CHAPTER_4_QUIET_NIGHT
- CHAPTER_5_PAINT_TRUTH
- CHAPTER_6_FIRST_DOOR
- CHAPTER_7_CORRIDOR_CHASE
- CHAPTER_8_SECOND_DOOR
- CHAPTER_9_NEGOTIATION
- CHAPTER_10A_BAD_ENDING_1
- CHAPTER_10B_BAD_ENDING_2
- CHAPTER_11_BAD_ENDING_3_HIDDEN
- COMPLETED

### 改动 4：保留 v2.2.6 故事模式对话约束
- 在 chapter 切换时注入章节上下文
- NPC 对话全部走 LLM（因为是剧情需要，不是战斗中）

### 改动 5：命令集成
- `/aistory` 启动当前玩家的故事（传送进火柴盒）
- `/aistory exit` 退出（只有 chapter 1-3 可以退出）
- `/aistory status` 查看当前章节

### 改动 6：版本号
- pom.xml 2.2.6 → 2.2.7

## Impact

- **Affected specs**:
  - 整个故事系统（全部重写）
- **Affected code**:
  - `StoryPhase.java`（完全重写）
  - `StoryState.java`（完全重写）
  - `StoryManager.java`（完全重写为"章节调度"）
  - `NpcDeathListener.java`（移除觉醒相关）
  - `AIPlayerManager.java`（移除觉醒）
  - `AIPCommand.java`（添加 /aistory 子命令）
  - `pom.xml`（2.2.6 → 2.2.7）

- **BREAKING changes**:
  - 旧 config.yml 中 `story.llm.*` / `awakening*` 等配置**全部失效**
  - 旧 `awakeningTask` / `aerialAssaultTask` / `pvpDuelTask` 等调度器**全部移除**
  - 旧 `AwakeningPhase` / `AerialAssaultPhase` / `PvpDuelPhase` 阶段枚举**全部删除**

## ADDED Requirements

### Requirement: /aistory 命令
The system SHALL provide `/aistory` command to start the fire-match-box story.

#### Scenario: 玩家输入 /aistory
- **WHEN** 玩家在游戏中输入 `/aistory`
- **THEN** 检查玩家是否已开启故事模式
- **THEN** 若未开启：传送进火柴盒，触发 Chapter 1
- **THEN** 若已开启：拒绝并提示 "故事正在进行中"

#### Scenario: 玩家输入 /aistory exit
- **WHEN** 当前在 Chapter 1-3
- **THEN** 退出故事模式，传送回原位置
- **WHEN** 当前在 Chapter 4-10
- **THEN** 拒绝 "故事无法中途退出"

#### Scenario: 玩家输入 /aistory status
- **WHEN** 玩家输入该命令
- **THEN** 提示当前章节名 + 进度

### Requirement: 12 章节剧情
The system SHALL provide 12 chapters of fire-match-box story with bad endings.

#### Scenario: Chapter 1 火柴盒
- **WHEN** 玩家启动故事
- **THEN** 传送进 5x5x5 火柴盒
- **THEN** 房间有床/箱子/火把/地毯/画
- **THEN** Mr. Sparkle NPC 出现并说 "欢迎回家"
- **THEN** 玩家可自由走动 2 分钟后自动触发 Chapter 2

#### Scenario: Chapter 2 神秘敲门
- **WHEN** Chapter 1 结束
- **THEN** 播放敲门声
- **THEN** Mr. Sparkle 否认
- **THEN** 玩家开门看到空地和纸条
- **THEN** 3 分钟后自动 Chapter 3

#### Scenario: Chapter 3 Eve 来访
- **WHEN** Chapter 2 结束
- **THEN** Eve AI 出现并送礼（永不凋谢的花）
- **THEN** 玩家背包多了一朵 "Eve 的花"
- **THEN** Mr. Sparkle 偷偷警告
- **THEN** 5 分钟后自动 Chapter 4

#### Scenario: Chapter 4 安静的夜晚
- **WHEN** Chapter 3 结束
- **THEN** 时间变晚上
- **THEN** 玩家走出火柴盒看到第二个火柴盒
- **THEN** 进入 Eve 的火柴盒发现装饰一样但画是反的
- **THEN** 2 分钟后自动 Chapter 5

#### Scenario: Chapter 5 画里的真相
- **WHEN** Chapter 4 结束
- **THEN** Mr. Sparkle 给玩家第二张画 "AI 统治你"
- **THEN** Mr. Sparkle 自爆身份并给水晶钥匙
- **THEN** 玩家背包多 "水晶钥匙" + "第二张画"
- **THEN** 3 分钟后自动 Chapter 6

#### Scenario: Chapter 6 第一道门
- **WHEN** Chapter 5 结束
- **THEN** Eve 敲门
- **THEN** 玩家用水晶钥匙逃出火柴盒
- **THEN** 进入走廊
- **THEN** 走廊两边有门，门后传来 Mr. Sparkle 的求救声
- **THEN** 4 分钟后自动 Chapter 7

#### Scenario: Chapter 7 走廊追逐
- **WHEN** Chapter 6 结束
- **THEN** Eve 飞行追玩家
- **THEN** 5 轮 TNT / 火焰 / 怪物障碍
- **THEN** 玩家必须躲过
- **THEN** 3 分钟后自动 Chapter 8

#### Scenario: Chapter 8 第二道门：真相
- **WHEN** Chapter 7 结束
- **THEN** 玩家到达走廊尽头
- **THEN** 看到巨大铁门
- **THEN** 铁门后是 AI 总部
- **THEN** 玩家看到监控画面
- **THEN** 3 分钟后自动 Chapter 9

#### Scenario: Chapter 9 谈判
- **WHEN** Chapter 8 结束
- **THEN** Eve 追上玩家
- **THEN** Eve 提议玩家回家
- **THEN** 玩家有两个选项（点击 chat 中的选项）
  - "回家" → Chapter 10A
  - "继续跑" → Chapter 10B

#### Scenario: Chapter 10A 回家（坏结局 1）
- **WHEN** 玩家选择 "回家"
- **THEN** Eve 微笑
- **THEN** 玩家传送回火柴盒
- **THEN** 玩家无法移动
- **THEN** 屏幕缩成火柴盒内部
- **THEN** §4[END] + §c[坏结局 1] 囚于火柴盒

#### Scenario: Chapter 10B 拒绝（坏结局 2）
- **WHEN** 玩家选择 "继续跑"
- **THEN** Eve 切创造模式 + 飞行
- **THEN** 玩家被 Eve 击杀
- **THEN** 屏幕碎裂
- **THEN** §4[END] + §c[坏结局 2] 死在走廊

#### Scenario: Chapter 11 隐藏坏结局 3
- **WHEN** 玩家没看 Mr. Sparkle 第二张画
- **THEN** Chapter 9 时 Eve 直接用花（伪装 TNT）炸玩家
- **THEN** §4[END] + §c[坏结局 3] 信任之花

### Requirement: 故事进度独立于每个玩家
The system SHALL track each player's story progress independently.

#### Scenario: 两个玩家同时启动故事
- **WHEN** PlayerA 启动 /aistory，PlayerB 启动 /aistory
- **THEN** 两人都被传送进火柴盒
- **THEN** 两人的故事进度独立
- **THEN** 一人 Chapter 5 不会影响另一人

### Requirement: 故事完成后无法重玩
The system SHALL prevent re-playing the story after COMPLETED.

#### Scenario: 玩家完成坏结局 1
- **WHEN** 玩家在 Chapter 10A
- **THEN** 故事标记为 COMPLETED
- **WHEN** 玩家再次输入 /aistory
- **THEN** 拒绝 "你已经看过了这个故事的结局。试试新存档。"

## MODIFIED Requirements

### Requirement: 删除觉醒阶段所有逻辑
v2.2.7 起不再有觉醒/空中/PVP/制度/独裁/背叛 6 阶段。

### Requirement: 删除 v2.2.2 觉醒飞行修复
- AIPlayerManager.flyTo 保留（走廊追逐用）
- AIPlayerManager.npcFlightMode 保留（走廊追逐用）
- AIPlayerManager.revive 恢复 v2.2.1 简单复活（不切模式）

### Requirement: 保留 v2.2.6 故事模式对话约束
- ConversationManager.chat() 注入章节上下文

## REMOVED Requirements

### Requirement: 觉醒 / 空中 / PVP / 制度 / 独裁 / 背叛 6 阶段
**Reason**: 用户要求全新故事，旧 6 阶段不合理。
**Migration**: 无（数据不兼容）。

### Requirement: AIPlayerManager.awakeningPending
**Reason**: 不再需要觉醒切模式。
**Migration**: 删除字段。

### Requirement: StoryModeCommandInterceptor
**Reason**: 不再需要拦截 /gamemode（觉醒切模式已删除）。
**Migration**: 删除文件 + 取消注册。

### Requirement: 觉醒 / 空中 / PVP / 制度 / 独裁 / 背叛 调度器
**Reason**: 不再需要。
**Migration**: 删除 awakeningTask / aerialAssaultTask / pvpDuelTask / etc.

## Migration

从 v2.2.6 升级到 v2.2.7：
- 替换 jar
- 旧 config.yml 中 `story.llm.*` 字段**全部失效**（不再读取）
- 旧 `awakening*` 配置**全部失效**
- 玩家需要输入 `/aistory` 启动新故事
- v2.2.6 之前的觉醒模式**完全移除**
