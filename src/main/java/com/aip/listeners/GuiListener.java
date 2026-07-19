package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import com.aip.gui.AIPlayerGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * GUI 交互监听器：处理 GUI 点击事件
 */
public class GuiListener implements Listener {

    private final AIPlayerPlugin plugin;

    public GuiListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 处理 GUI 点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AIPlayerGuiHolder)) return;
        event.setCancelled(true);  // 立即取消，防止 shift-click
        if (!(event.getWhoClicked() instanceof Player)) return;
        plugin.getGuiManager().handleClick(event);
    }

    /**
     * 玩家加入时提示命令
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("aip.gui")) {
            player.sendMessage("§6[AIPlayer] §7输入 §e/k §7打开 AI 玩家管理界面");
        }
    }

    /**
     * 玩家退出时清理 GUI 状态
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGuiManager().close(event.getPlayer());
    }
}
