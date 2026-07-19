package com.aip.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 用于标识 AIPlayer GUI 的 InventoryHolder。
 * 通过 instanceof 判断而非标题匹配，避免 i18n / 颜色码误判。
 */
public class AIPlayerGuiHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() { return null; }
}
