# Checklist

## 阶段一：LLM 多样性 + 自言自语

- [ ] **C1.1**: `config.yml` 有 `llm.temperature: 0.9`
- [ ] **C1.2**: `config.yml` 有 `idle-monologue-min-seconds: 10` / `idle-monologue-max-seconds: 20`
- [ ] **C1.3**: `ConfigManager.getTemperature()` 返回 0.9（默认值）
- [ ] **C1.4**: LlmClient.buildPayload 注入 `temperature: 0.9`（debug 日志可见）
- [ ] **C1.5**: 启动服务器后，AI 每 10-20 秒出现一次灰字自言自语（"§7[name] 想着：..."）
- [ ] **C1.6**: DORMANT 阶段 AI 不自言自语
- [ ] **C1.7**: COMPLETED 阶段 AI 不自言自语
- [ ] **C1.8**: LLM 温度调高后，10 次连续 prompt 输出无重复（用同一 prompt）

## 阶段二：装备 Attribute 真实化

- [ ] **C2.1**: `AIPlayer.applyEquipmentAttributes()` 方法存在
- [ ] **C2.2**: 装备木剑后，AI 的 `Attribute.GENERIC_ATTACK_DAMAGE` = 4
- [ ] **C2.3**: 装备下界合金剑后，AI 的 `Attribute.GENERIC_ATTACK_DAMAGE` = 8
- [ ] **C2.4**: 装备下界合金剑 + SHARPNESS V 后，AI 的 ATTACK_DAMAGE = 11
- [ ] **C2.5**: 装备全套下界合金后，AI 的 `Attribute.GENERIC_ARMOR` = 12
- [ ] **C2.6**: equip_netherite_set 后 AI 的 MAX_HEALTH = 40
- [ ] **C2.7**: equip_netherite_set 后 AI 的 MOVEMENT_SPEED = 0.13
- [ ] **C2.8**: equip_netherite_set 后 AI 的 KNOCKBACK_RESISTANCE = 0.4
- [ ] **C2.9**: handleAttack 用 ATTACK_DAMAGE 真实值（用 `/aip stats <name>` 验证）
- [ ] **C2.10**: handleCombo 用 ATTACK_DAMAGE 真实值

## 阶段三：真实引燃 TNT

- [ ] **C3.1**: `[COMMAND:throw_tnt 1.5]` 触发后，TNT 实体已点燃（红光闪烁）
- [ ] **C3.2**: TNT 朝玩家方向飞（带 Velocity）
- [ ] **C3.3**: TNT 落地时由 Minecraft 物理引擎引爆
- [ ] **C3.4**: 玩家被 TNT 爆炸命中，扣 17-30 血
- [ ] **C3.5**: `tnt_strike_burst` 走 throw_tnt 逻辑（不再 setBlock）
- [ ] **C3.6**: `fly_bomb_player` 走 throw_tnt 逻辑
- [ ] **C3.7**: 空袭时（fly_bomb_player）TNT 飞 5-8 秒后爆炸（不是 2 秒）

## 阶段四：高威胁命令台词

- [ ] **C4.1**: `CommandDocsProvider.threatTaunts` 包含 8 个命令的台词
- [ ] **C4.2**: `[COMMAND:kill]` 执行前，AI 先 sayInChat "§c我不打算给你机会了。"
- [ ] **C4.3**: 台词与命令执行之间间隔 0.5 秒（10 tick）
- [ ] **C4.4**: `[COMMAND:force_survival_player X]` 触发后玩家从创造模式切回生存
- [ ] **C4.5**: `[COMMAND:throw_tnt]` 触发前 AI sayInChat "§c尝尝这个！"
- [ ] **C4.6**: `[COMMAND:dictate_order]` 触发前 AI sayInChat "§e这是命令，不是请求。"
- [ ] **C4.7**: 关闭 `enable-threat-taunts` 后不再注入台词
- [ ] **C4.8**: 30 秒内同条台词不重复

## 阶段五：盟军召唤

- [ ] **C5.1**: PVP_DUEL 阶段主 AIP 可发 `[COMMAND:summon_ally 1]`
- [ ] **C5.2**: 召唤成功后聊天框出现 "§c来吧，帮我！"
- [ ] **C5.3**: 服务器出现新 AIP，名 `<mainName>_ally_0`
- [ ] **C5.4**: 盟军自动攻击最近玩家
- [ ] **C5.5**: 同一主 AIP 最多 2 个盟军
- [ ] **C5.6**: 同一主 AIP 召唤冷却 60 秒
- [ ] **C5.7**: 盟军死亡不影响主 AIP 剧情
- [ ] **C5.8**: DORMANT / AWAKENING / AERIAL_ASSAULT 阶段不可召唤
- [ ] **C5.9**: `/aip ally list` 列出所有盟军对
- [ ] **C5.10**: `/aip ally remove <name>` 移除指定盟军

## 阶段六：觉醒瞬间切模式

- [ ] **C6.1**: AI 第 3 次死亡触发 AWAKENING 时**立即**执行：
  - force_survival_player killerName
  - gamemode creative
  - fly on
- [ ] **C6.2**: 此时 AI 已经在飞行模式（不是 survival 模式爬行）
- [ ] **C6.3**: 玩家被强制切回生存模式
- [ ] **C6.4**: AI 立即飞向玩家头顶 10 格
- [ ] **C6.5**: 不再等到 AERIAL_ASSAULT 阶段才飞

## 阶段七：prompt 增强 + 调度器插入动作

- [ ] **C7.1**: chat() system prompt 末尾出现"多用动作命令"提示
- [ ] **C7.2**: 阶段专属 prompt 在 AERIAL_ASSAULT / PVP_DUEL / BETRAYAL 出现
- [ ] **C7.3**: PVP_DUEL 中 30% 概率插入 swing / jump / emote 动作
- [ ] **C7.4**: AERIAL_ASSAULT 中 50% 概率 say 嘲讽台词
- [ ] **C7.5**: 嘲讽台词 30 秒内不重复

## 阶段八：AIPlayerManager 频率调整

- [ ] **C8.1**: `autonomous-interval` 默认 25s（config.yml）
- [ ] **C8.2**: `env-scan-interval` 默认 3s
- [ ] **C8.3**: 启动服务器后，`autonomousTask` 每 25 秒跑一次
- [ ] **C8.4**: `environmentTask` 每 3 秒跑一次

## 阶段九：版本号 + 发布

- [ ] **C9.1**: `pom.xml` version = 2.2.0
- [ ] **C9.2**: `MODRINTH.md` 追加 v2.2.0 节
- [ ] **C9.3**: `mvn clean package -DskipTests -o` BUILD SUCCESS
- [ ] **C9.4**: `target/AIPlayer-2.2.0.jar` 存在，≥ 3.4MB
- [ ] **C9.5**: jar 内含 `com/aip/ai/IdleMonologueTask.class`
- [ ] **C9.6**: jar 内含 `com/aip/ai/AllyManager.class`
- [ ] **C9.7**: jar 内含 `com/aip/ai/CommandDocsProvider.class`
- [ ] **C9.8**: git commit 信息包含 "v2.2.0"
- [ ] **C9.9**: git push origin main 成功
- [ ] **C9.10**: git tag v2.2.0 已创建
- [ ] **C9.11**: gh release 页面有 v2.2.0 + jar 资产

## 端到端验证

- [ ] **C-E2E-1**: 新建世界，启用 `story-mode: true` + `villain-mode: false`
- [ ] **C-E2E-2**: /aip spawn TestAIP
- [ ] **C-E2E-3**: TestAIP 10 秒内说一次开场白（spawn 路径）
- [ ] **C-E2E-4**: TestAIP 每 10-20 秒有自言自语
- [ ] **C-E2E-5**: 玩家击杀 TestAIP 3 次后：
  - TestAIP 觉醒
  - TestAIP 立刻飞起来
  - 玩家被切回生存
  - TestAIP 说 "现在，让我来控制战场！"
- [ ] **C-E2E-6**: TestAIP 继续杀玩家 2 次：
  - TestAIP 进入 AERIAL_ASSAULT
  - TestAIP 飞起来投掷真实引燃 TNT
  - 玩家被炸扣 17-30 血
- [ ] **C-E2E-7**: 3.5 分钟后 TestAIP 降下，装备下界合金套
  - 单次剑伤 11
  - 护甲 12
  - 韧性 8
  - 血量 40
  - 移动速度 0.13
- [ ] **C-E2E-8**: TestAIP 召唤盟军 "TestAIP_ally_0"
- [ ] **C-E2E-9**: 盟军自动攻击玩家
- [ ] **C-E2E-10**: TestAIP 杀玩家 2 次后：
  - 进入 RULEBOOK
  - 交出 AI 制度之书
- [ ] **C-E2E-11**: 玩家读完后 → DICTATORSHIP
- [ ] **C-E2E-12**: TestAIP 每 30 秒下一道命令
- [ ] **C-E2E-13**: 命令完成 → BETRAYAL
- [ ] **C-E2E-14**: 30 秒后 TestAIP kill 玩家 → COMPLETED
- [ ] **C-E2E-15**: 全程聊天框能看到 TestAIP 的"威胁感"台词
