package com.aip.ai;

import com.aip.config.ConfigManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 游戏数据采集器：收集 AI 玩家周围的游戏资源信息
 */
public class GameDataCollector {

    private final ConfigManager config;

    public GameDataCollector(ConfigManager config) {
        this.config = config;
    }

    /**
     * 采集 AI 玩家当前游戏数据并格式化为文本
     */
    public String collect(AIPlayer aiPlayer) {
        Villager entity = aiPlayer.getEntity();
        if (entity == null) return "（无法获取实体）";

        StringBuilder sb = new StringBuilder();
        Location loc = entity.getLocation();
        World world = loc.getWorld();

        // 1. 基本信息
        sb.append("=== 游戏数据 ===\n");
        sb.append(String.format("名称: %s\n", aiPlayer.getName()));
        sb.append(String.format("坐标: x=%.1f, y=%.1f, z=%.1f (世界: %s)\n",
                loc.getX(), loc.getY(), loc.getZ(), world.getName()));
        sb.append(String.format("朝向: yaw=%.1f, pitch=%.1f\n", loc.getYaw(), loc.getPitch()));
        sb.append(String.format("区块: chunk=%d,%d\n", loc.getChunk().getX(), loc.getChunk().getZ()));
        double maxHealth = 20.0;
        try {
            org.bukkit.attribute.Attribute maxHealthAttr = resolveAttribute("max_health");
            if (maxHealthAttr != null) {
                var attr = entity.getAttribute(maxHealthAttr);
                if (attr != null) maxHealth = attr.getValue();
            }
        } catch (Throwable ignored) {
        }
        sb.append(String.format("血量: %.1f/%.1f\n", entity.getHealth(), maxHealth));

        // 2. 时间天气
        long time = world.getTime();
        sb.append(String.format("游戏时间: %d (=%s)\n", time, formatTime(time)));
        sb.append(String.format("白天/黑夜: %s\n", (time < 12300 || time > 23850) ? "白天" : "黑夜"));
        sb.append(String.format("天气: %s\n", world.hasStorm() ? (world.isThundering() ? "雷雨" : "下雨") : "晴朗"));
        sb.append(String.format("难度: %s\n", world.getDifficulty().name()));

        // 3. 装备
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            sb.append("装备: ");
            sb.append("手持=").append(itemName(eq.getItemInMainHand()));
            sb.append(", 头=").append(itemName(eq.getHelmet()));
            sb.append(", 胸=").append(itemName(eq.getChestplate()));
            sb.append(", 腿=").append(itemName(eq.getLeggings()));
            sb.append(", 脚=").append(itemName(eq.getBoots()));
            sb.append("\n");
        }

        // 4. 附近方块
        sb.append("附近重要方块 (半径=").append(config.getScanRadius()).append("):\n");
        sb.append(scanNearbyBlocks(loc, config.getScanRadius()));

        // 5. 附近实体
        sb.append("附近实体 (半径=").append(config.getEntityScanRadius()).append("):\n");
        sb.append(scanNearbyEntities(entity, config.getEntityScanRadius()));

        // 6. 附近玩家
        sb.append("附近玩家:\n");
        sb.append(scanNearbyPlayers(entity, config.getEntityScanRadius()));

        // 7. 正面方块（视线前方）
        sb.append("视线前方方块:\n");
        Block target = entity.getTargetBlockExact(5);
        if (target != null && !target.getType().isAir()) {
            sb.append(String.format("  方块=%s @ %d,%d,%d\n",
                    target.getType().name(), target.getX(), target.getY(), target.getZ()));
        } else {
            sb.append("  (无)\n");
        }

        sb.append("=== 数据结束 ===\n");
        return sb.toString();
    }

    private String formatTime(long ticks) {
        // 0 ticks = 06:00
        long hours = ((ticks / 1000) + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * 兼容 1.21.3+ 新 Attribute API：通过 Registry 查找属性
     */
    private org.bukkit.attribute.Attribute resolveAttribute(String key) {
        try {
            return org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft(key));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String itemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "无";
        return item.getType().name() + (item.getAmount() > 1 ? "x" + item.getAmount() : "");
    }

    private String scanNearbyBlocks(Location center, int radius) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        int maxCount = 30;
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = (int) center.getY();
        int cz = center.getBlockZ();

        // 扫描脚下方块、四周方块
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -radius; dx <= radius && count < maxCount; dx++) {
                for (int dz = -radius; dz <= radius && count < maxCount; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    Material type = b.getType();
                    // 只列出"重要"方块：非空气、非草、非石头（除非是矿石）
                    if (type.isAir()) continue;
                    if (type == Material.GRASS_BLOCK || type == Material.DIRT || type == Material.STONE
                            || type == Material.SAND || type == Material.WATER) continue;
                    if (type.name().contains("ORE") || type.name().contains("LOG") || type.name().contains("LEAVES")
                            || type.name().contains("PLANKS")
                            || type == Material.CHEST || type == Material.CRAFTING_TABLE || type == Material.FURNACE
                            || type == Material.TORCH || type == Material.COBBLESTONE
                            || type.isSolid()) {
                        sb.append(String.format("  %s @ %d,%d,%d\n", type.name(),
                                b.getX(), b.getY(), b.getZ()));
                        count++;
                    }
                }
            }
        }
        if (count == 0) sb.append("  (周围只有普通地形)\n");
        return sb.toString();
    }

    private String scanNearbyEntities(LivingEntity self, int radius) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        List<Entity> nearby = self.getNearbyEntities(radius, radius, radius);
        for (Entity e : nearby) {
            if (e.equals(self)) continue;
            String name = e.getName();
            Location l = e.getLocation();
            double dist = self.getLocation().distance(l);
            sb.append(String.format("  %s (距离 %.1f, %s) @ %d,%d,%d\n",
                    name, dist, e.getType().name(), l.getBlockX(), l.getBlockY(), l.getBlockZ()));
            count++;
            if (count >= 15) break;
        }
        if (count == 0) sb.append("  (附近无实体)\n");
        return sb.toString();
    }

    private String scanNearbyPlayers(LivingEntity self, int radius) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Player p : self.getWorld().getPlayers()) {
            if (p.getLocation().distance(self.getLocation()) > radius) continue;
            Location l = p.getLocation();
            sb.append(String.format("  %s (血量 %.1f, 手持 %s) @ %d,%d,%d\n",
                    p.getName(), p.getHealth(),
                    itemName(p.getInventory().getItemInMainHand()),
                    l.getBlockX(), l.getBlockY(), l.getBlockZ()));
            count++;
        }
        if (count == 0) sb.append("  (附近无玩家)\n");
        return sb.toString();
    }
}
