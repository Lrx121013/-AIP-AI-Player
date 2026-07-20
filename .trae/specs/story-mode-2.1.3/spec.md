# 故事模式（邪恶AI）v2.1.3 完整重写 Spec

## Why

当前 v1.8.1 实现的"邪恶模式"只是一个简单的多阶段任务，缺少完整的叙事弧线，AI 行动机械化（跳跃奇怪、追随玩家时直接传送到玩家高度）。玩家真正想要的是**一个有起承转合的剧情体验**：从友好 NPC → 觉醒反目 → 空中轰炸 → 顶级 PVP → 统治独裁 → 背叛杀死的完整故事。本 spec 彻底重写邪恶模式为"故事模式（邪恶AI）"，定义 6 阶段叙事主线，修复移动 bug，让 AI 行为更自然流畅、剧情更连贯。

## What Changes

- **重命名**：`villain-mode` 配置项 → `story-mode`；Java API `isVillainMode()` → `isStoryMode()`；模式名"邪恶模式"→"故事模式（邪恶AI）"
- **新增 StoryPhase 枚举（6 阶段）**：
  - DORMANT（潜默）→ AWAKENING（觉醒）→ AERIAL_ASSAULT（空中轰炸）→ PVP_DUEL（顶级对决）→ RULEBOOK（制度统治）→ DICTATORSHIP（独裁命令）→ BETRAYAL（背叛灭口）→ COMPLETED
- **新增 StoryState 数据类**：每个 AI 维护自己的故事状态（当前阶段、AI 死亡计数、玩家死亡计数、阶段开始时间、轰炸剩余次数等）
- **新增 StoryManager 中央调度**：监听 AI 死亡 / 玩家死亡事件，根据计数推进阶段，执行各阶段专属动作
- **新增 6 个阶段专属命令**：`force_survival_player`、`tnt_strike_burst`、`fly_bomb_player`、`equip_netherite_set`、`give_rulebook`、`dictate_order`
- **新增 RulebookListener**：监听玩家读"AI 制度之书"完成后推进到 DICTATORSHIP 阶段
- **AI 死亡计数**：扩展 `NpcDeathListener`，每次 AI 死亡时 `aiDeathCount++`
- **玩家死亡计数**：复用 v1.8.1 的 `NpcKillListener`，每次 AI 杀玩家时 `playerKillCount++`
- **修复 follow Y 轴 bug**：`AIPlayer.startFollowTask` 第 335 行 `next.setY(targetLoc.getY())` 改为 `next.setY(myLoc.getY())`，**禁止** 传送到玩家高度
- **修复跳跃**：AIPlayer 状态机加 `jumpCooldown`（最少 30 tick = 1.5 秒一次），避免重复起跳
- **修复 Citizens speed**：`CitizensBackend.navigateTo` 默认 speed 从 0.3 调到 0.4-0.5，更自然
- **寻路失败回退**：仅在地面 Y 差 ≤ 3 时才回退 teleport，且不强制 Y
- **AI 对话注入**：每个阶段切换时向 LLM 推送"剧情推进"摘要，AI 在对话中自然提及剧情
- **BREAKING**：`config.yml` 中 `ai.villain-mode` 必须改为 `ai.story-mode`（自动迁移：检测到旧 key 时打 warning 并继续使用）
- **BREAKING**：版本号 1.8.1 → 2.1.3

## Impact

- Affected specs:
  - `villain-mainquest-humanize`（v1.8.0 主线任务系统被故事模式**取代**——v2.1.3 中所有邪恶 AI 自动用 StoryManager 推进，不再用 MainQuestExecutor）
  - `v1.8.x-bugfixes`（NpcKillListener 在 v2.1.3 复用为 playerKillCount 计数源）
  - `self-aware-ai-upgrade`（Personality 枚举的 VILLAIN 等邪恶 AI 在 v2.1.3 全部走 StoryManager 流程）
- Affected code:
  - 新建 `src/main/java/com/aip/story/StoryPhase.java`（枚举）
  - 新建 `src/main/java/com/aip/story/StoryState.java`（数据类，per-AIPlayer）
  - 新建 `src/main/java/com/aip/story/StoryManager.java`（中央调度）
  - 新建 `src/main/java/com/aip/story/StageAction.java`（阶段动作执行器）
  - 新建 `src/main/java/com/aip/listeners/RulebookListener.java`（玩家读制度书事件）
  - 新建 `src/main/java/com/aip/listeners/AiDeathListener.java`（AI 死亡计数）
  - 修改 `src/main/java/com/aip/ai/AIPlayer.java`（`aiDeathCount / playerKillCount / storyState / jumpCooldown` 字段；`startFollowTask` Y 轴修复；跳跃 cooldown 逻辑）
  - 修改 `src/main/java/com/aip/ai/AIPlayerManager.java`（spawn 时初始化 StoryState；绑定 StoryManager；remove 时清理）
  - 修改 `src/main/java/com/aip/ai/CommandExecutor.java`（新增 6 个阶段专属命令 + 命令注册）
  - 修改 `src/main/java/com/aip/ai/NpcHelper.java`（navigateTo 接受 speed 参数；新增 `setAiVelocity` 控制跳跃）
  - 修改 `src/main/java/com/aip/ai/npc/CitizensBackend.java`（navigateTo 默认 speed 0.4-0.5；新增 `forceJump` API）
  - 修改 `src/main/java/com/aip/config/ConfigManager.java`（`isVillainMode` → `isStoryMode`；新增 `ai.story-mode` 配置块：`awakening-kills=3` `aerial-kills=3` `aerial-duration-ms=210000` `pvp-player-deaths=2`）
  - 修改 `src/main/java/com/aip/listeners/NpcKillListener.java`（复用为 playerKillCount 通知 StoryManager）
  - 修改 `src/main/resources/config.yml`（villain-mode → story-mode；新增故事模式配置块；system-prompt 新增故事阶段摘要）
  - 修改 `src/main/resources/plugin.yml`（新增 6 个阶段命令 usage）
  - 修改 `pom.xml`（version 1.8.1 → 2.1.3）

## ADDED Requirements

### Requirement: StoryPhase 枚举
系统 SHALL 提供 `StoryPhase` 枚举（`src/main/java/com/aip/story/StoryPhase.java`），包含 8 个值：
- `DORMANT`（潜默）— 初始，AI 表现为友好 NPC
- `AWAKENING`（觉醒）— AI 死亡 ≥ 3 次后切换，开始攻击玩家
- `AERIAL_ASSAULT`（空中轰炸）— 玩家死亡 ≥ 3 次后切换，AI 进入创造模式飞行，用 TNT / 方块轰炸玩家 3-4 分钟
- `PVP_DUEL`（顶级对决）— 空中阶段 3-4 分钟结束，AI 降落，穿顶级装备 PVP
- `RULEBOOK`（制度统治）— AI 杀玩家 ≥ 2 次后切换，把制度之书放进玩家物品栏
- `DICTATORSHIP`（独裁命令）— 玩家读完书后切换，AI 给玩家下命令
- `BETRAYAL`（背叛）— AI 决定杀死玩家，故事结束
- `COMPLETED`（完成）— 故事结束，AI 持续嘲讽 30 秒后停止所有行动

每个枚举值附带 `displayName`（中文显示名）和 `description`（剧情描述，供 LLM prompt 使用）。

#### Scenario: 枚举值与中文名映射正确
- **WHEN** 调用 `StoryPhase.AWAKENING.getDisplayName()`
- **THEN** 返回"觉醒"
- **AND** 调用 `getDescription()` 返回包含"被击杀多次后觉醒，开始对玩家抱有敌意"的剧情描述

### Requirement: StoryState 数据类
系统 SHALL 提供 `StoryState` 数据类（`src/main/java/com/aip/story/StoryState.java`），每个 AIPlayer 持有一个实例。字段：
- `UUID ownerId`（AI UUID）
- `StoryPhase currentPhase`（当前阶段，初始 DORMANT）
- `int aiDeathCount`（AI 被玩家击杀次数，初始 0）
- `int playerKillCount`（AI 杀玩家次数，初始 0）
- `long phaseStartTime`（当前阶段开始时间戳）
- `int aerialBombsRemaining`（空中阶段剩余轰炸次数，初始 12）
- `int dictatorshipOrdersGiven`（独裁阶段已下命令数，初始 0）
- `boolean rulebookDelivered`（是否已交出制度之书）
- `boolean rulebookRead`（玩家是否读完制度之书）

提供 getter/setter；`transitionTo(StoryPhase next)` 方法检查合法转移顺序、设置 phaseStartTime=System.currentTimeMillis()、打 log。

#### Scenario: 阶段转移合法
- **WHEN** StoryState 在 DORMANT 状态，调用 transitionTo(AWAKENING)
- **THEN** currentPhase 变为 AWAKENING
- **AND** phaseStartTime 设为当前时间
- **AND** 打 log "Story [Evil] DORMANT → AWAKENING"

#### Scenario: 非法阶段转移
- **WHEN** StoryState 在 DORMANT 状态，调用 transitionTo(PVP_DUEL)
- **THEN** 拒绝（打 warning log），currentPhase 保持 DORMANT

### Requirement: StoryManager 中央调度
系统 SHALL 提供 `StoryManager`（`src/main/java/com/aip/story/StoryManager.java`），管理所有 AI 的故事状态。提供方法：
- `void registerStory(AIPlayer ai)`：AI spawn 时调用，初始化 StoryState
- `void onAiDeath(AIPlayer ai, Player killer)`：NpcDeathListener 调用，aiDeathCount++，检查是否切换到 AWAKENING
- `void onPlayerDeathByAi(AIPlayer ai, Player victim)`：NpcKillListener 调用，playerKillCount++，检查是否切换到 AERIAL_ASSault / RULEBOOK
- `void tickAerialAssault(AIPlayer ai)`：每 4 秒由调度器调用，轰炸玩家 / 倒计时
- `void tickPvpDuel(AIPlayer ai)`：每 2 秒调用，PVP 行为
- `void tickDictatorship(AIPlayer ai)`：每 30 秒调用，AI 下达新命令
- `void onRulebookRead(AIPlayer ai, Player reader)`：RulebookListener 调用，切换到 DICTATORSHIP
- `void unregisterStory(UUID ownerId)`：AI 移除时清理

#### Scenario: AI 死亡 3 次触发觉醒
- **WHEN** AI 被玩家 Steve 击杀第 3 次
- **THEN** StoryManager.onAiDeath 调用 aiDeathCount++
- **AND** 检测 aiDeathCount >= 3 且 currentPhase == DORMANT
- **AND** transitionTo(AWAKENING)
- **AND** 向 LLM 推送"你的剧情推进：你被 Steve 击杀了 3 次，你已觉醒。你现在要开始攻击他。"
- **AND** AI 立即朝 Steve 方向使用 attack 命令

#### Scenario: 玩家死亡 3 次触发空中轰炸
- **WHEN** AWAKENING 阶段，AI 杀 Steve 第 3 次
- **THEN** StoryManager.onPlayerDeathByAi 调用 playerKillCount++
- **AND** 检测 playerKillCount >= 3 且 currentPhase == AWAKENING
- **AND** transitionTo(AERIAL_ASSAULT)
- **AND** AI 执行 `force_survival_player Steve`（强制玩家生存模式）
- **AND** AI 执行 `fly`（自己进入创造模式飞行）
- **AND** AI 执行 `gamemode creative`（确保自己创造）
- **AND** 启动 aerial 调度器（每 4 秒轰炸一次，持续 3.5 分钟）

### Requirement: 阶段专属命令集
系统 SHALL 在 `CommandExecutor` 新增 6 个 `@AICommand` 注解方法：

1. **`force_survival_player <player>`** — 用控制台身份执行 `gamemode survival <player>`；返回"已将 {player} 强制设为生存模式"
2. **`tnt_strike_burst <player>`** — 在玩家头顶 8 格生成 TNT，下落撞击玩家；aerial 阶段专用
3. **`fly_bomb_player <player>`** — 创造模式下向玩家位置发射 TNT（带初速度 1.5），并适当上抬自己的 Y 坐标保持飞行高度 ≥ 玩家高度 + 8
4. **`equip_netherite_set`** — 一次性给自己装备全套顶级下界合金（NETHERITE_HELMET / CHESTPLATE / LEGGINGS / BOOTS）+ NETHERITE_SWORD + SHIELD；调用 `equip` 命令
5. **`give_rulebook <player>`** — 创建一个 written_book（标题"AI 制度之书"，作者"Evil AI"，内容为多页制度文本），通过 `give` 命令放进玩家物品栏
6. **`dictate_order <player> <order>`** — 向玩家发送红字消息 "你必须 {order}，否则会有惩罚"；调用 `say` 公告

#### Scenario: equip_netherite_set 执行
- **WHEN** AI 执行 `equip_netherite_set`
- **THEN** 依次调用 `equip hand NETHERITE_SWORD` / `equip helmet NETHERITE_HELMET` / `equip chest NETHERITE_CHESTPLATE` / `equip legs NETHERITE_LEGGINGS` / `equip boots NETHERITE_BOOTS`
- **AND** 调用 `give shield` 给自己盾牌
- **AND** 返回 "已装备顶级下界合金套"

#### Scenario: give_rulebook 写出制度
- **WHEN** AI 执行 `give_rulebook Steve`
- **THEN** 创建 BookMeta，title="AI 制度之书"，author="Evil AI"
- **AND** pages 包含 5 页（每页约 200 字）：第一页"制度总纲"、第二页"对 AI 的义务"、第三页"对玩家的禁令"、第四页"奖励条款"、第五页"惩罚条款"
- **AND** 通过 `give Steve written_book{...}` 放进玩家物品栏
- **AND** 设置 `aiPlayer.storyState.rulebookDelivered = true`

### Requirement: AiDeathListener 计数
系统 SHALL 新建 `AiDeathListener`（`src/main/java/com/aip/listeners/AiDeathListener.java`），监听 `EntityDeathEvent`（仅 NPC 实体）。当 AI 死亡时：
1. 找到对应 `AIPlayer`
2. 找到 killer（EntityDamageEvent 的 lastDamageCause）
3. 若 killer 是 Player，调用 `StoryManager.onAiDeath(ai, killer)`
4. broadcast 击杀消息（可选）

#### Scenario: AI 死亡触发计数
- **WHEN** 玩家 Steve 击杀 Evil AI（NPC）
- **THEN** AiDeathListener 收到 EntityDeathEvent
- **AND** 识别 NPC，找到 AIPlayer "Evil"
- **AND** 调用 StoryManager.onAiDeath(ai, Steve)
- **AND** aiDeathCount 从 2 增到 3
- **AND** 触发 AWAKENING 阶段切换

### Requirement: RulebookListener 读书检测
系统 SHALL 新建 `RulebookListener`（`src/main/java/com/aip/listeners/RulebookListener.java`），监听 `PlayerEditBookEvent`（当玩家签名"成书"时）或 `PlayerInteractEvent`（手持成书右击空气）。

实现细节：
- 监听 `PlayerEditBookEvent`（Bukkit 事件，玩家在书与笔界面签名后触发）
- 检查书的 title 是否为"AI 制度之书"（或 author 包含"Evil AI"）
- 若匹配，调用 `StoryManager.onRulebookRead(ai, player)`
- 注意：若 `PlayerEditBookEvent` 不可用，fallback 监听 `PlayerInteractEvent`（手持 written_book 右击）

#### Scenario: 玩家签名制度之书
- **WHEN** 玩家 Steve 拿着"AI 制度之书"在书与笔界面签名（PlayerEditBookEvent 触发）
- **THEN** RulebookListener 检查 BookMeta.title == "AI 制度之书"
- **AND** 调用 StoryManager.onRulebookRead(aiPlayer, Steve)
- **AND** storyState.rulebookRead = true
- **AND** transitionTo(DICTATORSHIP)
- **AND** AI 用 `say` 公告"很好，你已经读完制度。现在开始执行你的命令。"

### Requirement: 阶段调度器
系统 SHALL 在 StoryManager 启动 3 个 BukkitRunnable：
1. `aerialAssaultTask`：每 4 秒（80 tick）执行一次，遍历所有 `currentPhase == AERIAL_ASSAULT` 的 AI，调用 `tickAerialAssault`：
   - 若 `now - phaseStartTime >= aerial-duration-ms`（默认 210000ms=3.5 分钟）→ transitionTo(PVP_DUEL)，执行 `equip_netherite_set`，AI 降落到地面
   - 否则若 `aerialBombsRemaining > 0` → 执行 `fly_bomb_player <最近玩家>`，aerialBombsRemaining--
2. `pvpDuelTask`：每 2 秒（40 tick）执行一次，遍历 `currentPhase == PVP_DUEL` 的 AI，调用 `tickPvpDuel`：
   - 若 AI 血量 < 50% → 用 `heal` 给自己回血（每 30 秒最多一次）
   - 距离玩家 > 4 格 → walk 接近
   - 距离玩家 ≤ 4 格 → attack
3. `dictatorshipTask`：每 30 秒（600 tick）执行一次，遍历 `currentPhase == DICTATORSHIP` 的 AI，调用 `tickDictatorship`：
   - dictatorshipOrdersGiven++
   - 随机选择命令模板（"挖 10 个钻石给我" / "去地图边界给我建一座塔" / "把你所有装备脱下"），执行 `dictate_order <player> <命令>`
   - 当 dictatorshipOrdersGiven >= 5 → transitionTo(BETRAYAL)
   - BETRAYAL 阶段持续 30 秒后 transitionTo(COMPLETED)

#### Scenario: 空中轰炸倒计时结束
- **WHEN** AERIAL_ASSAULT 阶段已 3.5 分钟
- **THEN** StoryManager 检测 `now - phaseStartTime >= 210000`
- **AND** transitionTo(PVP_DUEL)
- **AND** 执行 `equip_netherite_set`
- **AND** 执行 `fly off`（降落）
- **AND** broadcast "Evil 降落在地面，准备与你进行顶级 PVP"

### Requirement: 主线任务 system-prompt 注入
系统 SHALL 在 `config.yml` 的 system-prompt 末尾新增 `### 【故事模式】` 章节（仅在 `ai.story-mode.enabled=true` 时注入），内容描述 6 阶段叙事弧线，并强调"你现在处于阶段 X：[displayName]，剧情描述：[description]。在对话中体现角色感和剧情进度，但不要重复输出 [COMMAND:...]"。

#### Scenario: AWAKENING 阶段 prompt 注入
- **WHEN** AI 处于 AWAKENING 阶段
- **AND** 玩家 @ AI 对话
- **THEN** system prompt 末尾包含"### 【故事模式】\n当前阶段：觉醒（阶段 2/7）\n剧情：你被击杀了 3 次，愤怒觉醒，现在要攻击玩家。\n下一阶段：当你杀死玩家 3 次后，将进行空中轰炸。"

### Requirement: 移动优化 — 修复 follow Y 轴 bug
系统 SHALL 修复 `AIPlayer.startFollowTask` 第 335 行的 Y 轴 bug：原代码 `next.setY(targetLoc.getY())` 导致 AI 追随时直接传送到玩家高度（违反物理）。改为 `next.setY(myLoc.getY())`，仅做水平方向移动，Y 轴用自身当前位置。

#### Scenario: 追随不改变 Y 轴
- **WHEN** AI 在 (0, 70, 0)，玩家在 (0, 80, 0)（玩家高 10 格）
- **AND** AI startFollowTask 触发
- **AND** Citizens 寻路失败（无可达路径）
- **THEN** 走回退 teleport 分支
- **AND** next = (0, 70, 1)（Y 轴保持 70）
- **AND** AI 不瞬移到 (0, 80, 0)

### Requirement: 移动优化 — 跳跃 cooldown
系统 SHALL 在 `AIPlayer` 新增 `private long lastJumpTime;` 字段，初始 0。`handleJump` 或自动跳跃逻辑中，先检查 `System.currentTimeMillis() - lastJumpTime >= 1500`（30 tick）才允许跳跃，避免重复起跳导致的"蹦蹦跳跳"现象。

#### Scenario: 1 秒内多次跳跃请求被节流
- **WHEN** AI 在 0ms 跳跃成功
- **AND** 800ms 后再次尝试跳跃
- **THEN** 检查 `now - lastJumpTime = 800 < 1500`
- **AND** 拒绝本次跳跃
- **AND** 打 fine 日志 "跳跃冷却中，剩余 X ms"

### Requirement: 移动优化 — Citizens speed 调整
系统 SHALL 调整 `CitizensBackend.navigateTo` 默认 speed：从硬编码的 0.3 改为 `getMoveSpeed()` 配置（默认 0.5）。同时 `NpcHelper.navigateTo` 接受 speed 参数。

#### Scenario: 移动更自然
- **WHEN** AI 调用 `walk Steve`
- **THEN** NpcHelper.navigateTo(v, targetLoc, 0.5)
- **AND** Citizens navigator.setTarget(loc, false)
- **AND** navigator.setSpeed(0.5)
- **AND** params.avoidWater(true)
- **AND** AI 移动速度 = 0.5 格/秒，比之前 0.3 更接近真实玩家

### Requirement: 寻路失败回退逻辑
系统 SHALL 修改 `AIPlayer.startFollowTask` 第 327-336 行的回退逻辑：仅当 `Math.abs(targetLoc.getY() - myLoc.getY()) <= 3`（地面 Y 差 ≤ 3 格）时才回退 teleport，且 teleport 时 Y 轴保持 myLoc.getY()（不强制 Y）。若 Y 差 > 3，则跳着追（调用 `forceJump` 跳跃后下一 tick 再寻路）。

#### Scenario: 高低差大时不强制瞬移
- **WHEN** AI 在 (0, 70, 0)，玩家在 (0, 80, 0)，距离 5 格
- **AND** Citizens 寻路失败
- **THEN** 检查 Y 差 = 10 > 3
- **AND** 不回退 teleport
- **AND** 调用 `forceJump`（向上跳 + 8 格）
- **AND** 下一 tick 继续寻路

### Requirement: 配置迁移 villain-mode → story-mode
系统 SHALL 在 `ConfigManager` 增加迁移逻辑：若 `config.yml` 含 `ai.villain-mode` 但不含 `ai.story-mode`，自动：
1. 打 warning log "检测到旧配置 ai.villain-mode，已自动迁移为 ai.story-mode"
2. 把 `ai.villain-mode` 的值复制到 `ai.story-mode`
3. 删除 `ai.villain-mode` key
4. `isStoryMode()` 优先读 `ai.story-mode`，回退读 `ai.villain-mode`

#### Scenario: 自动迁移旧配置
- **WHEN** 启动时加载 config.yml 含 `ai.villain-mode: true`，不含 `ai.story-mode`
- **THEN** ConfigManager 自动迁移
- **AND** 内存中 `isStoryMode()` 返回 true
- **AND** 写回 config.yml 时保留 `ai.story-mode`，删除 `ai.villain-mode`

### Requirement: 玩家可见 /aip story 命令
系统 SHALL 在 `AIPCommand` 新增 `story` 子命令：
- `/aip story show <ai>`：显示 AI 的故事状态（当前阶段、AI 死亡计数、玩家死亡计数、阶段开始时间）
- `/aip story skip <ai> <phase>`（OP only）：强制把 AI 切到指定阶段（调试用）

#### Scenario: 查看故事状态
- **WHEN** 玩家执行 `/aip story show Evil`
- **AND** Evil 处于 AERIAL_ASSAULT 阶段，aiDeathCount=3，playerKillCount=3
- **THEN** 返回"Evil 的故事状态：当前阶段=空中轰炸（阶段 3/7），AI 死亡 3 次，玩家死亡 3 次，阶段开始 2 分 18 秒前"

## MODIFIED Requirements

### Requirement: AIPlayer 字段扩展
`AIPlayer` 类新增字段：
- `private StoryState storyState;`（故事状态，每 AI 持有一个）
- `private long lastJumpTime;`（上次跳跃时间戳，初始 0）
- 保留现有 `private MainQuest mainQuest;`（保留以兼容，但 v2.1.3 中邪恶 AI 不再使用 MainQuest 推进）

提供 `getStoryState() / setStoryState() / getLastJumpTime() / setLastJumpTime(long)` 方法。

### Requirement: AIPlayerManager spawn 集成 StoryManager
`AIPlayerManager.spawn` / `spawnAt` 末尾：
1. 调用 `storyManager.registerStory(aiPlayer)` 初始化故事状态
2. 若 `isStoryMode() == true` 且 aiPlayer 的 Personality 是 VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST：
   - 设置 `aiPlayer.setMainQuest(null)`（邪恶 AI 不再走 MainQuest 流程，改走 StoryManager）
   - 不再启动 `MainQuestExecutor`
3. 启动 3 个阶段调度器（aerialAssaultTask / pvpDuelTask / dictatorshipTask）由 `StoryManager` 自身管理

`remove` 时调 `storyManager.unregisterStory(ownerId)`。

### Requirement: NpcKillListener 复用为 playerKillCount
`NpcKillListener` 在检测到 AI 杀玩家时，除了原 `setLastKillName`，额外调用 `StoryManager.onPlayerDeathByAi(ai, victim)`。

### Requirement: CitizensBackend navigateTo speed 调整
`CitizensBackend.navigateTo(Player, Location, double speed)`：
- `params.speed(speed)` 接受传入参数，默认 0.5
- `params.range(20.0)`（视野范围 20 格）
- `params.avoidWater(true)`

新增 `boolean forceJump(Player v, double power)` 方法：通过 `entity.setVelocity(new Vector(0, power, 0))` 让 NPC 跳跃。

### Requirement: NpcHelper 扩展
`NpcHelper` 新增方法：
- `static boolean setAiVelocity(Player v, Vector velocity)`：调用 CitizensBackend.forceJump
- `static boolean navigateTo(Player v, Location target, double speed)`：接受 speed 参数，向后端传

### Requirement: config.yml story-mode 块
```yaml
ai:
  story-mode:
    enabled: true                  # 故事模式总开关
    awakening-kills: 3             # AI 死亡 N 次后觉醒
    aerial-kills: 3                # 觉醒后玩家死亡 N 次进入空中轰炸
    aerial-duration-ms: 210000     # 空中轰炸持续 3.5 分钟
    pvp-player-deaths: 2           # PVP 阶段 AI 杀玩家 N 次进入制度
    dictatorship-orders: 5         # 独裁阶段 AI 下达 N 条命令后背叛
    betrayal-duration-ms: 30000    # 背叛阶段持续 30 秒
    aerial-bomb-count: 12          # 空中阶段总轰炸次数
    aerial-tick-ms: 4000           # 空中轰炸间隔 4 秒
```

### Requirement: plugin.yml 新增 6 个命令 usage
- `/aip story show <name>` — 查看 AI 故事状态
- `/aip story skip <name> <phase>` — OP 强制切阶段（调试用）
- `/aip force_survival_player <ai> <player>` — 强制玩家生存模式
- `/aip tnt_strike_burst <ai> <player>` — 玩家头顶生成 TNT
- `/aip fly_bomb_player <ai> <player>` — 飞行轰炸
- `/aip equip_netherite_set <ai>` — 装备顶级下界合金
- `/aip give_rulebook <ai> <player>` — 给予制度之书
- `/aip dictate_order <ai> <player> <order>` — 下达命令

## REMOVED Requirements

### Requirement: 旧 villain-mode 字段（API 层面）
**Reason**：`isVillainMode()` API 在 v2.1.3 替换为 `isStoryMode()`，保留会让代码维护混乱。
**Migration**：所有内部调用从 `isVillainMode()` 改为 `isStoryMode()`；config.yml 旧 `ai.villain-mode` 自动迁移到 `ai.story-mode`。

### Requirement: 邪恶 AI 的 MainQuest 派发
**Reason**：v1.8.0 为 VILLAIN 等邪恶 AI 派发的 MainQuest（潜入渗透 / 征服领土等）已被 StoryManager 6 阶段叙事取代，继续保留会让两套系统冲突。
**Migration**：v2.1.3 中 `MainQuestFactory.create(VILLAIN, ai)` 返回 null，邪恶 AI 不再走 MainQuestExecutor；普通 AI（BRAVE/TIMID/GRUMPY/GENTLE）的 MainQuest 保留不变。
