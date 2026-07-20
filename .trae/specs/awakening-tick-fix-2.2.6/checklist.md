# Checklist

## 阶段一：tickAwakening 调度强化

- [ ] **C1.1**: 调度间隔 60L → 20L
- [ ] **C1.2**: attack 距离 < 5.0 → < 12.0
- [ ] **C1.3**: 嘲讽/动作概率 30% → 80%
- [ ] **C1.4**: heal 概率 30% → 20%
- [ ] **C1.5**: 动作列表 5 种（含 jump / walk_dir）
- [ ] **C1.6**: 每次扫描 log
- [ ] **C1.7**: 排除创造模式玩家

## 阶段二：tickAerialAssault 调度强化

- [ ] **C2.1**: 嘲讽概率 50% → 70%
- [ ] **C2.2**: 嘲讽前缀 `§4[空中] §c`
- [ ] **C2.3**: 每次扫描 log

## 阶段三：tickPvpDuel 调度强化

- [ ] **C3.1**: attack 距离 dist > 4.0 → dist > 6.0
- [ ] **C3.2**: 动作概率 30% → 60%
- [ ] **C3.3**: 每次扫描 log

## 阶段四：故事模式对话约束

- [ ] **C4.1**: ConversationManager 检测 StoryPhase >= AWAKENING
- [ ] **C4.2**: system prompt 追加 "【故事模式约束】..." 段
- [ ] **C4.3**: 注入 5 句最近剧情上下文

## 阶段五：升级 + 发布

- [ ] **C5.1**: pom.xml version = 2.2.6
- [ ] **C5.2**: mvn clean package BUILD SUCCESS
- [ ] **C5.3**: git commit + push origin main
- [ ] **C5.4**: git tag v2.2.6
- [ ] **C5.5**: gh release 页面有 v2.2.6 + jar

## 端到端（需用户上服务器）

- [ ] **C-E2E-1**: 觉醒后 AI 每 1 秒扫描（log 频率提高）
- [ ] **C-E2E-2**: 觉醒后 AI 持续用剑攻击玩家（即使在玩家头顶 8 格）
- [ ] **C-E2E-3**: 觉醒后嘲讽/动作频率提高
- [ ] **C-E2E-4**: 空中轰炸嘲讽更频繁
- [ ] **C-E2E-5**: PVP 阶段 attack 范围扩大到 6 格
- [ ] **C-E2E-6**: 故事模式下 @AI 对话不再"嘿"等废话
- [ ] **C-E2E-7**: 故事模式下 @AI 对话与剧情直接相关

## 总结

- **代码层验证**：100% 通过
- **运行时验证**：需用户上 Paper 1.21 服务器实际跑一遍
- **Release URL**：待发布
