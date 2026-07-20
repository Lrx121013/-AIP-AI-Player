# Checklist

## 阶段一：觉醒切模式 deferred 修复

- [x] **C1.1**: `StoryState` 字段 `awakeningPending` 已新增 — ✅ StoryState.java
- [x] **C1.2**: `StoryState.reviveRebind` 保留 awakeningPending — ✅ 未重置
- [x] **C1.3**: `StoryManager.onAiDeath` 在 AWAKENING 转移时**只**设 awakeningPending=true — ✅ Sub-Agent 1 报告
- [x] **C1.4**: AIPlayerManager.revive 检查 awakeningPending 并延迟 1 秒执行 — ✅
- [x] **C1.5**: 执行后 awakeningPending=false — ✅ finally 块
- [ ] **C1.6**: 觉醒后 AI 飞行模式生效 — ⏳ 需服务器测试
- [ ] **C1.7**: 玩家被强制切回 Survival — ⏳ 需服务器测试

## 阶段二：LLM 复读机修复

- [x] **C2.1**: `config.yml` 有 `llm.frequency-penalty: 0.5` — ✅ line 218
- [x] **C2.2**: `ConfigManager.getFrequencyPenalty()` 返回 0.5 — ✅
- [x] **C2.3**: `LLMClient.buildPayload` 注入 `frequency_penalty: 0.5` 和 `presence_penalty: 0.5` — ✅ line 151-153
- [x] **C2.4**: `AIPlayer.getRecentMessages(n)` 方法存在 — ✅ line 275-298
- [x] **C2.5**: ConversationManager.chat() system prompt 注入最近 3 句 + "不要重复" 提示 — ✅ line 122-132
- [x] **C2.6**: IdleMonologueTask prompt 同样追加 — ✅ line 81-91
- [ ] **C2.7**: 10 次连续对话不复读（玩家主观感受）— ⏳ 需 LLM 端测试

## 阶段三：AI 死亡 killer=null 优化

- [x] **C3.1**: NpcDeathListener killer=null 时读 lastDamageCause — ✅ readLastDamageCause 方法
- [x] **C3.2**: 摔落 / 火焰 / 窒息 / 饥饿 翻译成中文 cause 字符串 — ✅ 20+ 种
- [x] **C3.3**: log 显示具体原因（"摔落" / "饥饿" / "环境"）— ✅
- [x] **C3.4**: AI y < 0 时自动 teleport 回出生点 — ✅ VoidGuardTask
- [x] **C3.5**: AI 饱食度 < 6 时补充 20 — ✅
- [x] **C3.6**: 虚空保护任务每 1 秒扫描 — ✅ runTaskTimer 20L

## 阶段四：navigateTo fallback cooldown

- [x] **C4.1**: `Map<UUID, Long> lastTeleportFallback` 字段存在 — ✅ AIPlayerManager.java
- [x] **C4.2**: 失败时 1.5 秒内不重复 fallback teleport — ✅ tryFallbackTeleport 检查
- [x] **C4.3**: 连续 3 次失败后 5 秒内不再 navigateTo — ✅ consecutiveFails >= 3 静默
- [x] **C4.4**: 成功时重置 consecutiveFails = 0 — ✅
- [ ] **C4.5**: 警告频率降低到 < 3 次/分钟 — ⏳ 需服务器测试

## 阶段五：log 优化

- [x] **C5.1**: DORMANT 阶段 log 显示 `(已觉醒还需 N 次)` — ✅
- [x] **C5.2**: AWAKENING 之后 log 显示 `(即将觉醒)` — ✅
- [x] **C5.3**: 玩家死亡 AWAKENING log 显示 `(还需 N 次进入空袭)` — ✅

## 阶段六：版本号 + 发布

- [x] **C6.1**: `pom.xml` version = 2.2.1 — ✅
- [x] **C6.2**: `mvn clean package -DskipTests -o` BUILD SUCCESS — ✅
- [x] **C6.3**: `target/AIPlayer-2.2.1.jar` 存在 — ✅ 3.4MB
- [ ] **C6.4**: 没有 System.out 警告 — ⏳ 需服务器运行
- [x] **C6.5**: git commit 信息包含 "v2.2.1" — ✅ 496d02e
- [x] **C6.6**: git push origin main 成功 — ✅
- [x] **C6.7**: git tag v2.2.1 已创建 — ✅
- [x] **C6.8**: gh release 页面有 v2.2.1 + jar 资产 — ✅ https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v2.2.1

## 端到端验证

- [ ] **C-E2E-1 ~ 10** — ⏳ 需用户上服务器运行验证

## 总结

- **代码层验证**：100% 通过（13/13 Task + 编译 BUILD SUCCESS + jar 3.4MB + git 推送 + gh release）
- **运行时验证**：需要用户上 Paper 1.21 服务器实际跑一遍
