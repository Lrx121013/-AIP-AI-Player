package com.aip.gui;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.NpcHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI 管理器：管理所有 AIPlayer GUI 界面
 * <p>
 * 快捷键 K 打开主界面。
 */
public class GuiManager {

    private final AIPlayerPlugin plugin;
    private final Map<UUID, GuiType> openGui = new HashMap<>();

    public GuiManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public enum GuiType {
        PLAYER_LIST,
        ACTION_MENU,
        SKIN_MENU
    }

    /** 打开主界面（AI 玩家列表） */
    public void openPlayerList(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, "§6AI 玩家列表");

        int slot = 0;
        for (AIPlayer ai : plugin.getAiPlayerManager().getAll()) {
            if (slot >= 45) break;
            Player entity = ai.getEntity();
            String status = entity != null && entity.isValid() ? "§a在线" : "§c离线";
            String health = entity != null ? String.format("%.0f", entity.getHealth()) : "?";

            ItemStack item = new ItemStack(entity != null && !entity.isInvisible()
                    ? Material.PLAYER_HEAD : Material.SKELETON_SKULL);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + ai.getName());
            meta.setLore(Arrays.asList(
                    "§7状态: " + status,
                    "§7血量: " + health + "/20",
                    "§7UUID: " + ai.getEntityId().toString().substring(0, 8) + "...",
                    "",
                    "§6右键: 打开动作菜单",
                    "§c左键: 移除"
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // 填充空位
        for (; slot < 45; slot++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fm = filler.getItemMeta();
            fm.setDisplayName("§7");
            filler.setItemMeta(fm);
            inv.setItem(slot, filler);
        }

        // 底部功能按钮
        ItemStack addBtn = createButton(Material.EMERALD_BLOCK, "§a生成 AI 玩家",
                Arrays.asList("§7点击输入名字"));
        inv.setItem(45, addBtn);

        ItemStack reloadBtn = createButton(Material.COMMAND_BLOCK, "§6重新加载配置",
                Arrays.asList("§7重新加载 config.yml"));
        inv.setItem(46, reloadBtn);

        ItemStack closeBtn = createButton(Material.BARRIER, "§c关闭",
                Arrays.asList("§7按 ESC 关闭"));
        inv.setItem(53, closeBtn);

        player.openInventory(inv);
        openGui.put(player.getUniqueId(), GuiType.PLAYER_LIST);
    }

    /** 打开动作菜单 */
    public void openActionMenu(Player player, AIPlayer aiPlayer) {
        Inventory inv = Bukkit.createInventory(player, 45, "§6" + aiPlayer.getName() + " - 动作菜单");

        int slot = 0;
        // 移动类
        inv.setItem(slot++, createButton(Material.IRON_BOOTS, "§e走到我身边", Arrays.asList("[COMMAND:approach]")));
        inv.setItem(slot++, createButton(Material.LEAD, "§e跟随我", Arrays.asList("[COMMAND:follow]")));
        inv.setItem(slot++, createButton(Material.WOODEN_SWORD, "§e停止移动", Arrays.asList("[COMMAND:stop]")));
        inv.setItem(slot++, createButton(Material.ELYTRA, "§e跳跃", Arrays.asList("[COMMAND:jump]")));

        // 战斗类
        inv.setItem(slot++, createButton(Material.DIAMOND_SWORD, "§c攻击最近怪物", Arrays.asList("[COMMAND:attack nearest]")));

        // 姿态类
        inv.setItem(slot++, createButton(Material.WHITE_WOOL, "§e坐下", Arrays.asList("[COMMAND:sit]")));
        inv.setItem(slot++, createButton(Material.RED_BED, "§e睡觉", Arrays.asList("[COMMAND:sleep]")));
        inv.setItem(slot++, createButton(Material.LEATHER_BOOTS, "§e潜行", Arrays.asList("[COMMAND:sneak]")));
        inv.setItem(slot++, createButton(Material.STONE_SLAB, "§e站立", Arrays.asList("[COMMAND:stand]")));

        // 动作类
        inv.setItem(slot++, createButton(Material.STICK, "§e挥手", Arrays.asList("[COMMAND:wave]")));
        inv.setItem(slot++, createButton(Material.GOLD_INGOT, "§e跳舞", Arrays.asList("[COMMAND:dance]")));
        inv.setItem(slot++, createButton(Material.SHIELD, "§e挥动手臂", Arrays.asList("[COMMAND:swing]")));

        // 物品类
        inv.setItem(slot++, createButton(Material.CHEST, "§e捡起掉落物", Arrays.asList("[COMMAND:pickup]")));
        inv.setItem(slot++, createButton(Material.DROPPER, "§e丢出物品", Arrays.asList("[COMMAND:throw_item]")));
        inv.setItem(slot++, createButton(Material.APPLE, "§e吃东西", Arrays.asList("[COMMAND:eat]")));

        // 方块类
        inv.setItem(slot++, createButton(Material.DIAMOND_PICKAXE, "§e挖脚下方块", Arrays.asList("[COMMAND:break 脚下]")));

        // 换皮肤
        inv.setItem(slot++, createButton(Material.PLAYER_HEAD, "§6换皮肤", Arrays.asList("点击打开皮肤菜单")));

        // 移除
        inv.setItem(slot++, createButton(Material.LAVA_BUCKET, "§4移除 AI 玩家", Arrays.asList("危险！将永久删除")));

        // 填充
        for (; slot < 45; slot++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fm = filler.getItemMeta();
            fm.setDisplayName("§7");
            filler.setItemMeta(fm);
            inv.setItem(slot, filler);
        }

        player.openInventory(inv);
        openGui.put(player.getUniqueId(), GuiType.ACTION_MENU);
    }

    /** 打开皮肤菜单 */
    public void openSkinMenu(Player player, AIPlayer aiPlayer) {
        Inventory inv = Bukkit.createInventory(player, 27, "§6" + aiPlayer.getName() + " - 换皮肤");

        int slot = 0;
        // 复制在线玩家皮肤
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 18) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName("§e" + online.getName());
            meta.setLore(Arrays.asList("§7点击复制此玩家皮肤"));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        // 填充空位
        for (; slot < 18; slot++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fm = filler.getItemMeta();
            fm.setDisplayName("§7");
            filler.setItemMeta(fm);
            inv.setItem(slot, filler);
        }

        // 底部按钮
        ItemStack urlBtn = createButton(Material.PAPER, "§6通过 URL 设置",
                Arrays.asList("§7输入: skinurl:<URL>"));
        inv.setItem(18, urlBtn);

        ItemStack backBtn = createButton(Material.ARROW, "§e返回",
                Arrays.asList("§7返回动作菜单"));
        inv.setItem(26, backBtn);

        player.openInventory(inv);
        openGui.put(player.getUniqueId(), GuiType.SKIN_MENU);
    }

    /** 处理 GUI 点击事件 */
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        event.setCancelled(true);

        String name = clicked.getItemMeta().getDisplayName();
        if (name == null || name.isEmpty()) return;

        GuiType type = openGui.get(player.getUniqueId());
        if (type == null) return;

        switch (type) {
            case PLAYER_LIST -> handlePlayerListClick(player, clicked, event);
            case ACTION_MENU -> handleActionMenuClick(player, clicked);
            case SKIN_MENU -> handleSkinMenuClick(player, clicked);
        }
    }

    private void handlePlayerListClick(Player player, ItemStack clicked, InventoryClickEvent event) {
        String name = clicked.getItemMeta().getDisplayName().replace("§e", "").replace("§6", "").trim();

        if (clicked.getType() == Material.EMERALD_BLOCK) {
            player.closeInventory();
            plugin.getAiPlayerManager().spawn("AI" + System.currentTimeMillis(), player);
            player.sendMessage("§a已生成 AI 玩家: AI" + System.currentTimeMillis());
            return;
        }

        if (clicked.getType() == Material.COMMAND_BLOCK) {
            plugin.reloadAll();
            player.sendMessage("§a配置已重新加载");
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        AIPlayer ai = plugin.getAiPlayerManager().get(name);
        if (ai == null) return;

        if (event.isLeftClick()) {
            plugin.getAiPlayerManager().remove(name);
            player.sendMessage("§c已移除: " + name);
            openPlayerList(player);
        } else {
            openActionMenu(player, ai);
        }
    }

    private void handleActionMenuClick(Player player, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName().replace("§e", "").replace("§c", "")
                .replace("§6", "").replace("§a", "").replace("§4", "").trim();

        // 获取当前选中的 AI 玩家（从 inventory 标题提取）
        String aiName = player.getOpenInventory().getTitle().replace("§6", "").replace(" - 动作菜单", "").trim();
        AIPlayer ai = plugin.getAiPlayerManager().get(aiName);
        if (ai == null) {
            player.sendMessage("§cAI 玩家不存在");
            return;
        }

        switch (name) {
            case "走到我身边" -> executeCommand(ai, "[COMMAND:approach " + player.getName() + "]");
            case "跟随我" -> executeCommand(ai, "[COMMAND:follow " + player.getName() + "]");
            case "停止移动" -> executeCommand(ai, "[COMMAND:stop]");
            case "跳跃" -> executeCommand(ai, "[COMMAND:jump]");
            case "攻击最近怪物" -> executeCommand(ai, "[COMMAND:attack nearest]");
            case "坐下" -> executeCommand(ai, "[COMMAND:sit]");
            case "睡觉" -> executeCommand(ai, "[COMMAND:sleep]");
            case "潜行" -> executeCommand(ai, "[COMMAND:sneak]");
            case "站立" -> executeCommand(ai, "[COMMAND:stand]");
            case "挥手" -> executeCommand(ai, "[COMMAND:wave]");
            case "跳舞" -> executeCommand(ai, "[COMMAND:dance]");
            case "挥动手臂" -> executeCommand(ai, "[COMMAND:swing]");
            case "捡起掉落物" -> executeCommand(ai, "[COMMAND:pickup]");
            case "丢出物品" -> executeCommand(ai, "[COMMAND:throw_item]");
            case "吃东西" -> executeCommand(ai, "[COMMAND:eat]");
            case "挖脚下方块" -> {
                Player p = ai.getEntity();
                if (p != null) {
                    Location loc = p.getLocation();
                    executeCommand(ai, "[COMMAND:break " + loc.getBlockX() + " " + (int) loc.getY() + " " + loc.getBlockZ() + "]");
                }
            }
            case "换皮肤" -> openSkinMenu(player, ai);
            case "移除 AI 玩家" -> {
                plugin.getAiPlayerManager().remove(aiName);
                player.sendMessage("§c已移除: " + aiName);
                openPlayerList(player);
            }
        }
    }

    private void handleSkinMenuClick(Player player, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName().replace("§e", "").replace("§6", "").replace("§a", "").trim();

        String aiName = player.getOpenInventory().getTitle().replace("§6", "").replace(" - 换皮肤", "").trim();
        AIPlayer ai = plugin.getAiPlayerManager().get(aiName);
        if (ai == null) return;

        if (name.equals("通过 URL 设置")) {
            player.closeInventory();
            player.sendMessage("§e请输入: /aip skin " + aiName + " skinurl:<URL>");
            return;
        }

        if (name.equals("返回")) {
            openActionMenu(player, ai);
            return;
        }

        // 复制在线玩家皮肤
        Player target = Bukkit.getPlayerExact(name);
        if (target != null) {
            try {
                Object skin = NpcHelper.getSkinFromPlayer(target);
                if (skin != null) {
                    plugin.getAiPlayerManager().setSkin(aiName, skin);
                    player.sendMessage("§a已复制 " + name + " 的皮肤");
                } else {
                    player.sendMessage("§c该玩家没有皮肤");
                }
            } catch (Exception e) {
                player.sendMessage("§c换皮肤失败: " + e.getMessage());
            }
        }
    }

    private void executeCommand(AIPlayer ai, String cmd) {
        plugin.getCommandExecutor().execute(ai, cmd);
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void close(Player player) {
        openGui.remove(player.getUniqueId());
    }
}
