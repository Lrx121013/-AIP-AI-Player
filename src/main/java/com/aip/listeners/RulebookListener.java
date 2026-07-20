package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * v2.2.7：监听"AI 制度之书"阅读事件（已废弃）
 * <p>
 * 火柴盒版移除了 Rulebook 阶段（书与笔的制度统治玩法）。
 * 本类保留为占位 Listener，检测到玩家阅读"AI 制度之书"时仅打印日志，
 * 不再推进任何故事阶段（由 StoryManager.tickChapter 周期任务独立驱动）。
 */
public class RulebookListener implements Listener {

    private final AIPlayerPlugin plugin;

    public RulebookListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        // v2.2.7 stub：原 StoryManager.onRulebookRead / state.isRulebookDelivered / isRulebookRead 已废弃
        if (!isAiRulebook(event.getPlayer().getInventory().getItemInMainHand())
                && !isAiRulebook(event.getPlayer().getInventory().getItemInOffHand())) {
            return;
        }
        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("检测到玩家 " + event.getPlayer().getName() + " 阅读 AI 制度之书（v2.2.7 占位）");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != Material.WRITTEN_BOOK) return;
        if (!isAiRulebook(hand)) return;
        // v2.2.7 stub：仅记录日志
        Player reader = event.getPlayer();
        if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("检测到玩家 " + reader.getName() + " 右击 AI 制度之书（v2.2.7 占位）");
        }
    }

    /**
     * 判断物品是否是"AI 制度之书"
     */
    private boolean isAiRulebook(ItemStack book) {
        if (book == null || !book.hasItemMeta()) return false;
        if (book.getItemMeta() instanceof BookMeta meta) {
            String title = meta.getTitle();
            if (title != null && title.contains("AI 制度之书")) return true;
            String author = meta.getAuthor();
            if (author != null && author.contains("Evil AI")) return true;
        }
        return false;
    }
}
