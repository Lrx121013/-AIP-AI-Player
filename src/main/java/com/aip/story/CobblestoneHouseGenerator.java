package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;

/**
 * v2.2.10：圆石小屋世界生成器（替代火柴盒）
 * <p>
 * 尺寸：11 宽 x 11 长 x 7 高（比火柴盒 7x7 大 4 倍）
 * <p>
 * 装饰清单（比火柴盒升级版）：
 *   - 地板：圆石
 *   - 屋顶：圆石台阶
 *   - 墙：圆石边框 + 玻璃（中央墙是玻璃，光线好）
 *   - 门口朝南（3 格宽，橡木门）
 *   - 家具：
 *     - 双人床（红色，朝门）
 *     - 橡木箱子 x3（角落）
 *     - 4 个书架
 *     - 工作台 + 熔炉 x2
 *     - 桌子（橡木台阶 + 灰色地毯）
 *     - 椅子（橡木楼梯）
 *     - 画 x6
 *     - 火把 x8
 *     - 花盆 x4
 *     - 灯笼 x2
 *     - 红色地毯
 *   - 门外：5x3 平台 + 台阶
 */
public class CobblestoneHouseGenerator {
    /** 内部宽度 */
    public static final int SIZE = 11;
    /** 内部高度 */
    public static final int HEIGHT = 7;

    /**
     * 在指定位置生成圆石小屋
     * @param origin 房子西南角（地板位置）
     * @return 玩家传送点（房子中心 y+1）
     */
    public static Location generate(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        return generate(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    /**
     * 在指定世界坐标生成圆石小屋
     * @return 玩家传送点（房子中心 y+1）
     */
    public static Location generate(World world, int ox, int oy, int oz) {
        if (world == null) return null;
        int bx = ox;
        int by = oy;
        int bz = oz;

        // 清空区域（多清 1 圈）
        for (int x = -1; x <= SIZE; x++) {
            for (int y = 0; y < HEIGHT + 2; y++) {
                for (int z = -1; z <= SIZE + 1; z++) {
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    b.setType(Material.AIR);
                }
            }
        }

        // ==== 地板（圆石）====
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.COBBLESTONE);
            }
        }

        // ==== 屋顶（圆石台阶）====
        for (int x = -1; x <= SIZE; x++) {
            for (int z = -1; z <= SIZE + 1; z++) {
                Block b = world.getBlockAt(bx + x, by + HEIGHT, bz + z);
                b.setType(Material.COBBLESTONE_SLAB);
                Slab slab = (Slab) b.getBlockData();
                slab.setType(Slab.Type.TOP);
                b.setBlockData(slab);
            }
        }

        // ==== 外墙（圆石边框 + 玻璃）====
        for (int y = 1; y < HEIGHT; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    // 只生成外墙
                    if (x != 0 && x != SIZE - 1 && z != 0 && z != SIZE - 1) continue;
                    // 南墙门口（z=0, x=4,5,6 留空）
                    if (z == 0 && y < 3 && (x == 4 || x == 5 || x == 6)) continue;
                    // 北墙窗户下方（z=SIZE-1, y=1）放玻璃
                    if (z == SIZE - 1 && y >= 1 && y <= 3) {
                        // 玻璃窗
                        Block b = world.getBlockAt(bx + x, by + y, bz + z);
                        b.setType(Material.GLASS);
                        continue;
                    }
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    // 边框用圆石
                    if (x == 0 || x == SIZE - 1 || z == 0 || z == SIZE - 1) {
                        if (y == 1 || y == HEIGHT - 1) {
                            b.setType(Material.COBBLESTONE);
                        } else if (y == 2 || y == 3) {
                            // 中间用玻璃（仅南北侧有窗，东西侧实心）
                            if (x == 0 || x == SIZE - 1) {
                                b.setType(Material.COBBLESTONE);
                            } else {
                                b.setType(Material.GLASS);
                            }
                        } else {
                            b.setType(Material.COBBLESTONE);
                        }
                    }
                }
            }
        }

        // ==== 门口门（橡木门）- 3 格宽 ====
        placeDoor(world, bx + 4, by + 1, bz, org.bukkit.block.BlockFace.SOUTH);
        placeDoor(world, bx + 5, by + 1, bz, org.bukkit.block.BlockFace.SOUTH);
        placeDoor(world, bx + 6, by + 1, bz, org.bukkit.block.BlockFace.SOUTH);

        // ==== 门口灯笼（2 个）====
        world.getBlockAt(bx + 4, by + 2, bz - 1).setType(Material.LANTERN);
        world.getBlockAt(bx + 6, by + 2, bz - 1).setType(Material.LANTERN);

        // ==== 门外 5x3 平台 ====
        for (int x = 3; x <= 7; x++) {
            for (int z = -3; z <= -1; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.STONE_BRICKS);
            }
        }
        // 平台边缘台阶（南侧）
        Block front = world.getBlockAt(bx + 5, by - 1, bz - 3);
        front.setType(Material.STONE_BRICK_STAIRS);
        Stairs stairs = (Stairs) front.getBlockData();
        stairs.setFacing(org.bukkit.block.BlockFace.SOUTH);
        front.setBlockData(stairs);

        // ==== 内部装饰 ====

        // 1) 双人床（西南角，床头朝南 z=0）
        try {
            // 床脚在 (x=1, y=1, z=1)
            Block foot = world.getBlockAt(bx + 1, by + 1, bz + 1);
            foot.setType(Material.RED_BED);
            Bed bedFoot = (Bed) foot.getBlockData();
            bedFoot.setPart(Bed.Part.FOOT);
            bedFoot.setFacing(org.bukkit.block.BlockFace.SOUTH);
            foot.setBlockData(bedFoot);
            // 床头在 (x=1, y=1, z=2)
            Block head = world.getBlockAt(bx + 1, by + 1, bz + 2);
            head.setType(Material.RED_BED);
            Bed bedHead = (Bed) head.getBlockData();
            bedHead.setPart(Bed.Part.HEAD);
            bedHead.setFacing(org.bukkit.block.BlockFace.SOUTH);
            head.setBlockData(bedHead);
        } catch (Exception ignored) {}

        // 2) 橡木箱子 x3（东北角 + 东南角 + 西北角）
        world.getBlockAt(bx, by + 1, bz + SIZE - 1).setType(Material.CHEST);
        world.getBlockAt(bx + SIZE - 1, by + 1, bz + SIZE - 1).setType(Material.CHEST);
        world.getBlockAt(bx + SIZE - 1, by + 1, bz).setType(Material.CHEST);

        // 3) 4 个书架（西墙，y=1 和 y=2，x=0，z=4,5,6,7）
        world.getBlockAt(bx, by + 1, bz + 4).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 1, bz + 5).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 2, bz + 4).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 2, bz + 5).setType(Material.BOOKSHELF);

        // 4) 工作台（厨房角 x=10, y=1, z=0）
        world.getBlockAt(bx + SIZE - 1, by + 1, bz + 1).setType(Material.CRAFTING_TABLE);

        // 5) 熔炉 x2（厨房角 x=9,10 y=1 z=1）
        try {
            Block furnace1 = world.getBlockAt(bx + SIZE - 2, by + 1, bz + 1);
            furnace1.setType(Material.FURNACE);
            org.bukkit.block.data.type.Furnace furnaceData1 = (org.bukkit.block.data.type.Furnace) furnace1.getBlockData();
            furnaceData1.setFacing(org.bukkit.block.BlockFace.SOUTH);
            furnace1.setBlockData(furnaceData1);

            Block furnace2 = world.getBlockAt(bx + SIZE - 2, by + 2, bz + 1);
            furnace2.setType(Material.FURNACE);
            org.bukkit.block.data.type.Furnace furnaceData2 = (org.bukkit.block.data.type.Furnace) furnace2.getBlockData();
            furnaceData2.setFacing(org.bukkit.block.BlockFace.SOUTH);
            furnace2.setBlockData(furnaceData2);
        } catch (Exception ignored) {}

        // 6) 桌子 + 椅子（中央）
        // 桌子腿（橡木台阶，中央 x=5, y=1, z=5）
        Block tableLeg = world.getBlockAt(bx + 5, by + 1, bz + 5);
        tableLeg.setType(Material.OAK_SLAB);
        Slab tableSlab = (Slab) tableLeg.getBlockData();
        tableSlab.setType(Slab.Type.BOTTOM);
        tableLeg.setBlockData(tableSlab);
        // 桌上方（灰色地毯）
        Block tableTop = world.getBlockAt(bx + 5, by + 2, bz + 5);
        tableTop.setType(Material.GRAY_CARPET);
        // 椅子（橡木楼梯，北面）
        Block chair = world.getBlockAt(bx + 5, by + 1, bz + 6);
        chair.setType(Material.OAK_STAIRS);
        Stairs chairStairs = (Stairs) chair.getBlockData();
        chairStairs.setFacing(org.bukkit.block.BlockFace.SOUTH);
        chair.setBlockData(chairStairs);

        // 7) 画（东墙 x=SIZE-1, y=2, z=2,3,4,5,7,8）
        world.getBlockAt(bx + SIZE - 1, by + 2, bz + 2).setType(Material.PAINTING);
        world.getBlockAt(bx + SIZE - 1, by + 2, bz + 3).setType(Material.PAINTING);
        world.getBlockAt(bx + SIZE - 1, by + 2, bz + 4).setType(Material.PAINTING);
        world.getBlockAt(bx + SIZE - 1, by + 2, bz + 5).setType(Material.PAINTING);
        world.getBlockAt(bx + SIZE - 1, by + 2, bz + 7).setType(Material.PAINTING);
        world.getBlockAt(bx + SIZE - 1, by + 2, bz + 8).setType(Material.PAINTING);

        // 8) 火把 x8（4 角 + 4 中央）
        world.getBlockAt(bx + 1, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 1, by + 3, bz + SIZE - 2).setType(Material.TORCH);
        world.getBlockAt(bx + SIZE - 2, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + SIZE - 2, by + 3, bz + SIZE - 2).setType(Material.TORCH);
        world.getBlockAt(bx + 4, by + 3, bz + 3).setType(Material.TORCH);
        world.getBlockAt(bx + 7, by + 3, bz + 3).setType(Material.TORCH);
        world.getBlockAt(bx + 4, by + 3, bz + 7).setType(Material.TORCH);
        world.getBlockAt(bx + 7, by + 3, bz + 7).setType(Material.TORCH);

        // 9) 花盆 x4（角落）
        world.getBlockAt(bx, by + 1, bz + 1).setType(Material.POTTED_POPPY);
        world.getBlockAt(bx + SIZE - 1, by + 1, bz + 1).setType(Material.POTTED_OXEYE_DAISY);
        world.getBlockAt(bx, by + 1, bz + SIZE - 2).setType(Material.POTTED_DANDELION);
        world.getBlockAt(bx + SIZE - 1, by + 1, bz + SIZE - 2).setType(Material.POTTED_AZURE_BLUET);

        // 10) 中央地毯（中央 2x2，y=1）
        for (int x = 5; x <= 6; x++) {
            for (int z = 3; z <= 4; z++) {
                world.getBlockAt(bx + x, by + 1, bz + z).setType(Material.RED_CARPET);
            }
        }

        // 11) 床前地毯（y=1, 床前 z=0 一排）
        for (int x = 1; x <= 2; x++) {
            world.getBlockAt(bx + x, by + 1, bz).setType(Material.RED_CARPET);
        }

        // 玩家传送点：房子中心 (x+5.5, y+1, z+5.5)
        return new Location(world, bx + (SIZE / 2.0) - 0.5 + 0.5, by + 1, bz + (SIZE / 2.0) - 0.5 + 0.5);
    }

    /**
     * 放置橡木门
     */
    private static void placeDoor(World world, int x, int y, int z, org.bukkit.block.BlockFace facing) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(Material.OAK_DOOR);
        try {
            org.bukkit.block.data.type.Door door = (org.bukkit.block.data.type.Door) b.getBlockData();
            door.setFacing(facing);
            door.setOpen(false);
            b.setBlockData(door);
        } catch (Exception ignored) {}
    }

    /**
     * 清除圆石小屋
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
