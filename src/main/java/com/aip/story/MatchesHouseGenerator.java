package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;

/**
 * v2.2.7：火柴盒世界生成器
 * <p>
 * 生成 5x5x5 温馨火柴盒（玻璃墙 + 木地板 + 木屋顶）：
 *   - 地板：橡木木板
 *   - 屋顶：深色橡木
 *   - 墙：橡木边框 + 玻璃
 *   - 门口朝南
 *   - 装饰：双人床（红色）、橡木箱子、4 根火把、红色地毯、2 幅画、2 个书架
 *   - 床位朝向门口
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
        return generate(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    /**
     * 在指定世界坐标生成火柴盒
     * @return 玩家传送点（房子中心）
     */
    public static Location generate(World world, int ox, int oy, int oz) {
        if (world == null) return null;
        int bx = ox - 2;  // 火柴盒从 origin 中心点扩展
        int by = oy;
        int bz = oz - 2;

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
                    // 留出门（门口朝南，z=0, x=2）
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

        // 门口（南侧，z=0, x=2）—— 清理重复
        world.getBlockAt(bx + 2, by + 1, bz).setType(Material.AIR);
        world.getBlockAt(bx + 2, by + 2, bz).setType(Material.AIR);

        // ===== 装饰 =====

        // 装饰 1：双人床（红色，床头靠北墙 z=4，朝向门口南 z=0）
        //   - HEAD 块在北墙（z=4, x=1）
        //   - FOOT 块在中央（z=3, x=1）
        //   - 朝向：south（床头朝南，对着门）
        try {
            Block head = world.getBlockAt(bx + 1, by + 1, bz + 4);
            head.setType(Material.RED_BED);
            Bed bedHead = (Bed) head.getBlockData();
            bedHead.setPart(Bed.Part.HEAD);
            bedHead.setFacing(org.bukkit.block.BlockFace.SOUTH);
            head.setBlockData(bedHead);

            Block foot = world.getBlockAt(bx + 1, by + 1, bz + 3);
            foot.setType(Material.RED_BED);
            Bed bedFoot = (Bed) foot.getBlockData();
            bedFoot.setPart(Bed.Part.FOOT);
            bedFoot.setFacing(org.bukkit.block.BlockFace.SOUTH);
            foot.setBlockData(bedFoot);
        } catch (Exception ignored) {}

        // 装饰 2：橡木箱子（西北角，x=0, z=4）
        world.getBlockAt(bx, by + 1, bz + 4).setType(Material.CHEST);

        // 装饰 3：4 根火把（4 角天花板）
        world.getBlockAt(bx + 1, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 3, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 1, by + 3, bz + 3).setType(Material.TORCH);
        world.getBlockAt(bx + 3, by + 3, bz + 3).setType(Material.TORCH);

        // 装饰 4：红色地毯（中央 3x3，y=1）
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                world.getBlockAt(bx + x, by + 1, bz + z).setType(Material.RED_CARPET);
            }
        }

        // 装饰 5：2 幅画（东墙 x=4, y=2, z=1 和 z=3）
        world.getBlockAt(bx + 4, by + 2, bz + 1).setType(Material.PAINTING);
        world.getBlockAt(bx + 4, by + 2, bz + 3).setType(Material.PAINTING);

        // 装饰 6：2 个书架（西墙 x=0, y=1 和 y=2, z=2 中央）⭐ 新增
        world.getBlockAt(bx, by + 1, bz + 2).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 2, bz + 2).setType(Material.BOOKSHELF);

        return new Location(world, bx + 2.5, by + 1, bz + 2.5);
    }

    /**
     * 生成 Eve 的火柴盒（与玩家火柴盒并排，镜像反转版）
     * @param origin 玩家火柴盒 origin
     * @return Eve 火柴盒的玩家传送点
     */
    public static Location generateEveHouse(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        // Eve 的火柴盒在玩家火柴盒西边 6 米（z=-6）
        int ex = origin.getBlockX();
        int ey = origin.getBlockY();
        int ez = origin.getBlockZ() - 6;
        Location eveOrigin = new Location(world, ex, ey, ez);
        // 生成 Eve 的火柴盒（结构同玩家，但装饰略不同）
        Location loc = generate(eveOrigin);
        if (loc == null) return null;
        // 反转装饰：把红色地毯改成黑色，把画换成别的（Eve 的"镜像反转"）
        // 简单实现：把中央 3x3 红色地毯改成黑色
        int bx = ex - 2;
        int by = ey;
        int bz = ez - 2;
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                world.getBlockAt(bx + x, by + 1, bz + z).setType(Material.BLACK_CARPET);
            }
        }
        return loc;
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
