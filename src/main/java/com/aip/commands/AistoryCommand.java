package com.aip.commands;

import com.aip.AIPlayerPlugin;
import com.aip.story.StoryPhase;
import com.aip.story.StoryState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * v2.2.9：火柴盒故事独立命令 /aistory（别名 /aiquest）
 * <p>
 * 探索逃跑版 12 章节剧情。用法：
 *   /aistory                  —— 开启故事（需 aip.admin 权限）
 *   /aistory exit             —— 退出故事（仅章节 1-3 可中途退出）
 *   /aistory status           —— 查看当前章节与剩余时间
 * <p>
 * 设计：
 *   - 独立于 /aip 命令，方便玩家在聊天框直接触发
 *   - 故事逻辑委托给 {@link com.aip.story.StoryManager}
 */
public class AistoryCommand implements CommandExecutor, TabCompleter {

    private final AIPlayerPlugin plugin;

    public AistoryCommand(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令必须由玩家执行。");
            return true;
        }
        if (!player.isOp() && !player.hasPermission("aip.admin")) {
            player.sendMessage("§c你没有权限执行此操作。");
            return true;
        }

        if (args.length == 0) {
            handleStart(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "exit", "quit", "leave" -> handleExit(player);
            case "status", "info" -> handleStatus(player);
            case "start", "begin" -> handleStart(player);
            default -> sendHelp(player);
        }
        return true;
    }

    /**
     * 开启故事
     */
    private void handleStart(Player player) {
        UUID uuid = player.getUniqueId();
        StoryState s = plugin.getStoryManager().getOrCreateState(uuid);
        if (s.isStoryStarted() && !s.isStoryCompleted()) {
            player.sendMessage("§c故事正在进行中。/aistory status 查看当前章节。");
            return;
        }
        if (s.isStoryCompleted()) {
            player.sendMessage("§c你已经看过了这个故事的结局。重启服务器或重置状态后才能再玩。");
            return;
        }
        // 记录原始位置（用于 exit 时传送回去）
        s.setStoryStartTime(System.currentTimeMillis());
        boolean ok = plugin.getStoryManager().startStory(player);
        if (!ok) {
            player.sendMessage("§c无法开启故事，请稍后再试。");
            return;
        }
        player.sendMessage("§6[AI 故事] §a火柴盒故事已开启！/aistory status 查看当前章节。");
        player.sendMessage("§7第一章节：火柴盒。Eve 和 Mr. Sparkle 正在火柴盒外等待…");
    }

    /**
     * 退出故事（仅章节 1-3 可中途退出）
     */
    private void handleExit(Player player) {
        UUID uuid = player.getUniqueId();
        StoryState s = plugin.getStoryManager().getState(uuid);
        if (s == null || !s.isStoryStarted()) {
            player.sendMessage("§c你还没有开启故事。");
            return;
        }
        StoryPhase p = s.getCurrentPhase();
        // 仅 CHAPTER_1/2/3 可中途退出；章节 4+ 不可退出（玩家已卷入剧情）
        if (p != StoryPhase.CHAPTER_1_MATCH_HOUSE
                && p != StoryPhase.CHAPTER_2_DOOR_KNOCK
                && p != StoryPhase.CHAPTER_3_AI_VISITOR) {
            player.sendMessage("§c故事无法中途退出。你已经进入 §4" + p.getDisplayName() + "§c 阶段。");
            return;
        }
        // 退出：标记完成 + 传送回原位置
        s.setStoryCompleted(true);
        player.sendMessage("§6[AI 故事] §e已退出故事。");
        // 重置状态以便下次重玩
        s.reset();
    }

    /**
     * 查看当前章节与剩余时间
     */
    private void handleStatus(Player player) {
        UUID uuid = player.getUniqueId();
        StoryState s = plugin.getStoryManager().getState(uuid);
        if (s == null || !s.isStoryStarted()) {
            player.sendMessage("§7你还没有开启故事。/aistory 即可开始。");
            return;
        }
        StoryPhase current = s.getCurrentPhase();
        int remain = current.getDurationSeconds() - s.getElapsedSeconds();
        if (remain < 0) remain = 0;
        player.sendMessage("§6[AI 故事] §e" + current.getDisplayName()
                + " §7(剩余 " + remain + " 秒)");
        if (s.isStoryCompleted()) {
            player.sendMessage("§7状态：§a已完成 §7（你已看完结局）");
        } else if (s.getChosenEnding() != null) {
            player.sendMessage("§7已选结局：§f" + s.getChosenEnding());
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6===== /aistory 用法 =====");
        player.sendMessage("§e/aistory §7- 开启火柴盒探索逃跑版故事（12 章节）");
        player.sendMessage("§e/aistory exit §7- 退出故事（仅章节 1-3）");
        player.sendMessage("§e/aistory status §7- 查看当前章节与剩余时间");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Arrays.asList("start", "exit", "status").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
