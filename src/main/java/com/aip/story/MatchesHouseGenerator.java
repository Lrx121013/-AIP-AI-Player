package com.aip.story;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * v2.2.7：火柴盒世界生成器
 * <p>
 * 生成 5x5x5 温馨火柴盒（玻璃墙 + 木地板 + 木屋顶）：
 *   - 内部装饰：床、箱子、火把、地毯、墙上的画
 *   - 床位朝向门口
 *   - 入口门（朝南）
 */
public class MatchesHouseGenerator {
    /** 内部宽度 */
    public static final int SIZE = 5;
    /** 内部高度 */
    public static final int HEIGHT = 5;

    /**
     * 在指定位置生成火柴盒
     * @param origin 房子东南角地面
     * @return 玩家传送点（房子中心）
     */
    public static Location generate(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        int bx = origin.getBlockX() - 2;  // 火柴盒从 origin 中心点扩展
        int by = origin.getBlockY();
        int bz = origin.getBlockZ() - 2;

        // 清空区域
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    b.setType(Material.AIR);
                }
            }
        }

        // 地板（橡木木板）
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.OAK_PLANKS);
            }
        }

        // 屋顶（深色橡木）
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Block b = world.getBlockAt(bx + x, by + HEIGHT - 1, bz + z);
                b.setType(Material.DARK_OAK_PLANKS);
            }
        }

        // 墙（玻璃 + 橡木边框）
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    // 只生成外墙
                    if (x != 0 && x != SIZE - 1 && z != 0 && z != SIZE - 1) continue;
                    // 留出门
                    if (y < 3 && z == 0 && x == 2) continue;
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    // 边框用橡木
                    if (x == 0 || x == SIZE - 1 || y == 1 || y == HEIGHT - 2) {
                        b.setType(Material.OAK_LOG);
                    } else {
                        b.setType(Material.GLASS);
                    }
                }
            }
        }

        // 门口（南侧，z=0, x=2）
        world.getBlockAt(bx + 2, by + 1, bz).setType(Material.AIR);
        world.getBlockAt(bx + 2, by + 2, bz).setType(Material.AIR);

        // 装饰 1：床（红色羊毛，床头靠北墙）
        world.getBlockAt(bx + 1, by + 1, bz + 4).setType(Material.RED_BED);
        // 让床有"床头"朝向
        try {
            org.bukkit.block.data.type.Bed bed = (org.bukkit.block.data.type.Bed) world.getBlockAt(bx + 1, by + 1, bz + 4).getBlockData();
            bed.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
            world.getBlockAt(bx + 1, by + 1, bz + 4).setBlockData(bed);
            Block head = world.getBlockAt(bx + 1, by + 1, bz + 3);
            head.setType(Material.RED_BED);
            org.bukkit.block.data.type.Bed bed2 = (org.bukkit.block.data.type.Bed) head.getBlockData();
            bed2.setPart(org.bukkit.block.data.type.Bed.Part.FOOT);
            head.setBlockData(bed2);
        } catch (Exception ignored) {}

        // 装饰 2：箱子（西北角）
        world.getBlockAt(bx, by + 1, bz + 4).setType(Material.CHEST);
        // 装饰 3：火把（4 角天花板上）
        world.getBlockAt(bx + 1, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 3, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 1, by + 3, bz + 3).setType(Material.TORCH);
        world.getBlockAt(bx + 3, by + 3, bz + 3).setType(Material.TORCH);
        // 装饰 4：地毯（中央 3x3 红色）
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                world.getBlockAt(bx + x, by + 1, bz + z).setType(Material.RED_CARPET);
            }
        }
        // 装饰 5：画（东墙，2 格高）
        world.getBlockAt(bx + 4, by + 2, bz + 1).setType(Material.PAINTING);
        world.getBlockAt(bx + 4, by + 2, bz + 3).setType(Material.PAINTING);

        return new Location(world, bx + 2.5, by + 1, bz + 2.5);
    }

    /**
     * 清除火柴盒
     */
    public static void clear(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        World world = origin.getWorld();
        int bx = origin.getBlockX() - 2;
        int by = origin.getBlockY();
        int bz = origin.getBlockZ() - 2;
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
