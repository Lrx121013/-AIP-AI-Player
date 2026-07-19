package com.aip.commands;

import com.aip.AIPlayerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /k 命令处理器：打开 AIPlayer GUI
 */
public class GuiCommand implements CommandExecutor, TabCompleter {

    private final AIPlayerPlugin plugin;

    public GuiCommand(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aip.gui")) {
            sender.sendMessage("§c你没有权限使用此功能");
            return true;
        }
        if (!(sender instanceof Player player)) {
            return false;
        }
        plugin.getGuiManager().openPlayerList(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
