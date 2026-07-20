# Tasks

## 阶段一：移除 v2.2.4 推理支持

- [x] **Task 1**: LLMClient 移除 reasoning 相关
  - [x] SubTask 1.1: 移除 `buildPayload` 中的 `chat_template_kwargs` 注入
  - [x] SubTask 1.2: 移除 `chatStream` 中的 `reasoning_content` 解析（恢复 v2.2.3 行为）
  - [x] SubTask 1.3: 移除 `StreamCallback.onThinkToken` 默认方法
  - [x] SubTask 1.4: 移除 `parseContent` 中的 `reasoning_content` debug log

- [x] **Task 2**: ConfigManager 移除 reasoning 字段
  - [x] SubTask 2.1: 移除 `reasoningEnabled` / `reasoningStreamToChat` / `reasoningPrefix` / `reasoningColor` 字段
  - [x] SubTask 2.2: 移除 4 个 getter
  - [x] SubTask 2.3: 移除 `load()` 中的 4 行读取

- [x] **Task 3**: ConversationManager 恢复 v2.2.1 极简回调
  - [x] SubTask 3.1: 改回 `(token, isFirst) -> { if (isFirst && speaker != null) speaker.sendMessage("§7" + aiPlayer.getName() + " 正在打字…"); }`

- [x] **Task 4**: config.yml 移除 llm.reasoning 子块
  - [x] SubTask 4.1: 删除 `reasoning: { enabled, stream-to-chat, prefix, color }` 配置

## 阶段二：StoryManager 改造为纯预设 + 7 个 LLM hook

- [x] **Task 5**: StoryManager 移除所有 notifyLlm 调用
  - [x] SubTask 5.1: `onAiDeath` 觉醒分支移除 `notifyLlm`
  - [x] SubTask 5.2: `onPlayerDeathByAi` AERIAL_ASSAULT 分支移除 `notifyLlm`
  - [x] SubTask 5.3: `onPlayerDeathByAi` PVP_DUEL/RULEBOOK 分支移除 `notifyLlm`
  - [x] SubTask 5.4: `onRulebookRead` 移除 `notifyLlm`
  - [x] SubTask 5.5: `tickAerialAssault` 阶段结束移除 `notifyLlm`
  - [x] SubTask 5.6: `tickDictatorship` 阶段切换移除 `notifyLlm`

- [x] **Task 6**: StoryManager 移除整个 `notifyLlm` 私有方法
  - [x] SubTask 6.1: 删除方法

- [x] **Task 7**: StoryManager 添加 7 个 hook 方法
  - [x] SubTask 7.1: `llmHookSituation(ai)` — 局势分析
  - [x] SubTask 7.2: `llmHookTaunt(ai)` — 嘲讽生成
  - [x] SubTask 7.3: `llmHookSummonAlly(ai)` — 是否生成支援
  - [x] SubTask 7.4: `llmHookDictateOrder(ai, orderIndex)` — 下达命令内容
  - [x] SubTask 7.5: `llmHookCheckOrder(ai, orderText, result)` — 验证命令执行
  - [x] SubTask 7.6: `llmHookKillPlayer(ai)` — 杀玩家前最后一句话

- [x] **Task 8**: StoryManager 集成 hook 调用点
  - [x] SubTask 8.1: `tickAwakening` 集成 `llmHookSituation`
  - [x] SubTask 8.2: `tickAerialAssault` 集成 `llmHookTaunt`（替换 50% 预设嘲讽）
  - [x] SubTask 8.3: `tickPvpDuel` 集成 `llmHookSummonAlly`（替换血量 < 30% 概率生成）
  - [x] SubTask 8.4: `tickDictatorship` 集成 `llmHookDictateOrder`（替换预设命令循环）
  - [x] SubTask 8.5: `tickDictatorship` 命令完成时集成 `llmHookCheckOrder`
  - [x] SubTask 8.6: `tickBetrayal` COMPLETED 时集成 `llmHookKillPlayer`

- [x] **Task 9**: StoryManager 添加预设模板常量
  - [x] SubTask 9.1: `TAUNT_PRESETS` — 5 个嘲讽模板
  - [x] SubTask 9.2: `DICTATE_PRESETS` — 5 个命令模板
  - [x] SubTask 9.3: `KILL_PRESETS` — 3 个杀玩家遗言
  - [x] SubTask 9.4: `SITUATION_PRESETS` — 3 个局势描述模板

## 阶段三：ConfigManager 添加 story.llm.* 配置项

- [x] **Task 10**: ConfigManager 7 个 story.llm.* 字段
  - [x] SubTask 10.1: `storyLlmSituation` (boolean, default false)
  - [x] SubTask 10.2: `storyLlmTaunt` (boolean, default false)
  - [x] SubTask 10.3: `storyLlmDialogue` (boolean, default false)
  - [x] SubTask 10.4: `storyLlmSummonAlly` (boolean, default false)
  - [x] SubTask 10.5: `storyLlmDictateOrder` (boolean, default false)
  - [x] SubTask 10.6: `storyLlmCheckOrder` (boolean, default false)
  - [x] SubTask 10.7: `storyLlmKillPlayer` (boolean, default false)
  - [x] SubTask 10.8: 7 个 getter

## 阶段四：config.yml story.llm.* 配置块

- [x] **Task 11**: config.yml 添加 story: llm: 配置块
  - [x] SubTask 11.1: 新增 `story.llm.*` 7 个字段（全部默认 false）

## 阶段五：编译 + 发布

- [x] **Task 12**: 升级到 v2.2.5 + 发布
  - [x] SubTask 12.1: `pom.xml` version 2.2.4 → 2.2.5
  - [x] SubTask 12.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [x] SubTask 12.3: git commit + push origin main
  - [x] SubTask 12.4: git tag v2.2.5
  - [x] SubTask 12.5: gh release create v2.2.5
