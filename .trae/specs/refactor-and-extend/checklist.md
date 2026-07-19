# Checklist

## GUI 修复验证
- [ ] `plugin.yml` 中已注册 `k` 命令，含 description/usage/permission
- [ ] `plugin.yml` 中已添加 `aip.gui` 权限，default: op
- [ ] `GuiCommand` 类已创建，实现 `CommandExecutor` 和 `TabCompleter`
- [ ] OP 玩家执行 `/k` 能打开 AI 玩家列表 GUI
- [ ] 无 `aip.gui` 权限玩家执行 `/k` 只收到"你没有权限"，不收到 "Unknown command"
- [ ] `GuiListener.onKeyPress`（PlayerCommandPreprocessEvent 监听）已删除
- [ ] `GuiManager.openPlayerList` 中 `entity.isValid()` 检查已添加
- [ ] `getHealth()` / `isInvisible()` 调用包裹 try-catch
- [ ] `"AI" + System.currentTimeMillis()` 只调用一次，存入局部变量
- [ ] `AIPlayerGuiHolder` 类已创建，实现 `InventoryHolder`
- [ ] 所有 `Bukkit.createInventory` 使用带 holder 的重载
- [ ] `GuiListener.onInventoryClick` 用 `instanceof AIPlayerGuiHolder` 判断
- [ ] `onInventoryClick` 方法入口立即 `setCancelled(true)`

## 代码休整验证
- [ ] `GuiListener` 中 `PlayerInteractEvent`、`PlayerToggleSneakEvent` import 已删除
- [ ] `onPlayerJoin` 提示文案为"输入 /k"而非"按 K 键"
- [ ] `GuiManager` 中 `fillEmpty(inv, from, to)` 公共方法已抽取
- [ ] 所有 `replace("§e","")` 链式调用已替换为 `ChatColor.stripColor()`
- [ ] `Map<UUID, AIPlayer> selectedAi` 上下文已替代标题反解析
- [ ] `executeCommand` 加了 try-catch，失败时给玩家反馈
- [ ] `handleSkinMenuClick` 异常后清理 GUI 状态
- [ ] `AIPlayerPlugin.onEnable` 中 `guiManager` 判 null 保护

## 10 个新功能验证
- [ ] `/aip history <ai> [page]` 能分页显示对话历史
- [ ] `/aip personality set <ai> <trait>` 能设置个性，trait ∈ {brave, timid, grumpy, gentle}
- [ ] 个性片段已注入 LLM system prompt
- [ ] `/aip team create/join/leave/disband/list` 全部可用
- [ ] 同队 AI 不会互相攻击
- [ ] `/aip task assign <ai> <type>` 能指派长期任务，type ∈ {build, gather, patrol, escort, farm}
- [ ] `TaskManager` 定时任务每 60 秒执行已指派任务
- [ ] `/aip relation set <ai1> <ai2> <friend|enemy|neutral>` 能设置关系
- [ ] 敌对关系（值 < -50）的 AI 相遇时主动攻击
- [ ] `/aip revive <ai>` 能在死亡位置复活 AI，保留对话历史和个性
- [ ] `/aip schedule <ai> <time-range> <action>` 能添加日程
- [ ] 日程在对应游戏时间段自动执行 action
- [ ] `/aip mood <ai>` 能显示情绪值和状态（开心/平静/沮丧）
- [ ] 被攻击时 mood -= 10，对话时 mood += 2，击杀时 mood += 5
- [ ] mood < 30 时 prompt 注入"心情沮丧"
- [ ] `/aip deathlog <ai>` 能显示死亡记录（时间/原因/凶手）
- [ ] AI 统计面板 GUI 可访问，显示 chatCount/walkDistance/killCount/onlineTime
- [ ] `CommandExecutor` 各命令已埋点统计

## 构建发布验证
- [ ] `AIPCommand.onCommand` 已添加所有新子命令分发
- [ ] `plugin.yml` 的 `/aip` usage 帮助文本已更新
- [ ] `pom.xml` version 已改为 1.4.0
- [ ] `MODRINTH.md` 更新日志已添加 v1.4.0 条目
- [ ] `mvn clean package -DskipTests` 编译通过
- [ ] `git commit` 提交成功
- [ ] `git push origin main` 推送成功
- [ ] `gh release create v1.4.0` 发布成功
