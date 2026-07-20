# Tasks

## 阶段一：删除旧故事系统

- [ ] **Task 1**: StoryPhase 枚举完全重写
  - [ ] SubTask 1.1: 替换 8 个旧阶段为 12 章节
  - [ ] SubTask 1.2: 添加 3 个坏结局
  - [ ] SubTask 1.3: 保留 COMPLETED

- [ ] **Task 2**: StoryState 完全重写
  - [ ] SubTask 2.1: 移除 awakeningPending / pendingKillerName
  - [ ] SubTask 2.2: 添加 storyStarted / storyCompleted
  - [ ] SubTask 2.3: 添加 chapterStartTime
  - [ ] SubTask 2.4: 添加 playerFlags（trustMrSparkle / sawEveWarning）

- [ ] **Task 3**: StoryManager 完全重写
  - [ ] SubTask 3.1: 删除所有旧阶段调度（awakeningTask / aerialAssaultTask / etc）
  - [ ] SubTask 3.2: 删除 7 个 LLM hook 方法（v2.2.5 引入）
  - [ ] SubTask 3.3: 删除 notifyLlm
  - [ ] SubTask 3.4: 新增章节调度（tickChapter）每 10 秒扫描
  - [ ] SubTask 3.5: 12 个 chapter 进入方法（enterChapter1...12）
  - [ ] SubTask 3.6: 章节结束自动进入下一章

- [ ] **Task 4**: 删除 StoryModeCommandInterceptor
  - [ ] SubTask 4.1: 删除文件
  - [ ] SubTask 4.2: 移除 AIPlayerPlugin 中注册

- [ ] **Task 5**: AIPlayerManager 移除觉醒
  - [ ] SubTask 5.1: revive() 恢复 v2.2.1 简单版（不切模式）
  - [ ] SubTask 5.2: 保留 flyTo + npcFlightMode（走廊追逐用）

- [ ] **Task 6**: NpcDeathListener 移除觉醒逻辑
  - [ ] SubTask 6.1: 移除觉醒切模式调用

## 阶段二：火柴盒世界生成

- [ ] **Task 7**: 新建 MatchesHouseGenerator
  - [ ] SubTask 7.1: 生成 5x5x5 火柴盒（玻璃墙 + 木地板 + 屋顶）
  - [ ] SubTask 7.2: 内部装饰（床/箱子/火把/地毯/画）
  - [ ] SubTask 7.3: 床位朝向门口
  - [ ] SubTask 7.4: 入口门

- [ ] **Task 8**: 新建 CorridorGenerator
  - [ ] SubTask 8.1: 长走廊（20x3x100 块）
  - [ ] SubTask 8.2: 走廊两边有 10 扇门
  - [ ] SubTask 8.3: 尽头巨大铁门

- [ ] **Task 9**: 新建 AiHeadquartersGenerator
  - [ ] SubTask 9.1: 巨型铁门后是 50x50 房间
  - [ ] SubTask 9.2: 内部有 9x9 监控屏幕（红石灯模拟）
  - [ ] SubTask 9.3: 屏幕显示火柴盒玩家日常

## 阶段三：NPC 创建

- [ ] **Task 10**: 新建 MrSparkleNPC
  - [ ] SubTask 10.1: Citizens NPC 类
  - [ ] SubTask 10.2: 章节 1-5 的对话（LLM 驱动）
  - [ ] SubTask 10.3: 章节 5 自爆逻辑
  - [ ] SubTask 10.4: 离开火柴盒后消失

- [ ] **Task 11**: 新建 EveNPC
  - [ ] SubTask 11.1: Citizens NPC 类
  - [ ] SubTask 11.2: 章节 3 出现 + 送礼
  - [ ] SubTask 11.3: 章节 6-9 追击
  - [ ] SubTask 11.4: 章节 10A 切回温和
  - [ ] SubTask 11.5: 章节 10B 切攻击模式

## 阶段四：命令集成

- [ ] **Task 12**: AIPCommand 添加 /aistory
  - [ ] SubTask 12.1: 子命令 /aistory
  - [ ] SubTask 12.2: 子命令 /aistory exit
  - [ ] SubTask 12.3: 子命令 /aistory status
  - [ ] SubTask 12.4: 权限检查（op）

- [ ] **Task 13**: AIPlayerPlugin 注册故事
  - [ ] SubTask 13.1: 启动时初始化 StoryManager
  - [ ] SubTask 13.2: 监听玩家事件（章节触发）

## 阶段五：升级 + 发布

- [ ] **Task 14**: 升级到 v2.2.7 + 发布
  - [ ] SubTask 14.1: `pom.xml` version 2.2.6 → 2.2.7
  - [ ] SubTask 14.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [ ] SubTask 14.3: git commit + push origin main
  - [ ] SubTask 14.4: git tag v2.2.7
  - [ ] SubTask 14.5: gh release create v2.2.7

# Task Dependencies

```
Task 1-6 (删除旧故事) parallel
Task 7-9 (世界生成) parallel
Task 10-11 (NPC) parallel
Task 12-13 (命令集成) depends on Task 1-11
Task 14 (发布) last
```

# 验证清单（运行时）

- [ ] V1: /aistory 启动故事
- [ ] V2: 火柴盒世界生成正确
- [ ] V3: 12 章节按顺序触发
- [ ] V4: 3 个坏结局都能触发
- [ ] V5: Mr. Sparkle 对话符合剧情
- [ ] V6: Eve 送礼 + 追击逻辑
- [ ] V7: 走廊追逐有 5 轮障碍
- [ ] V8: AI 总部有 9x9 监控屏幕
- [ ] V9: 每个玩家进度独立
- [ ] V10: 故事完成后无法重玩
