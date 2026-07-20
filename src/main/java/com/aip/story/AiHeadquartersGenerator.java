package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * v2.2.7：AI 总部生成器（章节 8 用）
 * <p>
 * 巨型铁门后是 50x50 房间（章节 8 真相揭露）：
 *   - 中央 9x9 红石灯屏幕墙（模拟监控画面）
 *   - 屏幕用红石灯明暗交替显示"玩家在火柴盒里"
 *   - 中央控制台（附魔台）
 */
public class AiHeadquartersGenerator {
    public static final int SIZE = 50;
    public static final int HEIGHT = 12;

    public static Location generate(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        int sx = origin.getBlockX() - SIZE / 2;
        int sy = origin.getBlockY();
        int sz = origin.getBlockZ() - SIZE / 2;

        // 清空
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    world.getBlockAt(sx + x, sy + y, sz + z).setType(Material.AIR);
                }
            }
        }

        // 地板（铁块）
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                world.getBlockAt(sx + x, sy, sz + z).setType(Material.IRON_BLOCK);
            }
        }
        // 天花板
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                world.getBlockAt(sx + x, sy + HEIGHT - 1, sz + z).setType(Material.IRON_BLOCK);
            }
        }
        // 墙
        for (int x = 0; x < SIZE; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                world.getBlockAt(sx + x, sy + y, sz).setType(Material.IRON_BLOCK);
                world.getBlockAt(sx + x, sy + y, sz + SIZE - 1).setType(Material.IRON_BLOCK);
            }
        }
        for (int z = 0; z < SIZE; z++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                world.getBlockAt(sx, sy + y, sz + z).setType(Material.IRON_BLOCK);
                world.getBlockAt(sx + SIZE - 1, sy + y, sz + z).setType(Material.IRON_BLOCK);
            }
        }

        // 9x9 监控屏幕墙（北墙，z=0）：用红石灯模拟监控画面
        int screenStartX = sx + SIZE / 2 - 5;
        int screenStartY = sy + 2;
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                // 奇偶交替（模拟不同的监控画面）
                Block b = world.getBlockAt(screenStartX + x, screenStartY + y, sz + 1);
                b.setType(Material.REDSTONE_LAMP);
                org.bukkit.block.data.Lightable lamp = (org.bukkit.block.data.Lightable) b.getBlockData();
                lamp.setLit(((x + y) % 2 == 0));
                b.setBlockData(lamp);
            }
        }

        // 中央控制台（附魔台）
        world.getBlockAt(sx + SIZE / 2, sy + 1, sz + SIZE / 2).setType(Material.ENCHANTING_TABLE);

        // 火把（4 角）—— 提高亮度
        world.getBlockAt(sx + 5, sy + 5, sz + 5).setType(Material.TORCH);
        world.getBlockAt(sx + SIZE - 5, sy + 5, sz + 5).setType(Material.TORCH);
        world.getBlockAt(sx + 5, sy + 5, sz + SIZE - 5).setType(Material.TORCH);
        world.getBlockAt(sx + SIZE - 5, sy + 5, sz + SIZE - 5).setType(Material.TORCH);

        return new Location(world, sx + SIZE / 2.0, sy + 1, sz + SIZE - 5.0);
    }

    public static void clear(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        World world = origin.getWorld();
        int sx = origin.getBlockX() - SIZE / 2;
        int sy = origin.getBlockY();
        int sz = origin.getBlockZ() - SIZE / 2;
        for (int x = -5; x < SIZE + 5; x++) {
            for (int y = -2; y < HEIGHT + 5; y++) {
                for (int z = -5; z < SIZE + 5; z++) {
                    Block b = world.getBlockAt(sx + x, sy + y, sz + z);
                    if (b.getType() != Material.BEDROCK) b.setType(Material.AIR);
                }
            }
        }
    }
}
