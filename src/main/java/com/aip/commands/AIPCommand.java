package com.aip.commands;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.ConversationManager;
import com.aip.ai.GameDataCollector;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /aip 命令处理器
 * <p>
 * 子命令：
 *   /aip spawn <名字>     —— 在自己身边生成 AI 玩家
 *   /aip remove <名字>    —— 移除 AI 玩家
 *   /aip list             —— 列出所有 AI 玩家
 *   /aip reload           —— 重新加载配置
 *   /aip talk <名字> <消息> —— 与 AI 玩家对话
 *   /aip reset <名字>     —— 重置 AI 对话历史
 */
public class AIPCommand implements CommandExecutor, TabCompleter {

    private final AIPlayerPlugin plugin;

    public AIPCommand(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "talk" -> handleTalk(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== AI Player 命令 =====");
        sender.sendMessage("§e/aip spawn <名字> §7- 生成 AI 玩家（在自己身边）");
        sender.sendMessage("§e/aip remove <名字> §7- 移除 AI 玩家");
        sender.sendMessage("§e/aip list §7- 列出所有 AI 玩家");
        sender.sendMessage("§e/aip talk <名字> <消息> §7- 与 AI 玩家对话");
        sender.sendMessage("§e/aip reset <名字> §7- 重置 AI 对话历史");
        sender.sendMessage("§e/aip reload §7- 重新加载配置");
        sender.sendMessage("§7你也可以在聊天框输入 @<AI名字> <消息> 来对话");
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (!plugin.getConfigManager().isConfigured()) {
            sender.sendMessage("§c模型提供商尚未配置！请编辑 plugins/AIPlayer/config.yml 后重启服务器。");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令必须由玩家执行。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip spawn <名字>");
            return;
        }
        String name = args[1];
        if (name.length() > 16) {
            sender.sendMessage("§c名字过长（最多 16 字符）");
            return;
        }
        Player spawner = (Player) sender;
        if (plugin.getAiPlayerManager().get(name) != null) {
            sender.sendMessage("§c已存在同名 AI 玩家: " + name);
            return;
        }
        AIPlayer aiPlayer = plugin.getAiPlayerManager().spawn(name, spawner);
        // 首次激活：发送初始系统提示给 AI
        if (!aiPlayer.isActivated()) {
            activateAI(aiPlayer);
        }
    }

    /**
     * 首次激活 AI 玩家：发送初始上下文，让 AI 给一个自我介绍
     */
    private void activateAI(AIPlayer aiPlayer) {
        aiPlayer.setActivated(true);
        // 先在主线程采集游戏数据，再异步调用 LLM（Bukkit.getEntity 必须在主线程）
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(aiPlayer);
                final String prompt = "（系统：你刚刚被召唤到这个世界，请简短自我介绍并说明你想做什么。"
                        + "下面是当前游戏数据：）\n" + gameData;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                        String reply = cm.chat(prompt, null);
                        // 在主线程执行命令
                        final String finalReply = reply;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                        });
                    } catch (Exception e) {
                        plugin.getLogger().warning("激活 AI 失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("激活 AI 采集数据失败: " + e.getMessage());
            }
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip remove <名字>");
            return;
        }
        boolean ok = plugin.getAiPlayerManager().remove(args[1]);
        sender.sendMessage(ok ? "§a已移除 AI 玩家: " + args[1]
                              : "§c未找到 AI 玩家: " + args[1]);
    }

    private void handleList(CommandSender sender) {
        var all = plugin.getAiPlayerManager().getAll();
        if (all.isEmpty()) {
            sender.sendMessage("§7当前没有 AI 玩家。");
            return;
        }
        sender.sendMessage("§6===== AI 玩家列表 =====");
        for (AIPlayer p : all) {
            var loc = p.getLocation();
            String locStr = loc == null ? "未知" : String.format("%.0f, %.0f, %.0f",
                    loc.getX(), loc.getY(), loc.getZ());
            sender.sendMessage("§e- " + p.getName() + " §7(@" + locStr + ")");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        plugin.reloadAll();
        sender.sendMessage("§a配置已重新加载。");
    }

    private void handleTalk(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /aip talk <名字> <消息>");
            return;
        }
        String name = args[1];
        AIPlayer aiPlayer = plugin.getAiPlayerManager().get(name);
        if (aiPlayer == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + name);
            return;
        }
        String[] msgParts = new String[args.length - 2];
        System.arraycopy(args, 2, msgParts, 0, msgParts.length);
        String msg = String.join(" ", msgParts);

        // 首次激活
        if (!aiPlayer.isActivated()) {
            activateAI(aiPlayer);
        }

        sender.sendMessage("§7你对 §e" + name + "§7 说: " + msg);
        // 先在主线程采集游戏数据，再异步调用 LLM（避免异步访问实体）
        final CommandSender finalSender = sender;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(aiPlayer);
                final String prompt = msg + "\n\n（附当前游戏数据：）\n" + gameData;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                        String reply = cm.chat(prompt, finalSender instanceof Player ? (Player) finalSender : null);
                        // 在主线程执行命令
                        final String finalReply = reply;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                        });
                    } catch (Exception e) {
                        finalSender.sendMessage("§cAI 处理失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                finalSender.sendMessage("§c采集游戏数据失败: " + e.getMessage());
            }
        });
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip reset <名字>");
            return;
        }
        AIPlayer p = plugin.getAiPlayerManager().get(args[1]);
        if (p == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[1]);
            return;
        }
        p.resetHistory();
        sender.sendMessage("§a已重置 " + args[1] + " 的对话历史。");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result = Arrays.asList("spawn", "remove", "list", "reload", "talk", "reset");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("talk") || sub.equals("reset")) {
                result = plugin.getAiPlayerManager().getAll().stream()
                        .map(AIPlayer::getName).collect(Collectors.toList());
            }
        }
        // 过滤已输入的内容
        String prefix = args[args.length - 1].toLowerCase();
        return result.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
    }
}
