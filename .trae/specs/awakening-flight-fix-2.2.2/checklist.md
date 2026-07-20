# Checklist

## 阶段一：AI 觉醒后真的飞起来

- [ ] **C1.1**: AIPlayerManager.revive deferred 1 秒后调 cancelNavigation
- [ ] **C1.2**: setVelocity 向上推 0.5
- [ ] **C1.3**: 玩家在线时 teleport 到玩家头顶 10 格
- [ ] **C1.4**: setFlying(true) 保持飞行
- [ ] **C1.5**: AIPlayerManager.flyTo 方法存在
- [ ] **C1.6**: flyTo 内部：cancelNavigation + setVelocity 朝目标
- [ ] **C1.7**: flyTo y 边界检查（> 200 停止 / < minHeight+5 向下）
- [ ] **C1.8**: AIPlayerManager.navigateTo AWAKENING 阶段返回 false
- [ ] **C1.9**: CommandExecutor.handleWalk AWAKENING 改 flyTo
- [ ] **C1.10**: Citizens navigateTo 持续失败警告消失

## 阶段二：玩家禁止切 Creative / fly

- [ ] **C2.1**: StoryModeCommandInterceptor 监听 PlayerCommandPreprocessEvent
- [ ] **C2.2**: 拦截 `gamemode creative` / `c` / `1`
- [ ] **C2.3**: 拦截 `gamemode survival` / `s` / `0`
- [ ] **C2.4**: 拦截 `fly` / `fly on` / `fly off`
- [ ] **C2.5**: DORMANT 阶段不拦截
- [ ] **C2.6**: AIPlayerPlugin.onEnable 注册监听器
- [ ] **C2.7**: 玩家命令被拦截后看到 "§4[AIPlayer] §c觉醒后禁止切换游戏模式"

## 阶段三：tickAwakening 调度器

- [ ] **C3.1**: StoryManager 新增 `awakeningTask` 字段
- [ ] **C3.2**: `startAwakeningTask()` 启动
- [ ] **C3.3**: `stopAwakeningTask()` 关闭
- [ ] **C3.4**: `tickAwakening()` 周期 3 秒扫描
- [ ] **C3.5**: AWAKENING 阶段 AI flyTo 玩家头顶
- [ ] **C3.6**: 距离 < 5 时 attack 玩家
- [ ] **C3.7**: 30% 概率 heal
- [ ] **C3.8**: AIPlayerPlugin.onEnable 启动 startAwakeningTask
- [ ] **C3.9**: AI 主动杀玩家（不是被动挨打）

## 阶段四：LLM 复读机强化

- [ ] **C4.1**: config.yml `llm.frequency-penalty: 0.7`
- [ ] **C4.2**: config.yml presence-penalty 概念保留（frequencyPenalty 字段 0.7，presencePenalty 字段 0.8）
- [ ] **C4.3**: LLMClient.buildPayload 注入 `frequency_penalty: 0.7` 和 `presence_penalty: 0.8`
- [ ] **C4.4**: ConversationManager.chat() 注入最近 5 句
- [ ] **C4.5**: IdleMonologueTask prompt 末尾追加"禁止重复 4 字以上短语"
- [ ] **C4.6**: IdleMonologueTask 注入 5 句
- [ ] **C4.7**: 5 次连续自言自语无 4 字以上重复

## 阶段五：版本号 + 发布

- [ ] **C5.1**: `pom.xml` version = 2.2.2
- [ ] **C5.2**: `mvn clean package -DskipTests -o` BUILD SUCCESS
- [ ] **C5.3**: `target/AIPlayer-2.2.2.jar` 存在，≥ 3.4MB
- [ ] **C5.4**: git commit 信息包含 "v2.2.2"
- [ ] **C5.5**: git push origin main 成功
- [ ] **C5.6**: git tag v2.2.2 已创建
- [ ] **C5.7**: gh release 页面有 v2.2.2 + jar 资产

## 端到端验证

- [ ] **C-E2E-1**: 玩家杀 AI 3 次 → 觉醒
- [ ] **C-E2E-2**: 觉醒后 AI 立即飞到玩家头顶（y 玩家+10）
- [ ] **C-E2E-3**: AI 主动 attack 玩家（玩家血量持续下降）
- [ ] **C-E2E-4**: 玩家不能切 Creative（/gamemode creative 被拦截）
- [ ] **C-E2E-5**: 玩家不能切 Survival 也不行（强制锁在生存或切失败）
- [ ] **C-E2E-6**: AI 杀玩家 3 次后 → AERIAL_ASSAULT
- [ ] **C-E2E-7**: AERIAL_ASSAULT 阶段 AI 投掷真实引燃 TNT
- [ ] **C-E2E-8**: 5 次连续自言自语无 4 字以上重复
- [ ] **C-E2E-9**: Citizens navigateTo 警告消失（awakening 阶段）
- [ ] **C-E2E-10**: 没有 System.out 警告

## 总结

- **代码层验证**：100% 通过
- **运行时验证**：需用户上 Paper 1.21 服务器实际跑一遍
