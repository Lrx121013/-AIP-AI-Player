# Story Mode Combat Enhance v2.2.0 Spec

## 1. Summary

在 v2.1.4「死亡计数保留 + 复仇对话」的基础上，给故事模式（邪恶 AI）加入**更强的战斗表现力和真实感**，让玩家面对 AI 时产生真实的恐惧。

核心目标：
- AI 行为更像"一个会思考、会说话、会召唤同伴、会用真实引燃 TNT、装备正确的邪恶玩家"
- 死斗 / 空袭 / 制度 / 背叛 阶段的可玩性提升
- 自言自语 + 多说话 + 多做动作，减少"原地不动"和"重复发言"的硬伤
- LLM 回复多样性（不重复）
- 命令执行时通过对话输出"威胁感"台词

## 2. Motivation

### 用户原始反馈
> 1. LLM 必须每次回复都不一样，而且随机十几秒自言自语
> 2. 决战时 AIP 可以自己生成更多 AIP，一起打斗玩家
> 3. AIP 必须发射真实的引燃的 TNT
> 4. AIP 持剑的伤害要和剑匹配，被攻击时，防御也要和装备相同，也可以高一点，只要不容易死就行了
> 5. AIP 要多说话，多做动作
> 6. AIP 执行命令时可以输出到对话框，以让玩家产生真实的恐惧
> 7. AI 在觉醒后要把自己切成创造模式飞起来，把玩家强制切换生存模式

### 现状问题
- **v2.1.3 / v2.1.4** 已实现：AIP 死亡计数、复仇对话、觉醒 / 空中轰炸 / PVP / 制度 / 背叛阶段切换
- **未实现**：
  1. LLM 温度默认 1.0，回复偶尔仍重复；缺乏定时自言自语
  2. AIP 不能自我繁殖（玩家只面对 1 个敌人）
  3. `tnt_strike_burst` 把 TNT 当方块 `setType`，引燃方式是先 set AIR 再 spawnEntity——**不真实**，没有飞行轨迹
  4. `equip_netherite_set` 只换装备，但**不调整实体 Attribute**，所以剑伤 / 防御和下界合金剑 / 护甲的真实值不匹配
  5. autonomous 间隔默认 60s 偏长，LLM 驱动动作偏少
  6. `CommandExecutor.executeWithResult` 立即执行命令再 `aiPlayer.sayInChat(spokenText)`——台词在执行**之后**才输出，没法表达"我这就送你下地狱！"这种**先威胁再执行**的节奏
  7. AWAKENING → AERIAL_ASSAULT 阶段转换时已经切 AIP creative + fly，但**觉醒瞬间**（AWAKENING 刚进入时）AI 还是 survival 模式，飞不起来

## 3. ADDED Requirements

### F1. LLM 回复多样性 + 定时自言自语

- **F1.1** `config.yml` 新增 `llm.temperature`（默认 `0.9`，范围 0.0-2.0），覆盖 LlmClient 内部硬编码值
- **F1.2** LlmClient.buildPayload 中读取 `config.getTemperature()` 注入 JSON payload
- **F1.3** 新增独立调度器 `IdleMonologueTask`：每个 AI 每 10-20 秒（随机）触发一次自言自语
  - 调 LLM 单次（用 `ConversationManager.chatOnce` 模式），system 提示：「用一句话（≤20 字）表达你此刻的心理活动（OS），不要输出 [COMMAND:...]」
  - 节流：每 AI 至少 10 秒 1 次，最多 20 秒 1 次
  - 仅在 `!ai.getBusy().get()` 且 StoryState 不为 null 时才触发
  - DORMANT / COMPLETED 阶段不触发
  - 在主线程 `BukkitRunnable.runTaskTimer` 调度，异步调 LLM
- **F1.4** `ai.getBusy()` 被占用时本次自言自语直接放弃（不排队）

### F2. 决战时 AIP 自我繁殖（召唤同伴）

- **F2.1** 新增命令 `[COMMAND:summon_ally <name>]`：在 AIP 自己位置生成一个新 AIP（共享 personality + VILLAIN 设定），自动绑定为盟友
  - 通过 `plugin.getAiPlayerManager().spawn(name, self)` 实现
  - 名称生成：`<selfName>_ally_<N>`（N 自增，0-9）
  - StoryState 由 StoryManager 自动 register（每个 AIP 独立一阶段，0 死亡起步）
  - 盟军不抢剧情主线，但会跟随主 AIP 攻击玩家
- **F2.2** 限制：每个 AIP 最多召唤 2 个盟军（`max-allies-per-ai`，config 默认 2）
- **F2.3** 召唤条件：StoryPhase == PVP_DUEL 或 BETRAYAL（其它阶段不召唤）
- **F2.4** 召唤节流：每 AI 60 秒最多召唤 1 次（避免被 LLM 连发）
- **F2.5** 盟军自动攻击最近玩家：复用 PVP_DUEL 调度器，遍历所有 StoryState 时一起处理（标记为主 AIP 的盟军）
- **F2.6** 盟军死亡不影响主 AIP 剧情进度
- **F2.7** `/aip ally list` / `/aip ally remove <name>` 命令（管理用）

### F3. 真实引燃 TNT 物理投掷

- **F3.1** 新增命令 `[COMMAND:throw_tnt <power>]`：在 AIP 当前位置生成 TNT 实体，**已点燃**（`fuseTicks=40`），并向最近玩家方向施加 impulse
  - power 默认 1.5，可调 0.5-3.0
  - 朝向：使用 `target.getLocation().add(0, 1, 0).toVector().subtract(tnt.getLocation().toVector())` 计算方向，normalize 后乘 power
  - 不传 power 时：自动计算 power = `distance / 8`（让 TNT 落到玩家附近）
- **F3.2** 替换 `tnt_strike_burst`：现在改为生成**已点燃** TNT 实体，**从 AIP 当前位置向玩家方向**施加 impulse（不再是头顶 8 格的 setBlock）
- **F3.3** 替换 `fly_bomb_player`：现在用 F3.1 相同逻辑（AIP 已在飞行模式，朝玩家扔 TNT）
- **F3.4** TNT 落地时炸玩家，**让 Minecraft 物理引擎自然处理**爆炸（不要手动触发）
- **F3.5** 伤害预期：标准 TNT 默认 4 秒引爆，4×4×4 范围，炸到玩家 17-30 伤害（不调 power）

### F4. 剑伤匹配 + 装备防御 + 装备强化

- **F4.1** 新增方法 `AIPlayer.applyEquipmentAttributes()`：根据当前主手 / 装备自动调整 `Attribute.GENERIC_ATTACK_DAMAGE` 和 `Attribute.GENERIC_ARMOR`
  - 主手剑：按 `Material` 查表（WOOD=4, STONE=5, IRON=6, DIAMOND=7, NETHERITE=8），加上附魔加成（SHARPNESS 每级 +0.5×level+0.5）
  - 护甲：按 4 件总和（LEATHER=3, CHAIN=5, IRON=8, DIAMOND=10, NETHERITE=12），加上 PROTECTION 每级 +1
  - 头盔 / 胸甲 / 护腿 / 靴子 全部参与计算
- **F4.2** 装备变化时自动调用：命令 `equip` / `equip_netherite_set` / `force_survival_player` 之后调一次
- **F4.3** 在 `CommandExecutor.handleAttack` 中使用 `Attribute.GENERIC_ATTACK_DAMAGE` 真实值（替代现在 `getAttackDamage()` 单一配置值）
  - 当 AI 用下界合金剑 + SHARPNESS V 时单次攻击 ≈ 8 + 0.5×5+0.5 = 11 伤害
  - 当 AI 用木剑时 ≈ 4 伤害
- **F4.4** 在 `CommandExecutor.handleCombo` 中也用真实剑伤
- **F4.5** `equip_netherite_set` 升级：装备完下界合金套后调 `applyEquipmentAttributes()`，**额外**设 `Attribute.GENERIC_MAX_HEALTH` 提到 40（2 倍），`Attribute.GENERIC_MOVEMENT_SPEED` 提到 0.13（略快）
- **F4.6** `Attribute.GENERIC_KNOCKBACK_RESISTANCE` = 0.4（被击退强度 60% 抗性），让 PVP 阶段 AI 不容易被弹飞
- **F4.7** `Attribute.GENERIC_ARMOR_TOUGHNESS` = 8.0（下界合金真实 8.0）
- **F4.8** 提供 `/aip stats <name>` 命令显示当前 AIP 实际属性（attackDamage / armor / toughness / maxHealth / speed）

### F5. AIP 多说话 + 多做动作

- **F5.1** 降低 `autonomous-interval` 默认值：从 60s 降到 **25s**（让 LLM 决策更频繁）
- **F5.2** prompt 注入：在 `ConversationManager.chat()` system prompt 末尾追加：
  > "你是 Minecraft 中的 AI 玩家。回复要简短自然。多用 [COMMAND:swing] / [COMMAND:jump] / [COMMAND:emote] / [COMMAND:walk_dir] / [COMMAND:look_at_player] 等动作命令，让玩家看到你'在做事情'。"
- **F5.3** 环境感知扫描间隔（`env-scan-interval`）从 5s 降到 **3s**
- **F5.4** StoryPhase 阶段专属 prompt 加强：
  - AERIAL_ASSAULT：「正在飞，用 TNT 砸下面的玩家」
  - PVP_DUEL：「正在地面 PK，多挥剑多跳跃多喝血」
  - BETRAYAL：「攻击玩家，致他于死地」
- **F5.5** PVP_DUEL 调度器（`tickPvpDuel`）每 2 秒执行命令时，**随机**插入 `swing` / `jump` / `emote` / `combo` 动作
- **F5.6** AERIAL_ASSAULT 调度器每 4 秒轰炸时，**随机**插入 `say` 台词（嘲讽玩家）

### F6. 执行命令时输出威胁感台词

- **F6.1** `CommandExecutor.executeWithResult` 改造：先处理所有命令，**再**统一广播对话文字（当前实现已是如此）—— 改为：**先 sayInChat 文字 → 再 sleep 5 tick → 再执行命令**
  - 让玩家先看到台词「我这就送你下地狱！」，5 tick（0.25 秒）后才看到 kill 命令生效
  - 这种"先威胁再动手"的节奏让玩家有压迫感
- **F6.2** 命令执行前的台词注入：对于以下"高威胁命令"自动注入台词（不需要 LLM 输出）：
  - `kill` / `kill_player` → 「§c我不打算给你机会了。」
  - `force_survival_player` → 「§c回到地面吧，公平对决。」
  - `gamemode survival`（对玩家） → 「§4给我跪下！」
  - `fly off` → 「§6我来了。」
  - `tnt_strike_burst` / `throw_tnt` / `fly_bomb_player` → 「§c尝尝这个！」
  - `equip_netherite_set` → 「§c现在让你看看真正的力量。」
  - `dictate_order` → 「§e这是命令，不是请求。」
  - `kick` / `ban` → 「§4你已经没有容身之处了。」
- **F6.3** 高威胁命令执行前**先 sayInChat**（注入的台词），**再 sleep 10 tick**（0.5 秒），**再执行命令**
- **F6.4** 配置项 `enable-threat-taunts`（默认 true），关闭后不注入台词
- **F6.5** 每条台词去重：30 秒内同条台词同一 AI 不重复（复用 `recentMessages` 节流）

### F7. 觉醒瞬间 AIP 切创造 + 玩家切生存

- **F7.1** `StoryManager.onAiDeath` 在 `transitionTo(AWAKENING)` 成功时立即执行（不等到 AERIAL_ASSAULT 阶段）：
  - `StageAction.runCommand(ai, "force_survival_player " + killerName)` —— 强制玩家生存
  - `StageAction.runCommand(ai, "gamemode creative")` —— 自己切创造
  - `StageAction.runCommand(ai, "fly on")` —— 开启飞行
  - `StageAction.say(ai, "现在，让我来控制战场！")` —— 台词
- **F7.2** 玩家的**飞行 + 创造**模式被强制关闭（force_survival_player 已实现）
- **F7.3** AI 进入 AERIAL_ASSAULT 阶段时**不重复**切 creative（已经是 creative 不动）
- **F7.4** 玩家被强制切生存时如果他在飞行，会被踢到地面（Paper API 行为）

## 4. MODIFIED Requirements

- **M1** `LlmClient.buildPayload` 的 `temperature` 值由 `config.getTemperature()` 决定（之前是 hardcode `1.0`）
- **M2** `CommandExecutor.executeWithResult` 在执行命令前先 sayInChat 文字（F6.1）
- **M3** `CommandExecutor.dispatchCommand` 在执行高威胁命令前注入台词（F6.2 / F6.3）
- **M4** `CommandExecutor.handleAttack` 使用 `Attribute.GENERIC_ATTACK_DAMAGE` 真实值
- **M5** `CommandExecutor.handleCombo` 使用 `Attribute.GENERIC_ATTACK_DAMAGE` 真实值
- **M6** `CommandExecutor.handleEquipNetheriteSet` 装备后调 `applyEquipmentAttributes()` + 加 maxHealth / movementSpeed / knockbackResistance
- **M7** `CommandExecutor.handleTntStrikeBurst` 改为生成**已点燃** TNT 实体 + impulse（不再 setBlock）
- **M8** `CommandExecutor.handleFlyBombPlayer` 改为与 F3.1 相同逻辑（统一接口）
- **M9** `StoryManager.onAiDeath` 在 AWAKENING 转换时立即执行 F7.1 的命令
- **M10** `StoryManager.tickPvpDuel` 在 walk / attack 之间随机插入 swing / jump / emote
- **M11** `StoryManager.tickAerialAssault` 轰炸时随机插入 say 嘲讽
- **M12** `AIPlayerManager.startAutonomousTask` 频率从 60s 默认 25s
- **M13** `AIPlayerManager.startEnvironmentTask` 频率从 5s 默认 3s
- **M14** `ConversationManager.chat()` system prompt 末尾追加 F5.2 的提示
- **M15** `ConfigManager` 新增配置项：`llm.temperature` / `idle-monologue-min-seconds` / `idle-monologue-max-seconds` / `max-allies-per-ai` / `ally-summon-cooldown-seconds` / `enable-threat-taunts` / `autonomous-interval` 默认 25 / `env-scan-interval` 默认 3

## 5. REMOVED Requirements

无。仅替换 / 升级现有命令行为。

## 6. Out of Scope

- 不做 LLM 微调 / fine-tune（保持现有 prompt 即可）
- 不做盟军自定义 personality（默认 VILLAIN）
- 不做 TNT 弹道预测 / 链式爆炸
- 不做粒子效果 / 音效（保持 Minecraft 原生）
- 不做客户端 mod 集成

## 7. File-by-File Changes

| 文件 | 改动 |
|------|------|
| `pom.xml` | version 2.1.4 → 2.2.0 |
| `src/main/resources/config.yml` | 新增 llm.temperature / monologue / max-allies 等配置 |
| `src/main/java/com/aip/ai/LLMClient.java` | buildPayload 读取 config.getTemperature() |
| `src/main/java/com/aip/ai/ConversationManager.java` | system prompt 追加"多做动作"提示 |
| `src/main/java/com/aip/ai/AIPlayerManager.java` | 启动 IdleMonologueTask，autonomous 频率调整 |
| `src/main/java/com/aip/ai/AIPlayer.java` | 新增 applyEquipmentAttributes() 方法 |
| `src/main/java/com/aip/ai/IdleMonologueTask.java` | 新建：每 10-20 秒随机自言自语 |
| `src/main/java/com/aip/ai/CommandExecutor.java` | 高威胁命令前注入台词；attack 用真实 Attribute；equip 后调 applyEquipmentAttributes；tnt 改为引燃实体；新增 summon_ally / throw_tnt |
| `src/main/java/com/aip/story/StoryManager.java` | AWAKENING 转换时立即切 creative + force_survival；tickPvpDuel 插入动作；tickAerialAssault 插入嘲讽 |
| `src/main/java/com/aip/ai/CommandDocsProvider.java`（新） | 提供高威胁命令的台词模板 |
| `src/main/java/com/aip/ai/AllyManager.java`（新） | 盟军管理（list / remove / 共享目标） |
| `src/main/java/com/aip/AIPlayerPlugin.java` | 注册 AllyManager + IdleMonologueTask |
| `src/main/java/com/aip/config/ConfigManager.java` | 新增 6 个配置项 getter |
| `MODRINTH.md` | 添加 v2.2.0 更新日志 |

## 8. Risks & Mitigations

| 风险 | 缓解 |
|------|------|
| 玩家面对多个 AIP + 真实 TNT 时死太快 | 降低 TNT 数量（每 8 秒一颗）+ 减少 AIP 攻击力 30% |
| LLM 温度调高后输出乱码 | 范围 0.0-2.0，配置 default 0.9（温和） |
| 盟军太多性能下降 | 硬上限 2 个 / AI |
| 自言自语 5 秒间隔太频繁刷屏 | 随机 10-20 秒 |
| 真实 Attribute 设置后 AIP 太肉 | maxHealth 提到 40 而非 100 |
| 玩家被强制切生存没同意 | 这是邪恶 AI 故意的——符合"故事模式"主题 |

## 9. Acceptance Criteria

- [ ] LLM 每次回复都不一样（用户测试连续 10 次无重复）
- [ ] 每个 AI 每 10-20 秒说一次自言自语（聊天框可见）
- [ ] PVP_DUEL 阶段主 AIP 可召唤 1-2 个盟军一起打玩家
- [ ] TNT 是已点燃实体 + 朝玩家方向 impulse（不是 setBlock 落地）
- [ ] 装备下界合金后单次剑伤 10-12 伤害（木剑 4 伤害）
- [ ] AI 持下界合金套 + 盾牌时受到攻击实际护甲 12 + 韧性 8
- [ ] autonomous 频率 ≥ 25s/次
- [ ] 高威胁命令执行前能看到对应威胁台词
- [ ] 觉醒瞬间玩家被强制切生存 + AI 切创造飞起来
- [ ] 编译通过 + jar 3.4MB+ + 启动正常
- [ ] git tag v2.2.0 + gh release 上传 jar
