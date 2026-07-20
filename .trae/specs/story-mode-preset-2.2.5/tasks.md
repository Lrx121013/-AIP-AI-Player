# Tasks

## 阶段一：移除 v2.2.4 推理支持

- [ ] **Task 1**: LLMClient 移除 reasoning 相关
  - [ ] SubTask 1.1: 移除 `buildPayload` 中的 `chat_template_kwargs` 注入
  - [ ] SubTask 1.2: 移除 `chatStream` 中的 `reasoning_content` 解析（恢复 v2.2.3 行为）
  - [ ] SubTask 1.3: 移除 `StreamCallback.onThinkToken` 默认方法
  - [ ] SubTask 1.4: 移除 `parseContent` 中的 `reasoning_content` debug log

- [ ] **Task 2**: ConfigManager 移除 reasoning 字段
  - [ ] SubTask 2.1: 移除 `reasoningEnabled` / `reasoningStreamToChat` / `reasoningPrefix` / `reasoningColor` 字段
  - [ ] SubTask 2.2: 移除 4 个 getter
  - [ ] SubTask 2.3: 移除 `load()` 中的 4 行读取

- [ ] **Task 3**: ConversationManager 恢复 v2.2.1 极简回调
  - [ ] SubTask 3.1: 改回 `(token, isFirst) -> { if (isFirst && speaker != null) speaker.sendMessage("§7" + aiPlayer.getName() + " 正在打字…"); }`

- [ ] **Task 4**: config.yml 移除 llm.reasoning 子块
  - [ ] SubTask 4.1: 删除 `reasoning: { enabled, stream-to-chat, prefix, color }` 配置

## 阶段二：StoryManager 改造为纯预设 + 7 个 LLM hook

- [ ] **Task 5**: StoryManager 移除所有 notifyLlm 调用
  - [ ] SubTask 5.1: `onAiDeath` 觉醒分支移除 `notifyLlm`
  - [ ] SubTask 5.2: `onPlayerDeathByAi` AERIAL_ASSAULT 分支移除 `notifyLlm`
  - [ ] SubTask 5.3: `onPlayerDeathByAi` PVP_DUEL/RULEBOOK 分支移除 `notifyLlm`
  - [ ] SubTask 5.4: `onRulebookRead` 移除 `notifyLlm`
  - [ ] SubTask 5.5: `tickAerialAssault` 阶段结束移除 `notifyLlm`
  - [ ] SubTask 5.6: `tickDictatorship` 阶段切换移除 `notifyLlm`

- [ ] **Task 6**: StoryManager 移除整个 `notifyLlm` 私有方法
  - [ ] SubTask 6.1: 删除方法

- [ ] **Task 7**: StoryManager 添加 7 个 hook 方法
  - [ ] SubTask 7.1: `llmHookSituation(ai)` — 局势分析
  - [ ] SubTask 7.2: `llmHookTaunt(ai)` — 嘲讽生成
  - [ ] SubTask 7.3: `llmHookSummonAlly(ai)` — 是否生成支援
  - [ ] SubTask 7.4: `llmHookDictateOrder(ai, orderIndex)` — 下达命令内容
  - [ ] SubTask 7.5: `llmHookCheckOrder(ai, orderText, result)` — 验证命令执行
  - [ ] SubTask 7.6: `llmHookKillPlayer(ai)` — 杀玩家前最后一句话

- [ ] **Task 8**: StoryManager 集成 hook 调用点
  - [ ] SubTask 8.1: `tickAwakening` 集成 `llmHookSituation`
  - [ ] SubTask 8.2: `tickAerialAssault` 集成 `llmHookTaunt`（替换 50% 预设嘲讽）
  - [ ] SubTask 8.3: `tickPvpDuel` 集成 `llmHookSummonAlly`（替换血量 < 30% 概率生成）
  - [ ] SubTask 8.4: `tickDictatorship` 集成 `llmHookDictateOrder`（替换预设命令循环）
  - [ ] SubTask 8.5: `tickDictatorship` 命令完成时集成 `llmHookCheckOrder`
  - [ ] SubTask 8.6: `tickBetrayal` COMPLETED 时集成 `llmHookKillPlayer`

- [ ] **Task 9**: StoryManager 添加预设模板常量
  - [ ] SubTask 9.1: `TAUNT_PRESETS` — 5 个嘲讽模板
  - [ ] SubTask 9.2: `DICTATE_PRESETS` — 5 个命令模板
  - [ ] SubTask 9.3: `KILL_PRESETS` — 3 个杀玩家遗言
  - [ ] SubTask 9.4: `SITUATION_PRESETS` — 3 个局势描述模板

## 阶段三：ConfigManager 添加 story.llm.* 配置项

- [ ] **Task 10**: ConfigManager 7 个 story.llm.* 字段
  - [ ] SubTask 10.1: `storyLlmSituation` (boolean, default false)
  - [ ] SubTask 10.2: `storyLlmTaunt` (boolean, default false)
  - [ ] SubTask 10.3: `storyLlmDialogue` (boolean, default false) — 注：此钩子暂不接
  - [ ] SubTask 10.4: `storyLlmSummonAlly` (boolean, default false)
  - [ ] SubTask 10.5: `storyLlmDictateOrder` (boolean, default false)
  - [ ] SubTask 10.6: `storyLlmCheckOrder` (boolean, default false)
  - [ ] SubTask 10.7: `storyLlmKillPlayer` (boolean, default false)
  - [ ] SubTask 10.8: 7 个 getter

## 阶段四：config.yml story.llm.* 配置块

- [ ] **Task 11**: config.yml 添加 story: llm: 配置块
  - [ ] SubTask 11.1: 新增 `story.llm.*` 7 个字段（全部默认 false）

## 阶段五：编译 + 发布

- [ ] **Task 12**: 升级到 v2.2.5 + 发布
  - [ ] SubTask 12.1: `pom.xml` version 2.2.4 → 2.2.5
  - [ ] SubTask 12.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [ ] SubTask 12.3: git commit + push origin main
  - [ ] SubTask 12.4: git tag v2.2.5
  - [ ] SubTask 12.5: gh release create v2.2.5

# Task Dependencies

```
Task 1 (LLMClient) → Task 2 (ConfigManager) → Task 3 (ConversationManager) → Task 4 (config.yml)
Task 5+6 (StoryManager 移除 notifyLlm) parallel
Task 7+8+9 (StoryManager 添加 hook) depends on Task 5+6
Task 10 (ConfigManager story.llm) depends on Task 7
Task 11 (config.yml story.llm) parallel with Task 10
Task 12 (发布) last
```

# 验证清单（运行时）

- [ ] V1: 故事模式 7 阶段切换全部走预设（log 中无 notifyLlm 调用）
- [ ] V2: config.yml 不写 story.llm.* 时，故事模式 LLM 调用次数 = 0
- [ ] V3: 启用 `story.llm.taunt: true` 后，嘲讽偶尔由 LLM 生成
- [ ] V4: config.yml 删除 `llm.reasoning.*` 块后，重启不报错
- [ ] V5: 玩家 @AI 普通对话走 LLM（不受故事模式 hook 影响）
- [ ] V6: v2.2.2 觉醒切模式 deferred 仍生效
- [ ] V7: v2.2.0 真实 TNT 引燃仍生效
