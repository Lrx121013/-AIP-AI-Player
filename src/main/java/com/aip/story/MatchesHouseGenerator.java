package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;

/**
 * v2.2.7：火柴盒世界生成器（升级版）
 * <p>
 * 尺寸：7x6x7（更宽敞）
 * <p>
 * 装饰清单：
 *   - 地板：橡木木板
 *   - 屋顶：深色橡木台阶
 *   - 墙：橡木边框 + 玻璃（中央墙是玻璃，光线好）
 *   - 门口朝南（2 格宽，橡木门）
 *   - 家具：
 *     - 双人床（红色，床头朝门）
 *     - 橡木箱子 x2（西北角 + 东北角）
 *     - 4 个书架（西墙）
 *     - 工作台（厨房角）
 *     - 熔炉 x2（厨房角）
 *     - 桌子（橡木台阶 + 红色地毯）
 *     - 椅子（橡木楼梯）
 *     - 画 x4（东墙 + 西墙）
 *     - 火把 x6（4 角 + 2 中央）
 *     - 花盆 x2（角落）
 *     - 灯笼 x2（门口）
 *   - 门外：3x3 平台 + 台阶
 *   - 整体设计：温馨小屋
 */
public class MatchesHouseGenerator {
    /** 内部宽度 */
    public static final int SIZE = 7;
    /** 内部高度 */
    public static final int HEIGHT = 6;

    /**
     * 在指定位置生成火柴盒
     * @param origin 房子中心地面 (玩家将站在这里上方 1 格)
     * @return 玩家传送点（房子中心 y+1）
     */
    public static Location generate(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        return generate(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    /**
     * 在指定世界坐标生成火柴盒
     * @return 玩家传送点（房子中心 y+1）
     */
    public static Location generate(World world, int ox, int oy, int oz) {
        if (world == null) return null;
        // 7x7 房子，以 origin 为房子西南角
        int bx = ox;
        int by = oy;
        int bz = oz;

        // 清空区域（多清 1 圈，避免残留）
        for (int x = -1; x <= SIZE; x++) {
            for (int y = 0; y < HEIGHT + 1; y++) {
                for (int z = -1; z <= SIZE + 1; z++) {
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    b.setType(Material.AIR);
                }
            }
        }

        // ==== 地板（橡木木板）====
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.OAK_PLANKS);
            }
        }

        // ==== 屋顶（深色橡木台阶）====
        for (int x = -1; x <= SIZE; x++) {
            for (int z = -1; z <= SIZE + 1; z++) {
                Block b = world.getBlockAt(bx + x, by + HEIGHT, bz + z);
                b.setType(Material.DARK_OAK_SLAB);
                Slab slab = (Slab) b.getBlockData();
                slab.setType(Slab.Type.TOP);
                b.setBlockData(slab);
            }
        }

        // ==== 外墙（橡木边框 + 玻璃）====
        for (int y = 1; y < HEIGHT; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    // 只生成外墙
                    if (x != 0 && x != SIZE - 1 && z != 0 && z != SIZE - 1) continue;
                    // 南墙门口（z=0, x=3,4）留空
                    if (z == 0 && y < 3 && (x == 3 || x == 4)) continue;
                    // 北墙窗户下方（z=6, y=1）放玻璃
                    if (z == SIZE - 1 && y == 1) continue;
                    Block b = world.getBlockAt(bx + x, by + y, bz + z);
                    // 边框用橡木（最外圈）
                    if (x == 0 || x == SIZE - 1 || z == 0 || z == SIZE - 1) {
                        // 4 角 + 上下边用橡木
                        if (y == 1 || y == HEIGHT - 1) {
                            b.setType(Material.OAK_LOG);
                        } else {
                            // 中间用玻璃
                            b.setType(Material.GLASS);
                        }
                    }
                }
            }
        }

        // ==== 门口门（橡木门）- 2 格宽 ====
        placeDoor(world, bx + 3, by + 1, bz, org.bukkit.block.BlockFace.SOUTH);
        placeDoor(world, bx + 4, by + 1, bz, org.bukkit.block.BlockFace.SOUTH);

        // ==== 门口灯笼（2 个）====
        world.getBlockAt(bx + 3, by + 2, bz - 1).setType(Material.LANTERN);
        world.getBlockAt(bx + 4, by + 2, bz - 1).setType(Material.LANTERN);

        // ==== 门外 3x3 平台 ====
        for (int x = 2; x <= 5; x++) {
            for (int z = -3; z <= -1; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.OAK_PLANKS);
            }
        }
        // 平台边缘台阶
        Block front = world.getBlockAt(bx + 3, by - 1, bz - 3);
        front.setType(Material.OAK_STAIRS);
        Stairs stairs = (Stairs) front.getBlockData();
        stairs.setFacing(org.bukkit.block.BlockFace.SOUTH);
        front.setBlockData(stairs);

        // ==== 内部装饰 ====

        // 1) 双人床（西侧，床头朝南 z=0）
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

        // 2) 橡木箱子 x2（西北角 + 东北角）
        world.getBlockAt(bx, by + 1, bz + 6).setType(Material.CHEST);
        world.getBlockAt(bx + 6, by + 1, bz + 6).setType(Material.CHEST);

        // 3) 4 个书架（西墙，y=1 和 y=2，x=0）
        world.getBlockAt(bx, by + 1, bz + 3).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 1, bz + 4).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 2, bz + 3).setType(Material.BOOKSHELF);
        world.getBlockAt(bx, by + 2, bz + 4).setType(Material.BOOKSHELF);

        // 4) 工作台（厨房角 x=6, y=1, z=0）
        world.getBlockAt(bx + 6, by + 1, bz).setType(Material.CRAFTING_TABLE);

        // 5) 熔炉 x2（厨房角 x=5,6 y=1 z=0）
        try {
            Block furnace1 = world.getBlockAt(bx + 5, by + 1, bz);
            furnace1.setType(Material.FURNACE);
            org.bukkit.block.data.type.Furnace furnaceData1 = (org.bukkit.block.data.type.Furnace) furnace1.getBlockData();
            furnaceData1.setFacing(org.bukkit.block.BlockFace.SOUTH);
            furnace1.setBlockData(furnaceData1);

            Block furnace2 = world.getBlockAt(bx + 5, by + 2, bz);
            furnace2.setType(Material.FURNACE);
            org.bukkit.block.data.type.Furnace furnaceData2 = (org.bukkit.block.data.type.Furnace) furnace2.getBlockData();
            furnaceData2.setFacing(org.bukkit.block.BlockFace.SOUTH);
            furnace2.setBlockData(furnaceData2);
        } catch (Exception ignored) {}

        // 6) 桌子 + 椅子（中央）
        // 桌子腿（橡木台阶，中央 x=3, y=1, z=4）
        Block tableLeg = world.getBlockAt(bx + 3, by + 1, bz + 4);
        tableLeg.setType(Material.OAK_SLAB);
        Slab tableSlab = (Slab) tableLeg.getBlockData();
        tableSlab.setType(Slab.Type.BOTTOM);
        tableLeg.setBlockData(tableSlab);
        // 桌上方（红色地毯） - 桌子桌面
        Block tableTop = world.getBlockAt(bx + 3, by + 2, bz + 4);
        tableTop.setType(Material.RED_CARPET);
        // 椅子（橡木楼梯，北面）
        Block chair = world.getBlockAt(bx + 3, by + 1, bz + 5);
        chair.setType(Material.OAK_STAIRS);
        Stairs chairStairs = (Stairs) chair.getBlockData();
        chairStairs.setFacing(org.bukkit.block.BlockFace.SOUTH);
        chair.setBlockData(chairStairs);

        // 7) 画（东墙 x=6, y=2, z=1,2,4,5）
        world.getBlockAt(bx + 6, by + 2, bz + 1).setType(Material.PAINTING);
        world.getBlockAt(bx + 6, by + 2, bz + 2).setType(Material.PAINTING);
        world.getBlockAt(bx + 6, by + 2, bz + 4).setType(Material.PAINTING);
        world.getBlockAt(bx + 6, by + 2, bz + 5).setType(Material.PAINTING);

        // 8) 火把 x6（4 角 + 2 中央）
        world.getBlockAt(bx + 1, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 5, by + 3, bz + 1).setType(Material.TORCH);
        world.getBlockAt(bx + 1, by + 3, bz + 5).setType(Material.TORCH);
        world.getBlockAt(bx + 5, by + 3, bz + 5).setType(Material.TORCH);
        world.getBlockAt(bx + 3, by + 3, bz + 2).setType(Material.TORCH);
        world.getBlockAt(bx + 4, by + 3, bz + 5).setType(Material.TORCH);

        // 9) 花盆 x2（角落）
        world.getBlockAt(bx, by + 1, bz).setType(Material.POTTED_POPPY);
        world.getBlockAt(bx + 6, by + 1, bz + 6).setType(Material.POTTED_OXEYE_DAISY);

        // 10) 中央地毯（中央 2x2，y=1）
        for (int x = 4; x <= 5; x++) {
            for (int z = 1; z <= 2; z++) {
                world.getBlockAt(bx + x, by + 1, bz + z).setType(Material.RED_CARPET);
            }
        }

        // 11) 床前地毯（y=1, 床前 z=0 一排）
        for (int x = 1; x <= 2; x++) {
            world.getBlockAt(bx + x, by + 1, bz).setType(Material.RED_CARPET);
        }

        // 玩家传送点：房子中心 (x+3.5, y+1, z+3.5)
        return new Location(world, bx + 3.5, by + 1, bz + 3.5);
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
     * 生成 Eve 的火柴盒（与玩家火柴盒并排，镜像反转版）
     * @param origin 玩家火柴盒 origin
     * @return Eve 火柴盒的 origin
     */
    public static Location generateEveHouse(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        // Eve 的火柴盒在玩家火柴盒东边 10 米
        int ex = origin.getBlockX() + 10;
        int ey = origin.getBlockY();
        int ez = origin.getBlockZ();
        Location eveOrigin = new Location(world, ex, ey, ez);
        // 生成 Eve 的火柴盒（结构同玩家）
        Location loc = generate(eveOrigin);
        if (loc == null) return null;
        // 反转装饰：把红色地毯改成黑色，把花换成凋零玫瑰，把门口反转到北侧
        int bx = ex;
        int by = ey;
        int bz = ez;
        // 把中央 2x2 红色地毯改成黑色
        for (int x = 4; x <= 5; x++) {
            for (int z = 1; z <= 2; z++) {
                world.getBlockAt(bx + x, by + 1, bz + z).setType(Material.BLACK_CARPET);
            }
        }
        // 床前地毯改成黑色
        for (int x = 1; x <= 2; x++) {
            world.getBlockAt(bx + x, by + 1, bz).setType(Material.BLACK_CARPET);
        }
        // 桌子桌面（红色地毯）改成黑色
        world.getBlockAt(bx + 3, by + 2, bz + 4).setType(Material.BLACK_CARPET);
        // 床改成黑色
        try {
            world.getBlockAt(bx + 1, by + 1, bz + 1).setType(Material.BLACK_BED);
            world.getBlockAt(bx + 1, by + 1, bz + 2).setType(Material.BLACK_BED);
        } catch (Exception ignored) {}
        // 花改成凋零玫瑰
        world.getBlockAt(bx, by + 1, bz).setType(Material.POTTED_WITHER_ROSE);
        world.getBlockAt(bx + 6, by + 1, bz + 6).setType(Material.POTTED_WITHER_ROSE);
        // 门口反转到北侧（z=6） - 删掉南侧门 + 平台
        for (int x = 2; x <= 5; x++) {
            for (int z = -3; z <= -1; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.AIR);
            }
        }
        // 删南侧门
        world.getBlockAt(bx + 3, by + 1, bz).setType(Material.AIR);
        world.getBlockAt(bx + 4, by + 1, bz).setType(Material.AIR);
        // 北侧开门
        placeDoor(world, bx + 3, by + 1, bz + 6, org.bukkit.block.BlockFace.NORTH);
        placeDoor(world, bx + 4, by + 1, bz + 6, org.bukkit.block.BlockFace.NORTH);
        // 北侧平台
        for (int x = 2; x <= 5; x++) {
            for (int z = 7; z <= 9; z++) {
                Block b = world.getBlockAt(bx + x, by, bz + z);
                b.setType(Material.OAK_PLANKS);
            }
        }
        // 北侧台阶
        Block northStair = world.getBlockAt(bx + 3, by - 1, bz + 9);
        northStair.setType(Material.OAK_STAIRS);
        Stairs ns = (Stairs) northStair.getBlockData();
        ns.setFacing(org.bukkit.block.BlockFace.NORTH);
        northStair.setBlockData(ns);
        // 北侧灯笼
        world.getBlockAt(bx + 3, by + 2, bz + 7).setType(Material.SOUL_LANTERN);
        world.getBlockAt(bx + 4, by + 2, bz + 7).setType(Material.SOUL_LANTERN);
        // 删南侧灯笼
        world.getBlockAt(bx + 3, by + 2, bz - 1).setType(Material.AIR);
        world.getBlockAt(bx + 4, by + 2, bz - 1).setType(Material.AIR);

        return eveOrigin;
    }

    /**
     * 清除火柴盒
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
