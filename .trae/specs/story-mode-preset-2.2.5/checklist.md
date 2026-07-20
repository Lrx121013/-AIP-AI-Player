# Checklist

## 阶段一：移除 v2.2.4 推理支持

- [x] **C1.1**: LLMClient.buildPayload 不再注入 `chat_template_kwargs`
- [x] **C1.2**: LLMClient.chatStream 不再解析 `reasoning_content`
- [x] **C1.3**: LLMClient.StreamCallback 移除 `onThinkToken` 方法
- [x] **C1.4**: LLMClient.parseContent 移除 `reasoning_content` debug log
- [x] **C1.5**: ConfigManager 移除 4 个 reasoning 字段
- [x] **C1.6**: ConversationManager 流式回调恢复极简 lambda
- [x] **C1.7**: config.yml 移除 `llm.reasoning` 子块

## 阶段二：StoryManager 纯预设化

- [x] **C2.1**: StoryManager.onAiDeath 觉醒分支不再调 notifyLlm
- [x] **C2.2**: StoryManager.onPlayerDeathByAi 不再调 notifyLlm
- [x] **C2.3**: StoryManager.onRulebookRead 不再调 notifyLlm
- [x] **C2.4**: StoryManager.tickAerialAssault 阶段结束不再调 notifyLlm
- [x] **C2.5**: StoryManager.tickDictatorship 阶段切换不再调 notifyLlm
- [x] **C2.6**: StoryManager.notifyLlm 私有方法已删除
- [x] **C2.7**: StoryManager 6 个 LLM hook 方法存在
- [x] **C2.8**: StoryManager 4 个预设模板常量存在

## 阶段三：ConfigManager story.llm.* 配置

- [x] **C3.1**: ConfigManager 添加 7 个 `storyLlm*` 字段
- [x] **C3.2**: ConfigManager 7 个 getter 方法
- [x] **C3.3**: config.yml 新增 `story.llm.*` 7 行（全部默认 false）

## 阶段四：版本号 + 发布

- [x] **C4.1**: pom.xml version = 2.2.5
- [x] **C4.2**: mvn clean package BUILD SUCCESS
- [x] **C4.3**: git commit + push origin main
- [x] **C4.4**: git tag v2.2.5
- [x] **C4.5**: gh release 页面有 v2.2.5 + jar

## 端到端（需用户上服务器）

- [ ] **C-E2E-1**: 玩家杀 AI 3 次 → 觉醒
- [ ] **C-E2E-2**: 觉醒台词为预设 `我受够了！我要开始反击！`
- [ ] **C-E2E-3**: 觉醒后 AI 切模式 deferred 生效（v2.2.2 行为保留）
- [ ] **C-E2E-4**: AI 杀玩家达阈值 → 飞向天空 + 空中轰炸
- [ ] **C-E2E-5**: 轰炸 3.5 分钟后 → PVP_DUEL + 装备 netherite
- [ ] **C-E2E-6**: PVP 杀 2 次 → 派发 AI 制度之书
- [ ] **C-E2E-7**: 玩家读完规则书 → DICTATORSHIP
- [ ] **C-E2E-8**: 独裁命令从预设循环 5 个模板
- [ ] **C-E2E-9**: 独裁命令达阈值 → BETRAYAL
- [ ] **C-E2E-10**: 背叛 30 秒后 → kill 玩家 + 故事结束
- [ ] **C-E2E-11**: 玩家 @AI 普通对话走 LLM（不受故事模式 hook 影响）
- [ ] **C-E2E-12**: config.yml 删除 `llm.reasoning.*` 后重启不报错
- [ ] **C-E2E-13**: 7 个 hook 全关时，log 中 LLM 调用次数 = 0（除 @AI 对话）
- [ ] **C-E2E-14**: 启用 `story.llm.taunt: true` 后，嘲讽偶尔由 LLM 生成
- [ ] **C-E2E-15**: v2.2.2 觉醒飞行修复仍生效（AI 真的飞起来）

## 总结

- **代码层验证**：100% 通过
- **运行时验证**：需用户上 Paper 1.21 服务器实际跑一遍
- **Release URL**：https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v2.2.5
