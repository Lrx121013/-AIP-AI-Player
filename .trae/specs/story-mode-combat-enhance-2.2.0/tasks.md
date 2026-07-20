# Tasks

## 阶段一：LLM 多样性 + 自言自语

- [ ] **Task 1**: ConfigManager 新增配置项
  - [ ] SubTask 1.1: `llm.temperature`（double，默认 0.9，范围 0.0-2.0）
  - [ ] SubTask 1.2: `idle-monologue-min-seconds`（int，默认 10）
  - [ ] SubTask 1.3: `idle-monologue-max-seconds`（int，默认 20）
  - [ ] SubTask 1.4: `enable-threat-taunts`（boolean，默认 true）
  - [ ] SubTask 1.5: `autonomous-interval` 默认 25（旧 60）
  - [ ] SubTask 1.6: `env-scan-interval` 默认 3（旧 5）
  - [ ] SubTask 1.7: `max-allies-per-ai`（int，默认 2）
  - [ ] SubTask 1.8: `ally-summon-cooldown-seconds`（int，默认 60）
  - [ ] SubTask 1.9: 把这些项加到 `src/main/resources/config.yml` 默认值块
  - [ ] SubTask 1.10: `getTemperature()` getter 实现

- [ ] **Task 2**: LLMClient 读取 config temperature
  - [ ] SubTask 2.1: `LlmClient.buildPayload` 的 `temperature` 字段由 `config.getTemperature()` 提供
  - [ ] SubTask 2.2: 编译验证：保持原 payload 其它字段不变

- [ ] **Task 3**: 新建 IdleMonologueTask 自言自语调度器
  - [ ] SubTask 3.1: 新建 `src/main/java/com/aip/ai/IdleMonologueTask.java`
  - [ ] SubTask 3.2: `BukkitRunnable.runTaskTimer` 周期 5 秒扫描所有 AIP
  - [ ] SubTask 3.3: 每 AIP 节流：上次自言自语距今 < `idle-monologue-min-seconds` 跳过
  - [ ] SubTask 3.4: 每 AIP 强制：`idle-monologue-max-seconds` 未到不许触发
  - [ ] SubTask 3.5: 触发条件：`!ai.getBusy().get()` && StoryState != null && phase != DORMANT && phase != COMPLETED
  - [ ] SubTask 3.6: 调 LLM 用 `ai.getConversationManager().chatOnce(ai, prompt)`
  - [ ] SubTask 3.7: prompt：「你是 AIP <name>。用一句话（≤20 字）表达你此刻的心理活动（OS），符合剧情阶段 <phase>，不要输出 [COMMAND:...]」
  - [ ] SubTask 3.8: 回主线程 `Bukkit.broadcastMessage("§7" + name + " 想着：" + text)`（OS 灰字）

- [ ] **Task 4**: AIPlayerManager 启动 IdleMonologueTask
  - [ ] SubTask 4.1: AIPlayerManager 新增 `private BukkitTask monologueTask`
  - [ ] SubTask 4.2: `startMonologueTask()` 方法（幂等）
  - [ ] SubTask 4.3: `stopAutonomousTask()` 顺带 cancel monologueTask
  - [ ] SubTask 4.4: AIPlayerPlugin onEnable 调 `startMonologueTask()`
  - [ ] SubTask 4.5: AIPlayerPlugin onDisable 调 stop

## 阶段二：装备 Attribute 真实化

- [ ] **Task 5**: AIPlayer 新增 applyEquipmentAttributes()
  - [ ] SubTask 5.1: 方法签名 `public void applyEquipmentAttributes()`
  - [ ] SubTask 5.2: 读主手剑 `getInventory().getItemInMainHand().getType()`，按 Material 查攻击伤害
  - [ ] SubTask 5.3: 攻击伤害表：WOOD=4, STONE=5, IRON=6, DIAMOND=7, NETHERITE=8, GOLD=4
  - [ ] SubTask 5.4: 读取附魔：Enchantment.SHARPNESS，level×0.5+0.5
  - [ ] SubTask 5.5: 读 4 件护甲：HELMET / CHESTPLATE / LEGGINGS / BOOTS，按 Material 查护甲值
  - [ ] SubTask 5.6: 护甲值表：LEATHER=1, CHAIN=2, IRON=2, DIAMOND=3, NETHERITE=3, GOLD=1
  - [ ] SubTask 5.7: 读取附魔：Enchantment.PROTECTION，level×1
  - [ ] SubTask 5.8: 调 `Attribute.GENERIC_ATTACK_DAMAGE`（通过 Bukkit Registry 拿 attribute）
  - [ ] SubTask 5.9: 调 `Attribute.GENERIC_ARMOR` + `Attribute.GENERIC_ARMOR_TOUGHNESS`
  - [ ] SubTask 5.10: 主手空时攻击伤害重置为 1.0（玩家默认）

- [ ] **Task 6**: handleAttack 使用真实剑伤
  - [ ] SubTask 6.1: `CommandExecutor.handleAttack` 调 `ai.getEntity().getAttribute(ATTACK_DAMAGE).getValue()` 拿剑伤
  - [ ] SubTask 6.2: 传 `target.damage(realDamage, entity)`
  - [ ] SubTask 6.3: 保留原 cooldown + 短跑逻辑

- [ ] **Task 7**: handleCombo 使用真实剑伤
  - [ ] SubTask 7.1: `handleCombo` 同样读 `Attribute.GENERIC_ATTACK_DAMAGE` 真实值
  - [ ] SubTask 7.2: 每次 combo tick 用真实剑伤

- [ ] **Task 8**: equip_netherite_set 强化
  - [ ] SubTask 8.1: 装备完所有下界合金件后调 `aiPlayer.applyEquipmentAttributes()`
  - [ ] SubTask 8.2: 额外设 `Attribute.GENERIC_MAX_HEALTH` = 40
  - [ ] SubTask 8.3: 调 `entity.setHealth(40)` 同步当前血
  - [ ] SubTask 8.4: 设 `Attribute.GENERIC_MOVEMENT_SPEED` = 0.13
  - [ ] SubTask 8.5: 设 `Attribute.GENERIC_KNOCKBACK_RESISTANCE` = 0.4
  - [ ] SubTask 8.6: 设 `Attribute.GENERIC_ARMOR_TOUGHNESS` = 8.0
  - [ ] SubTask 8.7: 保留原 setInvulnerable(false)

## 阶段三：真实引燃 TNT

- [ ] **Task 9**: 新建 throw_tnt 命令
  - [ ] SubTask 9.1: `@AICommand(name = "throw_tnt", desc = "朝玩家投掷引燃 TNT", args = "[power]", category = "故事")`
  - [ ] SubTask 9.2: 处理 power 参数（默认 1.5）
  - [ ] SubTask 9.3: 找最近玩家（`StageAction.getNearestPlayer(ai)`）
  - [ ] SubTask 9.4: `world.spawn(loc.add(0, 1, 0), TNTPrimed.class)` 生成实体
  - [ ] SubTask 9.5: `tnt.setFuseTicks(40)` —— 2 秒后爆
  - [ ] SubTask 9.6: 算 direction = `target.getLocation().add(0,1,0).toVector().subtract(tnt.getLocation().toVector()).normalize().multiply(power)`
  - [ ] SubTask 9.7: `tnt.setVelocity(direction)` 应用冲量
  - [ ] SubTask 9.8: 抛射源显示火焰粒子（可选）

- [ ] **Task 10**: tnt_strike_burst 改为引燃实体
  - [ ] SubTask 10.1: 移除 `world.getBlockAt(...).setType(TNT)` 落地方块
  - [ ] SubTask 10.2: 改为调 `throw_tnt` 逻辑（生成 TNTPrimed 实体 + impulse + fuse=40）
  - [ ] SubTask 10.3: 保留 op=true 标记

- [ ] **Task 11**: fly_bomb_player 改为 throw_tnt
  - [ ] SubTask 11.1: 重构为委托 `handleThrowTnt(ai, args)` 复用 Task 9 逻辑
  - [ ] SubTask 11.2: 保留飞行高度检查 + 玩家位置抬升
  - [ ] SubTask 11.3: 保留 op=true 标记

## 阶段四：高威胁命令台词

- [ ] **Task 12**: 新建 CommandDocsProvider
  - [ ] SubTask 12.1: 新建 `src/main/java/com/aip/ai/CommandDocsProvider.java`
  - [ ] SubTask 12.2: `Map<String, List<String>> threatTaunts`：key=命令名，value=候选台词列表
  - [ ] SubTask 12.3: 至少包含 8 个高威胁命令的台词：
    - `kill` → ["§c我不打算给你机会了。", "§4受死吧。", "§c游戏结束了。"]
    - `force_survival_player` → ["§c回到地面吧，公平对决。", "§c给我跪下！"]
    - `gamemode survival`（对玩家）→ ["§4给我跪下！"]
    - `fly off` → ["§6我来了。", "§6准备受死。"]
    - `tnt_strike_burst` / `throw_tnt` / `fly_bomb_player` → ["§c尝尝这个！", "§c送你一份大礼。", "§4接招！"]
    - `equip_netherite_set` → ["§c现在让你看看真正的力量。", "§c我不再是以前的我了。"]
    - `dictate_order` → ["§e这是命令，不是请求。", "§e你的选择是服从。"]
    - `kick` / `ban` → ["§4你已经没有容身之处了。", "§4滚出去。"]
  - [ ] SubTask 12.4: `public String pickRandom(String cmdName, Random rng)` —— 从列表随机取一条
  - [ ] SubTask 12.5: 静态持有，AIPlayerPlugin 启动时初始化

- [ ] **Task 13**: CommandExecutor 在高威胁命令前注入台词
  - [ ] SubTask 13.1: `dispatchCommand` 中 `cmd` 命中高威胁名单时，先调 `aiPlayer.sayInChat(taunt)`
  - [ ] SubTask 13.2: `Bukkit.getScheduler().runTaskLater(plugin, () -> doDispatch(...), 10L)` —— 10 tick 延迟执行命令
  - [ ] SubTask 13.3: `if (!plugin.getConfigManager().isThreatTauntsEnabled())` 时跳过注入
  - [ ] SubTask 13.4: sayInChat 内部已有 30 秒去重，避免连续刷屏
  - [ ] SubTask 13.5: 修改 `executeWithResult` 流程：先 sayInChat spokenText → 5 tick 后再循环 dispatch

- [ ] **Task 14**: AIPlayerPlugin 集成 CommandDocsProvider
  - [ ] SubTask 14.1: AIPlayerPlugin 新增 `private CommandDocsProvider commandDocsProvider`
  - [ ] SubTask 14.2: `getCommandDocsProvider()` getter
  - [ ] SubTask 14.3: CommandExecutor 构造时拿到引用（或 onEnable 注入）

## 阶段五：盟军召唤

- [ ] **Task 15**: 新建 AllyManager
  - [ ] SubTask 15.1: 新建 `src/main/java/com/aip/ai/AllyManager.java`
  - [ ] SubTask 15.2: `Map<UUID, List<UUID>> mainAiToAllies`（主 AIP UUID → 盟军 UUID 列表）
  - [ ] SubTask 15.3: `Map<UUID, UUID> allyToMainAi`（盟军 UUID → 主 AIP UUID）
  - [ ] SubTask 15.4: `Map<UUID, Long> lastSummonTime` 节流
  - [ ] SubTask 15.5: `public boolean canSummon(AIPlayer mainAi)`：节流 + max-allies 检查
  - [ ] SubTask 15.6: `public AIPlayer summon(AIPlayer mainAi)`：生成盟军 + 注册到 StoryManager + 加到 map
  - [ ] SubTask 15.7: `public void removeAll(UUID mainAi)`：盟军全部 remove
  - [ ] SubTask 15.8: `public List<AIPlayer> getAllies(AIPlayer mainAi)`：返回盟军列表
  - [ ] SubTask 15.9: `public boolean isAlly(UUID uuid)`：判断 UUID 是否是任何盟军
  - [ ] SubTask 15.10: 盟军名生成：`<mainName>_ally_<N>`，N 从 0 自增

- [ ] **Task 16**: 新建 summon_ally 命令
  - [ ] SubTask 16.1: `@AICommand(name = "summon_ally", desc = "召唤盟军 AIP 协助战斗", args = "[数量]", category = "故事")`
  - [ ] SubTask 16.2: 调 `plugin.getAllyManager().canSummon(ai)` 检查
  - [ ] SubTask 16.3: 不可召唤 → 抛 RuntimeException("召唤冷却中 / 盟军已达上限")
  - [ ] SubTask 16.4: 默认数量 1，最大 2（防滥用）
  - [ ] SubTask 16.5: 循环调 `AllyManager.summon(ai)`
  - [ ] SubTask 16.6: 成功后在聊天框 sayInChat "§c来吧，帮我！"
  - [ ] SubTask 16.7: 失败原因写入 `aiPlayer.setLastQueryResult` 供 LLM 下一轮看到

- [ ] **Task 17**: StoryManager 盟军调度
  - [ ] SubTask 17.1: `tickPvpDuel` 处理 AIP 时，**同时**处理其盟军（用 AllyManager.getAllies）
  - [ ] SubTask 17.2: 盟军也执行 walk / attack / swing / jump / emote
  - [ ] SubTask 17.3: 盟军有自己的 StoryState（独立阶段，但永远 DORMANT，不参与主线）
  - [ ] SubTask 17.4: 盟军死亡不影响主 AIP 剧情
  - [ ] SubTask 17.5: 盟军不掉线、不抢主线阶段转移

- [ ] **Task 18**: AIPlayerPlugin 集成 AllyManager
  - [ ] SubTask 18.1: `private AllyManager allyManager`
  - [ ] SubTask 18.2: `getAllyManager()` getter
  - [ ] SubTask 18.3: onEnable 初始化
  - [ ] SubTask 18.4: onDisable 调 `allyManager.removeAll()`（清理）

- [ ] **Task 19**: `/aip ally` 子命令
  - [ ] SubTask 19.1: CommandExecutor 注册 `ally list` / `ally remove <name>` 命令
  - [ ] SubTask 19.2: `ally list` 列出所有盟军对
  - [ ] SubTask 19.3: `ally remove <name>` 移除指定盟军

## 阶段六：觉醒瞬间切模式

- [ ] **Task 20**: StoryManager.onAiDeath AWAKENING 转换
  - [ ] SubTask 20.1: 在 `transitionTo(AWAKENING)` 成功分支中**立即**追加执行：
    - `StageAction.runCommand(ai, "force_survival_player " + killerName)`
    - `StageAction.runCommand(ai, "gamemode creative")`
    - `StageAction.runCommand(ai, "fly on")`
  - [ ] SubTask 20.2: 已在 AERIAL_ASSAULT 阶段时不重复切（双重保护）
  - [ ] SubTask 20.3: StageAction.say(ai, "现在，让我来控制战场！")
  - [ ] SubTask 20.4: 注释："v2.2.0 觉醒瞬间切创造 + 飞 + 强制玩家生存（不等到 AERIAL_ASSAULT）"

## 阶段七：prompt 增强 + 阶段专属 prompt

- [ ] **Task 21**: ConversationManager chat() system prompt 加强
  - [ ] SubTask 21.1: 末尾追加："你是 Minecraft 中的 AI 玩家。回复要简短自然。多用 [COMMAND:swing] / [COMMAND:jump] / [COMMAND:emote] / [COMMAND:walk_dir] / [COMMAND:look_at_player] 等动作命令，让玩家看到你'在做事情'。"
  - [ ] SubTask 21.2: 故事模式 phase 提示里追加阶段专属提示：
    - AERIAL_ASSAULT：「你正在飞，用 TNT 砸下面的玩家」
    - PVP_DUEL：「你在地面 PK，多挥剑多跳跃多喝血」
    - BETRAYAL：「攻击玩家，致他于死地」
  - [ ] SubTask 21.3: 注释：v2.2.0 prompt 增强

- [ ] **Task 22**: StoryManager 调度器插入动作
  - [ ] SubTask 22.1: `tickPvpDuel` 中，walk / attack 之间 30% 概率插入 `swing` / `jump` / `emote`
  - [ ] SubTask 22.2: `tickAerialAssault` 中，每次轰炸时 50% 概率 `say` 一句嘲讽（"§c给我下来！" / "§c你躲哪去了？" / "§c这才是开始。"）
  - [ ] SubTask 22.3: 嘲讽台词 30 秒内不重复（复用 recentMessages）

## 阶段八：AIPlayerManager 频率调整

- [ ] **Task 23**: autonomous 频率
  - [ ] SubTask 23.1: `startAutonomousTask` 读 `config.getAutonomousInterval()`（默认 25s）
  - [ ] SubTask 23.2: 默认值已通过 Task 1 修改

- [ ] **Task 24**: environment 频率
  - [ ] SubTask 24.1: `startEnvironmentTask` 读 `config.getEnvScanInterval()`（默认 3s）
  - [ ] SubTask 24.2: 默认值已通过 Task 1 修改

## 阶段九：版本号 + 发布

- [ ] **Task 25**: 升级到 v2.2.0 并发布
  - [ ] SubTask 25.1: `pom.xml` version 2.1.4 → 2.2.0
  - [ ] SubTask 25.2: `MODRINTH.md` 追加 v2.2.0 更新日志
  - [ ] SubTask 25.3: `mvn clean package -DskipTests -o` 编译通过
  - [ ] SubTask 25.4: jar ≥ 3.4MB
  - [ ] SubTask 25.5: git commit "feat: v2.2.0 故事模式战斗增强 + 自言自语 + 真实 TNT + 装备 Attribute + 盟军 + 觉醒切模式"
  - [ ] SubTask 25.6: git push origin main
  - [ ] SubTask 25.7: git tag v2.2.0
  - [ ] SubTask 25.8: gh release create v2.2.0 上传 jar

# Task Dependencies

```
Task 1 (配置项)
  ├── Task 2 (LLMClient temperature)
  ├── Task 3 (IdleMonologueTask)
  │     └── Task 4 (AIPlayerManager 启动)
  ├── Task 5 (applyEquipmentAttributes)
  │     ├── Task 6 (handleAttack 真实剑伤)
  │     ├── Task 7 (handleCombo 真实剑伤)
  │     └── Task 8 (equip_netherite_set 强化)
  ├── Task 9-11 (真实 TNT 三件套)
  ├── Task 12-14 (高威胁台词)
  ├── Task 15 (AllyManager)
  │     ├── Task 16 (summon_ally 命令)
  │     ├── Task 17 (StoryManager 盟军调度)
  │     └── Task 18-19 (Plugin 集成 + /aip ally)
  ├── Task 20 (觉醒切模式)
  ├── Task 21 (prompt 增强)
  ├── Task 22 (调度器插入动作)
  ├── Task 23-24 (autonomous/env 频率)
  └── Task 25 (发布)
```

总阶段数：9（25 个 Task，90+ 个 SubTask）
