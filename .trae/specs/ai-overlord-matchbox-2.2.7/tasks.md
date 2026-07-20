# Tasks

## 阶段一：删除旧故事系统

- [x] **Task 1**: StoryPhase 枚举完全重写
  - [x] SubTask 1.1: 替换为 11 章节（AI 统治版）
  - [x] SubTask 1.2: 添加 3 个坏结局（投降/反抗/信任之花）
  - [x] SubTask 1.3: 保留 COMPLETED
  - [x] SubTask 1.4: 添加每个章节的 displayName + durationSeconds

- [x] **Task 2**: StoryState 完全重写
  - [x] SubTask 2.1: 移除 awakeningPending / pendingKillerName / sawEveWarning / trustMrSparkle
  - [x] SubTask 2.2: 添加 storyStarted / storyCompleted
  - [x] SubTask 2.3: 添加 chapterStartTime
  - [x] SubTask 2.4: 添加 flowerUndisposed 标志（隐藏坏结局 3 用）
  - [x] SubTask 2.5: 添加 playerOriginalOpStatus

- [x] **Task 3**: StoryManager 完全重写
  - [x] SubTask 3.1: 删除所有旧阶段调度
  - [x] SubTask 3.2: 删除旧 12 章节 enterChapter 方法
  - [x] SubTask 3.3: 新增 `executeAiCommand(player, command)` 方法（聊天框先输出再执行）
  - [x] SubTask 3.4: 新增 11 章节 enterChapter 方法
  - [x] SubTask 3.5: 章节结束自动进入下一章
  - [x] SubTask 3.6: Chapter 6 真正执行 deop/op 命令
  - [x] SubTask 3.7: Chapter 10A 用基岩封死火柴盒

- [x] **Task 4**: 删除 StoryModeCommandInterceptor
  - [x] SubTask 4.1: 删除文件
  - [x] SubTask 4.2: 移除 AIPlayerPlugin 中注册

- [x] **Task 5**: AIPlayerManager 修改
  - [x] SubTask 5.1: 添加 Eve PVP 行为（EveNPC 反射实现）
  - [x] SubTask 5.2: 添加 Eve TNT 召唤（EveNPC 反射实现）
  - [x] SubTask 5.3: 添加 Eve 发射器放置（CorridorGenerator 实现）
  - [x] SubTask 5.4: revive() 保持 v2.2.1 简单复活

- [x] **Task 6**: NpcDeathListener 移除觉醒逻辑
  - [x] SubTask 6.1: 移除觉醒切模式调用
  - [x] SubTask 6.2: 改为：NPC 死亡 = 故事继续（不影响章节进度）

## 阶段二：删除 AiHeadquartersGenerator

- [x] **Task 7**: 删除 AiHeadquartersGenerator.java
  - [x] SubTask 7.1: 删除文件
  - [x] SubTask 7.2: 移除所有引用

## 阶段三：火柴盒 + 走廊重新设计

- [x] **Task 8**: MatchesHouseGenerator 升级
  - [x] SubTask 8.1: 保留 5x5x5 火柴盒结构
  - [x] SubTask 8.2: 添加书架（2 个）
  - [x] SubTask 8.3: 装饰更温馨（更多火把 + 地毯）
  - [x] SubTask 8.4: 床位朝向门口
  - [x] SubTask 8.5: 同时生成 Eve 的火柴盒（镜像反转版）

- [x] **Task 9**: CorridorGenerator 重新设计
  - [x] SubTask 9.1: 走廊改为 5x5x100 块
  - [x] SubTask 9.2: 每 10 米一个 TNT 发射器（共 10 个）
  - [x] SubTask 9.3: 发射器里放 TNT + 按钮
  - [x] SubTask 9.4: 走廊尽头一个巨型铁门
  - [x] SubTask 9.5: 铁门后是一个空地

## 阶段四：NPC 重写

- [x] **Task 10**: MrSparkleNPC 重写
  - [x] SubTask 10.1: Citizens NPC 类（反射）
  - [x] SubTask 10.2: 章节 1-4 对话（预设回复）
  - [x] SubTask 10.3: 章节 5 警告玩家 Eve 是 AI
  - [x] SubTask 10.4: 章节 6 后消失

- [x] **Task 11**: EveNPC 重写（AI 叛变者）
  - [x] SubTask 11.1: Citizens NPC 类（反射，皮肤 Steve）
  - [x] SubTask 11.2: 章节 3 出现 + 送花
  - [x] SubTask 11.3: 章节 4 私聊玩家
  - [x] SubTask 11.4: 章节 6 AI 叛变（飞行追玩家）
  - [x] SubTask 11.5: 章节 7 PVP 战斗（创造 + 附魔钻石剑 + 抗性 V + 力量 II）
  - [x] SubTask 11.6: 章节 8 TNT 轰炸
  - [x] SubTask 11.7: 章节 9 给出选择
  - [x] SubTask 11.8: 章节 10A 切温和（基岩封火柴盒）
  - [x] SubTask 11.9: 章节 10B kill 玩家
  - [x] SubTask 11.10: 章节 11 触发花爆炸

## 阶段五：命令集成

- [x] **Task 12**: AIPCommand 添加 /aistory
  - [x] SubTask 12.1: 子命令 /aistory（独立 AistoryCommand 类）
  - [x] SubTask 12.2: 子命令 /aistory exit
  - [x] SubTask 12.3: 子命令 /aistory status
  - [x] SubTask 12.4: 权限检查（op / aip.admin）
  - [x] SubTask 12.5: 启动前备份玩家 OP 状态

- [x] **Task 13**: AIPlayerPlugin 注册故事
  - [x] SubTask 13.1: 启动时初始化 StoryManager
  - [x] SubTask 13.2: 注册 AistoryCommand
  - [x] SubTask 13.3: 监听玩家 clickEvent（ChatListener 解析 [投降]/[反抗]）

## 阶段六：升级 + 发布

- [x] **Task 14**: 升级到 v2.2.7 + 发布
  - [x] SubTask 14.1: `pom.xml` version 2.2.6 → 2.2.7
  - [x] SubTask 14.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [x] SubTask 14.3: git commit + push origin main
  - [x] SubTask 14.4: git tag v2.2.7
  - [x] SubTask 14.5: gh release create v2.2.7

# Task Dependencies

```
Task 1-6 (删除旧故事) parallel
Task 7 (删除总部) parallel
Task 8-9 (世界生成) parallel
Task 10-11 (NPC 重写) parallel
Task 12-13 (命令集成) depends on Task 1-11
Task 14 (发布) last
```

# 验证清单（运行时）

- [ ] V1: /aistory 启动故事
- [ ] V2: 火柴盒世界生成正确
- [ ] V3: Eve 火柴盒镜像反转
- [ ] V4: 11 章节按顺序触发
- [ ] V5: 聊天框显示 `[AI 执行] /<command>`
- [ ] V6: 玩家在 Chapter 6 真的失去 OP
- [ ] V7: Eve 在 Chapter 6 真的获得 OP
- [ ] V8: Chapter 7 PVP 战斗
- [ ] V9: Chapter 8 TNT 发射器工作
- [ ] V10: 3 个坏结局都能触发
- [ ] V11: 投降选项触发坏结局 1
- [ ] V12: 反抗选项触发坏结局 2
- [ ] V13: 没看警告触发坏结局 3
- [ ] V14: 每个玩家进度独立
- [ ] V15: 故事时长约 30 分钟
- [ ] V16: 故事完成后无法重玩
