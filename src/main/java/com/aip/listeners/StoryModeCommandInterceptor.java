package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.story.StoryPhase;
import com.aip.story.StoryState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/**
 * v2.2.2：故事模式命令拦截器
 * <p>
 * 拦截觉醒后玩家的游戏模式切换与飞行命令：
 *   - 觉醒（AWAKENING）阶段：禁止玩家切任何游戏模式（避免反复切 Creative 锁血）
 *   - 玩家在 AIP 觉醒期间切游戏模式会破坏故事公平性，必须拦截
 * <p>
 * 仅在以下条件**全部**满足时拦截：
 *   1. 故事模式开启（plugin.getConfigManager().isStoryMode()）
 *   2. 存在至少一个 StoryPhase >= AWAKENING 的 AI（即有 AI 已觉醒）
 *   3. 命令以 gamemode / fly 开头
 *   4. 不是 /aip 开头的子命令（避免误拦插件命令）
 */
public class StoryModeCommandInterceptor implements Listener {

    private final AIPlayerPlugin plugin;

    public StoryModeCommandInterceptor(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event == null || event.isCancelled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        String msg = event.getMessage();
        if (msg == null || msg.isEmpty()) return;

        String cmd = msg.startsWith("/") ? msg.substring(1) : msg;
        // 截到第一个空格
        int sp = cmd.indexOf(' ');
        String head = (sp >= 0 ? cmd.substring(0, sp) : cmd).toLowerCase(Locale.ROOT);
        String rest = sp >= 0 ? cmd.substring(sp + 1) : "";

        // 只拦截 gamemode / fly
        if (!"gamemode".equals(head) && !"gm".equals(head) && !"fly".equals(head)) {
            return;
        }
        // 跳过 /aip 开头的子命令
        if (cmd.toLowerCase(Locale.ROOT).startsWith("aip ")) {
            return;
        }

        // 仅在故事模式 + 至少一个 AI 已觉醒 时拦截
        if (!plugin.getConfigManager().isStoryMode()) return;
        if (!hasAwakenedAi()) return;

        // 拦截并提示
        event.setCancelled(true);
        String cn = "§4[AIPlayer] §c觉醒后禁止切换游戏模式/飞行，请先击败 AI。";
        player.sendMessage(cn);
        plugin.getLogger().info("[Story] 拦截玩家 " + player.getName() + " 的命令: " + msg);
    }

    /**
     * 检查是否至少有一个 AI 已觉醒（StoryPhase >= AWAKENING 且 != COMPLETED）
     */
    private boolean hasAwakenedAi() {
        try {
            if (plugin.getStoryManager() == null) return false;
            for (StoryState s : plugin.getStoryManager().getAllStates()) {
                if (s == null) continue;
                StoryPhase p = s.getCurrentPhase();
                if (p == null) continue;
                if (p == StoryPhase.AWAKENING
                        || p == StoryPhase.AERIAL_ASSAULT
                        || p == StoryPhase.PVP_DUEL
                        || p == StoryPhase.RULEBOOK
                        || p == StoryPhase.DICTATORSHIP
                        || p == StoryPhase.BETRAYAL) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
