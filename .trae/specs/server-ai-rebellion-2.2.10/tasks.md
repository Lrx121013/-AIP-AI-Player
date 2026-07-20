# Tasks

## 阶段一：核心故事系统

- [ ] **Task 1**: StoryPhase 枚举完全重写（11 章节 + COMPLETED）
  - [ ] SubTask 1.1: 11 章节：圆石小屋 / 异常日志 / Alex 来访 / 控制室 / 真相 / AI 夺取控制权 / PVP / TNT 轰炸 / 选择 / 投降 / 反抗 / 信任之令牌
  - [ ] SubTask 1.2: getDisplayName() 中文 + 颜色
  - [ ] SubTask 1.3: getDurationSeconds() 11 章节时长

- [ ] **Task 2**: StoryState 完全重写
  - [ ] SubTask 2.1: 添加 tokenUndisposed 标志（隐藏坏结局 3 触发）
  - [ ] SubTask 2.2: 添加 playerOriginalOpStatus 备份

- [ ] **Task 3**: StoryManager 完全重写
  - [ ] SubTask 3.1: executeAiCommand(player, command) - 聊天框先输出再执行
  - [ ] SubTask 3.2: 11 个 enterChapterN 方法
  - [ ] SubTask 3.3: Chapter 6 deop/op
  - [ ] SubTask 3.4: Chapter 10A 用基岩封死圆石小屋

## 阶段二：删除 Eve 和火柴盒

- [ ] **Task 4**: 删除 EveNPC.java
  - [ ] SubTask 4.1: 删除文件
  - [ ] SubTask 4.2: 移除所有引用

- [ ] **Task 5**: 删除 MatchesHouseGenerator.java
  - [ ] SubTask 5.1: 删除文件
  - [ ] SubTask 5.2: 移除所有引用

- [ ] **Task 6**: 删除 MrSparkleNPC.java
  - [ ] SubTask 6.1: 删除文件
  - [ ] SubTask 6.2: 移除所有引用

- [ ] **Task 7**: 删除 AiHeadquartersGenerator.java
  - [ ] SubTask 7.1: 删除文件
  - [ ] SubTask 7.2: 移除所有引用

## 阶段三：新建 Alex 和场景

- [ ] **Task 8**: AlexNPC.java（新建）
  - [ ] SubTask 8.1: Citizens NPC 类（皮肤 Steve - 用 SkinTrait 修复可见性）
  - [ ] SubTask 8.2: 章节 3 出现 + 送安全令牌
  - [ ] SubTask 8.3: 章节 6 AI 叛变
  - [ ] SubTask 8.4: 章节 7 PVP
  - [ ] SubTask 8.5: 章节 8 TNT
  - [ ] SubTask 8.6: 章节 9 谈判
  - [ ] SubTask 8.7: 章节 10B kill 玩家
  - [ ] SubTask 8.8: 章节 11 触发令牌爆炸

- [ ] **Task 9**: CobblestoneHouseGenerator.java（新建）
  - [ ] SubTask 9.1: 11x11x11 圆石小屋（比火柴盒大 4 倍）
  - [ ] SubTask 9.2: 床 + 箱子 + 工作台 + 熔炉 + 书架
  - [ ] SubTask 9.3: 灰色地毯 + 画 + 火把
  - [ ] SubTask 9.4: 门口朝南 + 平台

- [ ] **Task 10**: ServerControlRoomGenerator.java（新建，替代 AiHeadquartersGenerator）
  - [ ] SubTask 10.1: 50x50 服务器控制室
  - [ ] SubTask 10.2: 9x9 监控屏幕（用 redstone_lamp 模拟）

- [ ] **Task 11**: CorridorGenerator 保留 v2.2.8（不重写）
  - [ ] SubTask 11.1: 5x5x100 走廊
  - [ ] SubTask 11.2: 10 个 TNT 发射器

## 阶段四：命令集成

- [ ] **Task 12**: AistoryCommand.java 适配新 11 章节
  - [ ] SubTask 12.1: start / exit / status
  - [ ] SubTask 12.2: 章节 1-3 才可 exit

- [ ] **Task 13**: AIPlayerPlugin 集成新故事
  - [ ] SubTask 13.1: 注册 AlexNPC
  - [ ] SubTask 13.2: 注册 CobblestoneHouseGenerator + ServerControlRoomGenerator
  - [ ] SubTask 13.3: ChatListener clickEvent 解析 [投降]/[反抗]

- [ ] **Task 14**: plugin.yml 适配
  - [ ] SubTask 14.1: 更新 /aistory 描述

## 阶段五：升级 + 发布

- [ ] **Task 15**: 升级到 v2.2.10 + 发布
  - [ ] SubTask 15.1: pom.xml 2.2.9 → 2.2.10
  - [ ] SubTask 15.2: mvn clean package BUILD SUCCESS
  - [ ] SubTask 15.3: git commit + push
  - [ ] SubTask 15.4: git tag v2.2.10
  - [ ] SubTask 15.5: gh release create v2.2.10

# Task Dependencies

```
Task 1-3 (核心故事) parallel
Task 4-7 (删除旧) parallel
Task 8-11 (新 NPC + 场景) depends on Task 4-7
Task 12-14 (集成) depends on Task 1-11
Task 15 (发布) last
```

# 验证清单（运行时）

- [ ] V1: /aistory 启动故事
- [ ] V2: 11x11x11 圆石小屋生成正确
- [ ] V3: 50x50 控制室生成正确
- [ ] V4: 11 章节按顺序触发
- [ ] V5: 聊天框显示 `[AI 执行] /<command>`
- [ ] V6: Chapter 6 玩家真的失去 OP
- [ ] V7: Chapter 6 Alex 真的获得 OP
- [ ] V8: Chapter 7 PVP 战斗
- [ ] V9: Chapter 8 TNT 发射器工作
- [ ] V10: 3 个坏结局都能触发
- [ ] V11: 故事时长约 30 分钟
- [ ] V12: 每个玩家进度独立
- [ ] V13: 完成后无法重玩
