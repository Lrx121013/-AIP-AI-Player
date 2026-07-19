package com.aip.listeners;

import com.aip.AIPlayerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * GUI 交互监听器：处理快捷键 K 和 GUI 点击事件
 */
public class GuiListener implements Listener {

    private final AIPlayerPlugin plugin;

    public GuiListener(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 快捷键 K 打开 AI 玩家列表
     * <p>
     * 通过检测玩家发送的 "/k" 命令来实现（Paper 的 PlayerCommandPreprocessEvent 在命令被解析前触发）
     */
    @EventHandler
    public void onKeyPress(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().equalsIgnoreCase("/k")) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("aip.admin")) {
            player.sendMessage("§c你没有权限使用此功能");
            return;
        }
        event.setCancelled(true);
        plugin.getGuiManager().openPlayerList(player);
    }

    /**
     * 处理 GUI 点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§6")) return;

        if (!(event.getWhoClicked() instanceof Player)) return;

        plugin.getGuiManager().handleClick(event);
    }

    /**
     * 玩家加入时提示快捷键
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("aip.admin")) {
            player.sendMessage("§6[AIPlayer] §7按 §eK §7键打开 AI 玩家管理界面");
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
