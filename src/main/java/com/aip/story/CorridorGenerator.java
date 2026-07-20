package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * v2.2.7：走廊生成器（章节 6-7 用）
 * <p>
 * 长 20x3x100 走廊（东西走向）：
 *   - 地板：黑曜石
 *   - 墙：深色橡木
 *   - 天花板：黑曜石
 *   - 两侧各有 10 扇门（每 10 块一扇）
 *   - 尽头（西侧）巨大铁门
 */
public class CorridorGenerator {
    public static final int LENGTH = 100;
    public static final int WIDTH = 3;
    public static final int HEIGHT = 5;

    /**
     * 在 origin 开始生成走廊（向东延伸）
     * @param origin 走廊西端点
     * @return 走廊出口（东端）
     */
    public static Location generate(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        int sx = origin.getBlockX();
        int sy = origin.getBlockY();
        int sz = origin.getBlockZ();

        // 清空区域
        for (int x = 0; x < LENGTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < WIDTH; z++) {
                    Block b = world.getBlockAt(sx + x, sy + y, sz + z);
                    b.setType(Material.AIR);
                }
            }
        }

        // 地板
        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < WIDTH; z++) {
                world.getBlockAt(sx + x, sy, sz + z).setType(Material.OBSIDIAN);
            }
        }
        // 天花板
        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < WIDTH; z++) {
                world.getBlockAt(sx + x, sy + HEIGHT - 1, sz + z).setType(Material.OBSIDIAN);
            }
        }
        // 墙（南北）
        for (int x = 0; x < LENGTH; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                world.getBlockAt(sx + x, sy + y, sz).setType(Material.DARK_OAK_PLANKS);
                world.getBlockAt(sx + x, sy + y, sz + WIDTH - 1).setType(Material.DARK_OAK_PLANKS);
            }
        }

        // 两侧门：每 10 块一扇（5 + 15 + 25 + ... + 95）
        for (int i = 10; i < LENGTH - 5; i += 10) {
            // 北墙门（z=0）
            world.getBlockAt(sx + i, sy + 1, sz).setType(Material.AIR);
            world.getBlockAt(sx + i, sy + 2, sz).setType(Material.AIR);
            // 南墙门（z=WIDTH-1）
            world.getBlockAt(sx + i, sy + 1, sz + WIDTH - 1).setType(Material.AIR);
            world.getBlockAt(sx + i, sy + 2, sz + WIDTH - 1).setType(Material.AIR);
        }

        // 尽头巨大铁门（西侧，x=-1）
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < WIDTH; z++) {
                world.getBlockAt(sx - 1, sy + y, sz + z).setType(Material.IRON_DOOR);
            }
        }

        // 走廊入口（东侧，x=LENGTH）
        // 不封，玩家直接走出去

        return new Location(world, sx + LENGTH - 5.0, sy + 1, sz + 1.5);
    }

    public static void clear(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        World world = origin.getWorld();
        int sx = origin.getBlockX();
        int sy = origin.getBlockY();
        int sz = origin.getBlockZ();
        for (int x = -5; x < LENGTH + 5; x++) {
            for (int y = -2; y < HEIGHT + 5; y++) {
                for (int z = -5; z < WIDTH + 5; z++) {
                    Block b = world.getBlockAt(sx + x, sy + y, sz + z);
                    if (b.getType() != Material.BEDROCK) b.setType(Material.AIR);
                }
            }
        }
    }
}
