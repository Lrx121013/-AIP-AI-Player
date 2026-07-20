# Tasks

## 阶段一：tickAwakening 调度强化

- [x] **Task 1**: StoryManager.tickAwakening 改造
  - [ ] SubTask 1.1: 调度间隔 60L → 20L
  - [ ] SubTask 1.2: attack 距离 < 5.0 → < 12.0
  - [ ] SubTask 1.3: 嘲讽/动作概率 30% → 80%
  - [ ] SubTask 1.4: heal 概率 30% → 20%
  - [ ] SubTask 1.5: 动作列表增加 jump / walk_dir（5 种）
  - [ ] SubTask 1.6: 加 log "tickAwakening fire for <ai> target=<player> dist=<d>"
  - [ ] SubTask 1.7: 排除创造模式玩家（如果 allowCreative=false）

## 阶段二：tickAerialAssault 调度强化

- [ ] **Task 2**: StoryManager.tickAerialAssault 改造
  - [ ] SubTask 2.1: 嘲讽概率 50% → 70%
  - [ ] SubTask 2.2: 嘲讽前加 `§4[空中] §c` 前缀
  - [ ] SubTask 2.3: 加 log "tickAerialAssault fire for <ai> target=<player> bombsLeft=<n>"

## 阶段三：tickPvpDuel 调度强化

- [ ] **Task 3**: StoryManager.tickPvpDuel 改造
  - [ ] SubTask 3.1: attack 距离 dist > 4.0 → dist > 6.0
  - [ ] SubTask 3.2: 动作概率 30% → 60%
  - [ ] SubTask 3.3: 加 log "tickPvpDuel fire for <ai> target=<player> dist=<d>"

## 阶段四：故事模式对话约束

- [ ] **Task 4**: ConversationManager.chat() 故事模式 prompt 约束
  - [ ] SubTask 4.1: 检测 StoryPhase >= AWAKENING
  - [ ] SubTask 4.2: system prompt 末尾追加 "【故事模式约束】..." 段
  - [ ] SubTask 4.3: 注入 5 句最近剧情上下文（用 addHistory 中的 assistant 消息）

## 阶段五：升级 + 发布

- [x] **Task 5**: 升级到 v2.2.6 + 发布
  - [ ] SubTask 5.1: `pom.xml` version 2.2.5 → 2.2.6
  - [ ] SubTask 5.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [ ] SubTask 5.3: git commit + push origin main
  - [ ] SubTask 5.4: git tag v2.2.6
  - [ ] SubTask 5.5: gh release create v2.2.6

# Task Dependencies

```
Task 1-3 (StoryManager) parallel
Task 4 (ConversationManager) depends on no one
Task 5 (发布) last
```

# 验证清单（运行时）

- [ ] V1: 觉醒后 AI 每 1 秒扫描（log 频率提高）
- [ ] V2: 觉醒后 AI 持续用剑攻击玩家（即使在玩家头顶 8 格）
- [ ] V3: 觉醒后嘲讽/动作频率提高（80% 概率）
- [ ] V4: 空中轰炸嘲讽更频繁（70%）
- [ ] V5: PVP 阶段 attack 范围扩大到 6 格
- [ ] V6: 故事模式下 @AI 对话不再"嘿"等废话
- [ ] V7: 故事模式下 @AI 对话与剧情直接相关
