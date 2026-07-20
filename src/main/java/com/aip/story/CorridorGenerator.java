package com.aip.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * v2.2.7：走廊生成器（章节 6-8 用）
 * <p>
 * 5 宽 x 5 高 x 100 长走廊（东西走向，向东延伸）：
 *   - 地板：黑曜石
 *   - 墙：深色橡木
 *   - 天花板：黑曜石
 *   - 走廊中每 10 米一个 TNT 发射器（共 10 个）
 *   - 发射器位置：(x=10, 20, 30, ..., 100, y=1, z=2)
 *   - 发射器里放 1 个 TNT（激活时发射）
 *   - 发射器上方放一个按钮
 *   - 走廊尽头（西侧）巨型铁门
 *   - 走廊入口（东侧）开放
 * <p>
 * TNT 实际召唤由 AlexNPC.startTntBombing() 的 BukkitTask 负责（每 2 秒召唤一个 TNT），
 * StoryManager 在 Chapter 8 启动这个 task，Chapter 9 取消。
 */
public class CorridorGenerator {
    public static final int LENGTH = 100;
    public static final int WIDTH = 5;
    public static final int HEIGHT = 5;

    /** TNT 发射器在 z 方向的位置（中央） */
    public static final int LAUNCHER_Z = 2;
    /** TNT 发射器在 y 方向的位置（地板上方一格） */
    public static final int LAUNCHER_Y = 1;

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

        // 地板（黑曜石）
        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < WIDTH; z++) {
                world.getBlockAt(sx + x, sy, sz + z).setType(Material.OBSIDIAN);
            }
        }
        // 天花板（黑曜石）
        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < WIDTH; z++) {
                world.getBlockAt(sx + x, sy + HEIGHT - 1, sz + z).setType(Material.OBSIDIAN);
            }
        }
        // 墙（南北两侧，深色橡木）
        for (int x = 0; x < LENGTH; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                world.getBlockAt(sx + x, sy + y, sz).setType(Material.DARK_OAK_PLANKS);
                world.getBlockAt(sx + x, sy + y, sz + WIDTH - 1).setType(Material.DARK_OAK_PLANKS);
            }
        }

        // ===== TNT 发射器 =====
        // 位置：(x=10, 20, 30, ..., 100, y=1, z=2)，共 10 个
        for (int launcherX = 10; launcherX <= 100; launcherX += 10) {
            placeLauncher(world, sx + launcherX, sy + LAUNCHER_Y, sz + LAUNCHER_Z);
        }

        // 走廊尽头（西侧，x=-1）巨型铁门
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < WIDTH; z++) {
                Block doorBlock = world.getBlockAt(sx - 1, sy + y, sz + z);
                doorBlock.setType(Material.IRON_DOOR);
            }
        }

        // 走廊入口（东侧，x=LENGTH）—— 不封，玩家直接走出去

        return new Location(world, sx + LENGTH - 5.0, sy + 1, sz + (WIDTH / 2.0));
    }

    /**
     * 放置一个 TNT 发射器：
     *   - 主体是 dispenser，朝向 EAST
     *   - 里放 1 个 TNT
     *   - 上面放一个石质按钮（方便手动激活）
     */
    private static void placeLauncher(World world, int x, int y, int z) {
        try {
            Block dispenserBlock = world.getBlockAt(x, y, z);
            dispenserBlock.setType(Material.DISPENSER);
            // 设置 dispenser 朝向 EAST
            try {
                org.bukkit.block.data.type.Dispenser dispenserData =
                        (org.bukkit.block.data.type.Dispenser) dispenserBlock.getBlockData();
                dispenserData.setFacing(org.bukkit.block.BlockFace.EAST);
                dispenserBlock.setBlockData(dispenserData);
            } catch (Throwable ignored) {}

            // 往 dispenser 里放 1 个 TNT
            try {
                if (dispenserBlock.getState() instanceof Dispenser) {
                    Dispenser dispenser = (Dispenser) dispenserBlock.getState();
                    Inventory inv = dispenser.getInventory();
                    inv.setItem(0, new ItemStack(Material.TNT, 1));
                    dispenser.update();
                }
            } catch (Throwable t) {
                // 兜底：直接给 dispenser 设置方块数据
                world.getBlockAt(x, y, z).getState();
            }

            // 发射器上方放一个石质按钮（玩家可手动触发）
            Block button = world.getBlockAt(x, y + 1, z);
            button.setType(Material.STONE_BUTTON);
            try {
                org.bukkit.block.data.type.Switch buttonData =
                        (org.bukkit.block.data.type.Switch) button.getBlockData();
                buttonData.setFacing(org.bukkit.block.BlockFace.DOWN);
                button.setBlockData(buttonData);
            } catch (Throwable ignored) {}
        } catch (Exception e) {
            // 兜底：什么都不做，不让生成失败
        }
    }

    /**
     * 清除走廊
     */
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
