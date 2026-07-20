package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * v2.2.9：AI 总部生成器（章节 8 - 第二道门：真相）
 * <p>
 * 50x50 大房间（包含墙壁），内部 9x9 监控屏幕（用 redstone_lamp 模拟）。
 * 中心有一道大铁门（入口）。
 * <p>
 * 房间布局：
 *   - 50x50 地板（深色橡木）
 *   - 50x50 墙壁（黑曜石，高 8）
 *   - 9x9 监控屏幕（北墙）：redstone_lamp
 *   - 中央 1x1 控制台（附魔台）
 *   - 中心大铁门（南墙）
 *   - 天花板：黑曜石 + 红石灯
 */
public class AiHeadquartersGenerator {

    /** 内部宽度（含墙壁） */
    public static final int SIZE = 50;
    /** 内部高度 */
    public static final int HEIGHT = 8;

    /**
     * 在 origin 处生成 AI 总部
     * @param origin 总部西南角（地板位置）
     * @return 玩家传送点（入口前）
     */
    public static Location generate(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        return generate(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    /**
     * 在指定世界坐标生成 AI 总部
     * @return 玩家传送点（入口前 y+1）
     */
    public static Location generate(World world, int ox, int oy, int oz) {
        if (world == null) return null;
        int bx = ox;
        int by = oy;
        int bz = oz;

        // 清空区域（多清 1 圈）
        for (int x = -1; x <= SIZE; x++) {
            for (int y = 0; y < HEIGHT + 1; y++) {
                for (int z = -1; z <= SIZE + 1; z++) {
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    b.setType(Material.AIR);
                }
            }
        }

        // ==== 地板（深色橡木木板）====
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.DARK_OAK_PLANKS);
            }
        }

        // ==== 天花板（黑曜石）====
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Block b = world.getBlockAt(bx + x, by + HEIGHT - 1, bz + z);
                b.setType(Material.OBSIDIAN);
            }
        }

        // ==== 墙壁（黑曜石）====
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    if (x != 0 && x != SIZE - 1 && z != 0 && z != SIZE - 1) continue;
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    b.setType(Material.OBSIDIAN);
                }
            }
        }

        // ==== 北墙 9x9 监控屏幕（redstone_lamp）====
        // 北墙 z = SIZE-1，y = 2 到 4，x = 21 到 29（中心 9 格）
        for (int x = 21; x <= 29; x++) {
            for (int y = 2; y <= 4; y++) {
                Block screen = world.getBlockAt(bx + x, by + y, bz + SIZE - 1);
                screen.setType(Material.REDSTONE_LAMP);
            }
        }

        // ==== 中央控制台（附魔台 + 红石块装饰）====
        Block center = world.getBlockAt(bx + SIZE / 2, by + 1, bz + SIZE / 2);
        center.setType(Material.ENCHANTING_TABLE);
        // 控制台四角的红色羊毛（标识 AI 控制中心）
        int cx = SIZE / 2;
        int cz = SIZE / 2;
        world.getBlockAt(bx + cx + 2, by + 1, bz + cz).setType(Material.RED_WOOL);
        world.getBlockAt(bx + cx - 2, by + 1, bz + cz).setType(Material.RED_WOOL);
        world.getBlockAt(bx + cx, by + 1, bz + cz + 2).setType(Material.RED_WOOL);
        world.getBlockAt(bx + cx, by + 1, bz + cz - 2).setType(Material.RED_WOOL);

        // ==== 4 角火把 ====
        world.getBlockAt(bx, by + 1, bz).setType(Material.REDSTONE_TORCH);
        world.getBlockAt(bx + SIZE - 1, by + 1, bz).setType(Material.REDSTONE_TORCH);
        world.getBlockAt(bx, by + 1, bz + SIZE - 1).setType(Material.REDSTONE_TORCH);
        world.getBlockAt(bx + SIZE - 1, by + 1, bz + SIZE - 1).setType(Material.REDSTONE_TORCH);

        // ==== 入口大铁门（南墙 z=0 中心 3x3）====
        // 删除南墙中段
        for (int x = 23; x <= 27; x++) {
            for (int y = 1; y <= 3; y++) {
                world.getBlockAt(bx + x, by + y, bz).setType(Material.AIR);
            }
        }
        // 放置大铁门（在 z=-1 外面作为入口门面）
        // 简化为放置 2 个铁门在入口两侧
        try {
            Block door1 = world.getBlockAt(bx + 24, by + 1, bz);
            door1.setType(Material.IRON_DOOR);
            org.bukkit.block.data.type.Door doorData1 = (org.bukkit.block.data.type.Door) door1.getBlockData();
            doorData1.setFacing(org.bukkit.block.BlockFace.SOUTH);
            door1.setBlockData(doorData1);

            Block door2 = world.getBlockAt(bx + 26, by + 1, bz);
            door2.setType(Material.IRON_DOOR);
            org.bukkit.block.data.type.Door doorData2 = (org.bukkit.block.data.type.Door) door2.getBlockData();
            doorData2.setFacing(org.bukkit.block.BlockFace.SOUTH);
            door2.setBlockData(doorData2);
        } catch (Exception ignored) {}

        // ==== 走廊入口连接（南墙外 1 块平台）====
        for (int x = 23; x <= 27; x++) {
            world.getBlockAt(bx + x, by, bz - 1).setType(Material.DARK_OAK_PLANKS);
        }

        // 玩家传送点：入口前（南墙外）
        return new Location(world, bx + SIZE / 2.0, by + 1, bz - 2.0);
    }

    /**
     * 清除 AI 总部
     */
    public static void clear(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        World world = origin.getWorld();
        int bx = origin.getBlockX();
        int by = origin.getBlockY();
        int bz = origin.getBlockZ();
        for (int x = -5; x < SIZE + 5; x++) {
            for (int y = -2; y < HEIGHT + 5; y++) {
                for (int z = -5; z < SIZE + 5; z++) {
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    if (b.getType() != Material.BEDROCK) b.setType(Material.AIR);
                }
            }
        }
    }
}
