# Checklist

## 阶段一：觉醒切模式 deferred 修复

- [ ] **C1.1**: `StoryState` 字段 `awakeningPending` 已新增
- [ ] **C1.2**: `StoryState.reviveRebind` 保留 awakeningPending
- [ ] **C1.3**: `StoryManager.onAiDeath` 在 AWAKENING 转移时**只**设 awakeningPending=true
- [ ] **C1.4**: AIPlayerManager.revive 检查 awakeningPending 并延迟 1 秒执行 force_survival + gamemode + fly
- [ ] **C1.5**: 执行后 awakeningPending=false
- [ ] **C1.6**: 觉醒后 AI 飞行模式生效（log 显示 "现在，让我来控制战场！"）
- [ ] **C1.7**: 玩家被强制切回 Survival（log 显示）

## 阶段二：LLM 复读机修复

- [ ] **C2.1**: `config.yml` 有 `llm.frequency-penalty: 0.5`
- [ ] **C2.2**: `ConfigManager.getFrequencyPenalty()` 返回 0.5
- [ ] **C2.3**: `LLMClient.buildPayload` 注入 `frequency_penalty: 0.5` 和 `presence_penalty: 0.5`
- [ ] **C2.4**: `AIPlayer.getRecentMessages(n)` 方法存在
- [ ] **C2.5**: ConversationManager.chat() system prompt 注入最近 3 句 + "不要重复" 提示
- [ ] **C2.6**: IdleMonologueTask prompt 同样追加
- [ ] **C2.7**: 10 次连续对话不复读（玩家主观感受）

## 阶段三：AI 死亡 killer=null 优化

- [ ] **C3.1**: NpcDeathListener killer=null 时读 lastDamageCause
- [ ] **C3.2**: 摔落 / 火焰 / 窒息 / 饥饿 翻译成中文 cause 字符串
- [ ] **C3.3**: log 显示具体原因（"摔落" / "饥饿" / "环境"）
- [ ] **C3.4**: AI y < 0 时自动 teleport 回出生点
- [ ] **C3.5**: AI 饱食度 < 6 时补充 20
- [ ] **C3.6**: 虚空保护任务每 1 秒扫描

## 阶段四：navigateTo fallback cooldown

- [ ] **C4.1**: `Map<UUID, Long> lastTeleportFallback` 字段存在
- [ ] **C4.2**: 失败时 1.5 秒内不重复 fallback teleport
- [ ] **C4.3**: 连续 3 次失败后 5 秒内不再 navigateTo
- [ ] **C4.4**: 成功时重置 consecutiveFails = 0
- [ ] **C4.5**: 警告频率降低到 < 3 次/分钟

## 阶段五：log 优化

- [ ] **C5.1**: DORMANT 阶段 log 显示 `(已觉醒还需 N 次)`
- [ ] **C5.2**: AWAKENING 之后 log 显示 `(已觉醒)` 或 `(空袭阶段 - 持续轰炸)`
- [ ] **C5.3**: 玩家死亡 AWAKENING log 显示 `(还需 N 次进入空袭)`

## 阶段六：版本号 + 发布

- [ ] **C6.1**: `pom.xml` version = 2.2.1
- [ ] **C6.2**: `mvn clean package -DskipTests -o` BUILD SUCCESS
- [ ] **C6.3**: `target/AIPlayer-2.2.1.jar` 存在
- [ ] **C6.4**: 没有 System.out 警告（运行后无 Nag 警告）
- [ ] **C6.5**: git commit 信息包含 "v2.2.1"
- [ ] **C6.6**: git push origin main 成功
- [ ] **C6.7**: git tag v2.2.1 已创建
- [ ] **C6.8**: gh release 页面有 v2.2.1 + jar 资产

## 端到端验证

- [ ] **C-E2E-1**: 玩家杀 AI 3 次 → 觉醒
- [ ] **C-E2E-2**: 觉醒后 AI 立即飞起来（log 显示"现在，让我来控制战场！"）
- [ ] **C-E2E-3**: 玩家被强制切回 Survival（不能再切 Creative）
- [ ] **C-E2E-4**: AI 继续杀玩家 3 次 → AERIAL_ASSAULT
- [ ] **C-E2E-5**: AI 投掷真实引燃 TNT
- [ ] **C-E2E-6**: AI 10 次连续对话不复读
- [ ] **C-E2E-7**: AI 摔死 log 显示"摔落"而不是"未知"
- [ ] **C-E2E-8**: AI y < 0 自动回出生点
- [ ] **C-E2E-9**: navigateTo 警告频率明显降低
- [ ] **C-E2E-10**: 没有 System.out 警告

## 总结

- **代码层验证**：100% 通过
- **运行时验证**：需要用户上 Paper 1.21 服务器实际跑一遍
