# AIPlayer — LLM 驱动的智能 AI 玩家

> 让大语言模型（LLM）真正进入你的 Minecraft 世界：生成可以对话、可以行动、可以自主决策的 AI 玩家。

[![GitHub Release](https://img.shields.io/badge/Release-v1.0.0-blue)](https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.0.0)

---

## 简介

**AIPlayer** 是一个基于 Paper 1.21.x 的服务端插件，它通过接入任意 OpenAI 兼容的大语言模型（如 OpenAI、DeepSeek、Moonshot、智谱 GLM、通义千问等），在游戏中生成由 LLM 实时控制的"AI 玩家"。

这些 AI 玩家以村民实体作为物理载体，能够：

- 💬 **像真人一样聊天**——在游戏聊天框中 `@AI名字` 即可与它对话
- 🎮 **真正执行游戏操作**——行走、破坏方块、放置方块、攻击、跟随、装备武器等 12 种动作
- 🧠 **基于真实游戏数据决策**——服务器会实时把坐标、周围方块、实体、时间天气、装备等信息发送给 AI
- ⏰ **可自主活动**——即使没人 @ 它，它也会定时自主思考、行动，让世界更有生命力

简单来说：**它不是 NPC，而是一个"住"在你服务器里、能看能聊能动手的智能体玩家。**

---

## 特性亮点

| 特性 | 说明 |
|------|------|
| 🤖 LLM 驱动 | 接入任意 OpenAI 兼容接口，支持国内外主流模型 |
| 💬 聊天对话 | 聊天框 `@AI名字 消息` 即可对话，支持中英文 |
| 🎮 游戏操作 | 12 种命令：walk / break / place / attack / follow / equip 等 |
| 🧠 上下文感知 | 实时采集周围方块、实体、玩家、天气、血量等数据供 AI 决策 |
| ⏰ 自主活动 | 可配置定时自主行动，无人 @ 时也会偶尔活动 |
| 🔧 高度可配置 | 自定义系统提示词、移动速度、攻击伤害、扫描半径等 |
| 🛡️ 安全可控 | AI 无敌、OP 命令权限可单独开关 |
| 🪶 轻量构建 | jar 仅约 47KB，依赖服务器运行时提供 Paper API 与 Gson |

---

## 环境要求

- **服务端**：Paper 1.21.x（推荐 1.21.4+，API 版本 1.21）
- **Java**：JDK 21 或更高
- **LLM 服务**：任何兼容 OpenAI Chat Completions 协议的接口及 API Key

> ⚠️ 本插件不支持 CraftBukkit / Spigot，必须使用 Paper。

---

## 快速开始

### 1. 安装插件

1. 从 [GitHub Release](https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.0.0) 下载 `AIPlayer-1.0.0.jar`
2. 将 jar 文件放入服务器的 `plugins/` 目录
3. 启动一次服务器，插件会自动生成 `plugins/AIPlayer/config.yml`
4. 关闭服务器，编辑配置文件填入你的模型信息

### 2. 配置模型

打开 `plugins/AIPlayer/config.yml`，填写以下必填项：

```yaml
provider:
  base-url: "https://api.openai.com/v1"   # OpenAI 兼容接口地址
  api-key: "sk-your-api-key-here"          # 你的 API Key
  model: "gpt-4o-mini"                     # 模型名称
```

**支持的国内供应商示例：**

| 供应商 | base-url | 推荐 model |
|--------|----------|-----------|
| OpenAI | `https://api.openai.com/v1` | gpt-4o-mini |
| DeepSeek | `https://api.deepseek.com/v1` | deepseek-chat |
| Moonshot（月之暗面） | `https://api.moonshot.cn/v1` | moonshot-v1-8k |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | glm-4-flash |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | qwen-turbo |

### 3. 启动并召唤 AI

1. 重新启动服务器
2. 在游戏内执行 `/aip spawn 小明`（在你身边生成一个名为"小明"的 AI 玩家）
3. AI 会自动自我介绍并开始活动
4. 在聊天框输入：`@小明 你好，跟我一起去挖矿吧！`

---

## 命令一览

插件主命令为 `/aip`（别名 `/aiplayer`、`/ai`）：

| 命令 | 权限 | 说明 |
|------|------|------|
| `/aip spawn <名字>` | `aip.admin` | 在自己身边生成 AI 玩家 |
| `/aip remove <名字>` | `aip.admin` | 移除指定 AI 玩家 |
| `/aip list` | `aip.use` | 列出所有 AI 玩家及坐标 |
| `/aip talk <名字> <消息>` | `aip.use` | 与指定 AI 玩家对话 |
| `/aip reset <名字>` | `aip.admin` | 重置 AI 的对话历史 |
| `/aip reload` | `aip.admin` | 重新加载配置（无需重启） |

**权限说明：**

- `aip.admin`：管理操作（生成/移除/重置/重载），默认仅 OP 拥有
- `aip.use`：基础使用（列表、对话），默认所有玩家拥有

---

## 聊天 @提及 对话

最便捷的交互方式——直接在游戏聊天框 @ AI 的名字：

```
@小明 你在干嘛？
@矿工 帮我挖点石头
@守卫 前面有僵尸，快去打它！
```

**格式**：`@<AI名字> <消息内容>`（名字与消息之间用空格分隔，支持中英文）

AI 会基于自身位置周围的游戏数据（坐标、方块、实体、天气、血量、装备等）做出回复，并自动执行相应的游戏操作。普通玩家看不到命令文本，只看到对话和动作——就像在跟真人玩家互动。

---

## AI 能做什么

AI 在回复中通过 `[COMMAND:...]` 执行操作，支持以下 12 种命令：

| 命令 | 示例 | 说明 |
|------|------|------|
| `walk x y z` | `[COMMAND:walk 12 64 -30]` | 走到指定绝对坐标 |
| `walk_dir 方向 距离` | `[COMMAND:walk_dir north 5]` | 朝指定方向走指定距离（north/south/east/west/up/down） |
| `follow 玩家名` | `[COMMAND:follow Steve]` | 跟随某个玩家 |
| `stop` | `[COMMAND:stop]` | 停止移动 |
| `break x y z` | `[COMMAND:break 12 64 30]` | 破坏指定坐标的方块 |
| `place x y z 方块类型` | `[COMMAND:place 12 64 30 stone]` | 在指定坐标放置方块 |
| `attack 目标` | `[COMMAND:attack nearest]` 或 `[COMMAND:attack Steve]` | 攻击附近实体（`nearest` 或具体名称） |
| `jump` | `[COMMAND:jump]` | 跳跃 |
| `look 方向` | `[COMMAND:look north]` | 转身朝某方向看 |
| `say 消息` | `[COMMAND:say 你好]` | 在聊天框发言 |
| `cmd 服务器命令` | `[COMMAND:cmd time set day]` | 执行服务器命令（需开启 `allow-op-commands`） |
| `equip 槽位 物品` | `[COMMAND:equip hand diamond_sword]` | 装备物品（hand/helmet/chest/legs/boots） |
| `drop 槽位` | `[COMMAND:drop hand]` | 丢弃某槽位物品 |

AI 可以在一条回复中包含多条命令并同时对话，让玩家感觉它是一个会边说话边干活的真人。

---

## 配置文件参考

```yaml
provider:
  base-url: "https://api.openai.com/v1"
  api-key: "sk-your-api-key-here"
  model: "gpt-4o-mini"
  timeout: 60

ai:
  autonomous: false          # 是否自动活动（无人@时也自主行动）
  autonomous-interval: 30    # 自动活动间隔（秒）
  max-history: 20            # 对话历史保留条数
  move-speed: 0.6            # 移动速度
  attack-damage: 5.0         # 攻击伤害
  allow-op-commands: false   # 是否允许AI执行服务器命令（建议关闭）
  invulnerable: true         # AI是否无敌
  scan-radius: 8             # 方块扫描半径
  entity-scan-radius: 16     # 实体扫描半径

system-prompt: |
  你是一个名为 {name} 的 Minecraft 玩家...
  # 可自定义 AI 人设、说话风格、行为准则
  # {name} 会被替换为 AI 玩家的名字

debug: false                 # 调试日志
```

修改后可在游戏内执行 `/aip reload` 热重载，无需重启服务器。

---

## 自主活动模式

在 `config.yml` 中开启：

```yaml
ai:
  autonomous: true
  autonomous-interval: 30
```

开启后，即使没有玩家 @ AI，它也会每隔 30 秒自主思考一次，根据周围环境决定下一步操作——可能去挖矿、可能去探索、也可能在聊天框自言自语。让服务器即使无人在线也充满活力。

---

## 自定义 AI 人设

通过修改 `config.yml` 中的 `system-prompt`，你可以让 AI 扮演任何角色：

- 🧙 **法师**：会使用命令施展"魔法"
- ⛏️ **矿工**：专注于挖矿和收集资源
- 🛡️ **守卫**：巡逻并保护区域
- 🏹 **猎人**：主动攻击附近怪物
- 🤡 **搞笑角色**：用幽默方式与玩家互动

提示词中可用 `{name}` 占位符代表 AI 玩家的名字。

---

## 常见问题

**Q：启动后提示"尚未完成模型提供商配置"？**
A：编辑 `plugins/AIPlayer/config.yml`，填写 `provider.api-key`、`base-url`、`model` 三项后重启或执行 `/aip reload`。

**Q：AI 不回复或报错？**
A：将 `config.yml` 中 `debug: true` 打开，查看后台日志中的 `=== LLM 请求 ===` 和 `=== LLM 回复 ===` 排查。常见原因：API Key 错误、网络不通、模型名称错误。

**Q：AI 会执行危险命令吗？**
A：默认 `allow-op-commands: false`，AI 无法执行服务器命令。如需开启请谨慎评估风险。

**Q：支持多少个 AI 同时在线？**
A：理论上无限制，但每个 AI 对话都会调用 LLM 接口，请注意你的 API 额度和速率限制。建议同时在线 AI 数量控制在合理范围（如 5-10 个）。

**Q：jar 文件为什么这么小（47KB）？**
A：本插件为轻量构建，Paper API 与 Gson 由服务器运行时提供，jar 内仅包含插件自身代码，因此体积小、加载快。

---

## 技术细节

- **物理载体**：使用村民（Villager）实体作为 AI 玩家的物理表现，禁用了交易功能
- **决策机制**：LLM 接收包含坐标、方块、实体、天气、装备等真实游戏数据的上下文，输出对话文本与 `[COMMAND:...]` 命令
- **线程模型**：LLM 调用异步执行，游戏操作命令在主线程执行，避免阻塞服务器
- **历史管理**：每个 AI 玩家独立维护对话历史，可配置最大保留条数

---

## 相关链接

- 📦 **下载地址**：[GitHub Release v1.0.0](https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.0.0)
- 🐛 **问题反馈**：[GitHub Issues](https://github.com/Lrx121013/-AIP-AI-Player/issues)
- 📖 **源代码**：[GitHub Repository](https://github.com/Lrx121013/-AIP-AI-Player)

---

## 许可证

本项目仅供学习交流使用。

---

如果这个插件对你有帮助，欢迎在 GitHub 上点个 ⭐ 支持一下！
