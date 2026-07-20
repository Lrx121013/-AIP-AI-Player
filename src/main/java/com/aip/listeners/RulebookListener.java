package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import org.bukkit.Bukkit;
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

import java.util.Map;

/**
 * 监听玩家阅读"AI 制度之书"事件
 * <p>
 * 玩家在书与笔界面签名（PlayerEditBookEvent）或手持已签名的书右击（PlayerInteractEvent）时触发。
 * 检测书的标题是否是"AI 制度之书"，若是则调用 StoryManager.onRulebookRead 推进阶段。
 */
public class RulebookListener implements Listener {

    private final AIPlayerPlugin plugin;

    public RulebookListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 优先方案：监听 PlayerEditBookEvent（玩家在书与笔界面签名后）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Player reader = event.getPlayer();
        ItemStack hand = reader.getInventory().getItemInMainHand();
        if (hand.getType() != Material.WRITTEN_BOOK) {
            ItemStack off = reader.getInventory().getItemInOffHand();
            if (off.getType() != Material.WRITTEN_BOOK) return;
            hand = off;
        }
        if (!isAiRulebook(hand)) return;

        AIPlayer ai = findAiByRulebook();
        if (ai == null) return;

        try {
            plugin.getStoryManager().onRulebookRead(ai, reader);
        } catch (Exception e) {
            plugin.getLogger().warning("RulebookListener 通知 StoryManager 失败: " + e.getMessage());
        }
    }

    /**
     * Fallback 方案：监听 PlayerInteractEvent（手持已签名书右击）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != Material.WRITTEN_BOOK) return;
        if (!isAiRulebook(hand)) return;

        Player reader = event.getPlayer();
        // 1 秒内同一玩家只触发一次
        long now = System.currentTimeMillis();
        Long last = lastTrigger.get(reader.getUniqueId());
        if (last != null && now - last < 1000L) return;
        lastTrigger.put(reader.getUniqueId(), now);

        AIPlayer ai = findAiByRulebook();
        if (ai == null) return;

        try {
            plugin.getStoryManager().onRulebookRead(ai, reader);
        } catch (Exception e) {
            plugin.getLogger().warning("RulebookListener(PlayerInteractEvent) 失败: " + e.getMessage());
        }
    }

    private final Map<java.util.UUID, Long> lastTrigger = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 判断物品是否是"AI 制度之书"
     */
    private boolean isAiRulebook(ItemStack book) {
        if (book.getItemMeta() instanceof BookMeta meta) {
            String title = meta.getTitle();
            if (title != null && title.contains("AI 制度之书")) return true;
            String author = meta.getAuthor();
            if (author != null && author.contains("Evil AI")) return true;
        }
        return false;
    }

    /**
     * 找到持有 rulebookDelivered=true 的 AI（最近一个）
     */
    private AIPlayer findAiByRulebook() {
        for (AIPlayer ai : plugin.getAiPlayerManager().getAll()) {
            com.aip.story.StoryState state = ai.getStoryState();
            if (state != null && state.isRulebookDelivered() && !state.isRulebookRead()) {
                return ai;
            }
        }
        return null;
    }
}
