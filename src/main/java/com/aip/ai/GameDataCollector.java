package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.config.ConfigManager;
import com.aip.util.LocationUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏数据采集器：收集 AI 玩家周围尽可能完整的游戏信息
 * <p>
 * 数据包括：
 *   1. AI 自身状态：坐标/朝向/血量/饱食度/氧气/经验/装备/手持
 *   2. 世界状态：时间/天气/难度/生物群系/光照等级/世界名
 *   3. 区块信息：当前区块坐标
 *   4. 附近方块：3D 扫描，区分地面/空中/资源方块（矿石/木材/工作台等）
 *   5. 视线前方方块（5 格内目标方块）
 *   6. 附近实体：怪物/动物/掉落物/矿车/船等，附距离和类型
 *   7. 附近玩家：所有可见玩家坐标、血量、装备、手持物
 *   8. 服务器内所有玩家位置（即使远也告诉 AI，让 AI 有空间感）
 *   9. AI 的背包内容
 *  10. 当前游戏阶段（白天/黑夜/黎明/黄昏）
 */
public class GameDataCollector {

    private final ConfigManager config;
    private final AIPlayerPlugin plugin;

    /** GameDataCollector 2 秒 TTL 缓存，避免自主活动+环境感知+@提及三入口重复采集 */
    private static final long CACHE_TTL_MS = 2000;
    private final Map<UUID, CacheEntry> collectCache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final String data;
        final long timestamp;
        CacheEntry(String data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    public GameDataCollector(AIPlayerPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /**
     * 采集 AI 玩家当前游戏数据并格式化为文本
     */
    public String collect(AIPlayer aiPlayer) {
        Player entity = aiPlayer.getEntity();
        if (entity == null) return "（无法获取实体）";
        if (entity.isDead()) return "（AI 已死亡）";

        UUID entityId = entity.getUniqueId();
        long now = System.currentTimeMillis();
        CacheEntry cached = collectCache.get(entityId);
        if (cached != null && now - cached.timestamp < CACHE_TTL_MS) {
            return cached.data;
        }

        StringBuilder sb = new StringBuilder();
        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return "（无法获取世界）";

        // 1. 自身状态
        sb.append("=== 自身状态 ===\n");
        sb.append(String.format("名称: %s (UUID: %s)\n", aiPlayer.getName(), entity.getUniqueId()));
        sb.append(String.format("坐标: x=%.2f, y=%.2f, z=%.2f\n", loc.getX(), loc.getY(), loc.getZ()));
        sb.append(String.format("朝向: yaw=%.1f, pitch=%.1f (面向 %s)\n",
                loc.getYaw(), loc.getPitch(), facingDirection(loc.getYaw())));
        sb.append(String.format("世界: %s (难度 %s)\n", world.getName(), world.getDifficulty().name()));
        sb.append(String.format("区块: chunk=%d,%d (区块内坐标 %d,%d)\n",
                loc.getChunk().getX(), loc.getChunk().getZ(),
                loc.getBlockX() & 15, loc.getBlockZ() & 15));

        // 血量/饱食度/氧气/经验
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
        sb.append(String.format("饱食度: %d/20\n", entity.getFoodLevel()));
        sb.append(String.format("饱和度: %.1f\n", entity.getSaturation()));
        sb.append(String.format("氧气: %d ticks\n", entity.getRemainingAir()));
        sb.append(String.format("经验: 等级 %d, 进度 %.2f, 总经验 %d\n",
                entity.getLevel(), entity.getExp(), entity.getTotalExperience()));
        sb.append(String.format("飞行: %s, 潜行: %s, 冲刺: %s, 睡眠: %s\n",
                entity.isFlying(), entity.isSneaking(), entity.isSprinting(), entity.isSleeping()));
        sb.append(String.format("在水里: %s, 在地上: %s, 阻挡: %s\n",
                entity.isInWater(), entity.isOnGround(), entity.isInvisible() ? "隐身" : "可见"));

        // 2. 装备
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            sb.append("装备:\n");
            sb.append(String.format("  主手: %s\n", itemName(eq.getItemInMainHand())));
            sb.append(String.format("  副手: %s\n", itemName(eq.getItemInOffHand())));
            sb.append(String.format("  头盔: %s\n", itemName(eq.getHelmet())));
            sb.append(String.format("  胸甲: %s\n", itemName(eq.getChestplate())));
            sb.append(String.format("  护腿: %s\n", itemName(eq.getLeggings())));
            sb.append(String.format("  靴子: %s\n", itemName(eq.getBoots())));
        }

        // 3. 背包（前 27 格）
        sb.append("背包:\n");
        PlayerInventory inv = entity.getInventory();
        int invCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            sb.append(String.format("  槽%d: %s\n", i, itemName(item)));
            invCount++;
            if (invCount >= 30) break;  // 限制最大输出
        }
        if (invCount == 0) sb.append("  (空)\n");

        // 4. 世界状态
        sb.append("=== 世界状态 ===\n");
        long time = world.getTime();
        sb.append(String.format("游戏时间: %d ticks (%s)\n", time, formatTime(time)));
        sb.append(String.format("时段: %s\n", timeOfDay(time)));
        sb.append(String.format("天气: %s ( thunder=%s, storm=%s )\n",
                weatherText(world), world.isThundering(), world.hasStorm()));
        sb.append(String.format("满月/月相: %d\n", world.getFullTime() / 24000L % 8));

        // 5. 生物群系 + 光照
        Block feetBlock = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        try {
            Biome biome = feetBlock.getBiome();
            sb.append(String.format("生物群系: %s\n", biome.getKey().toString()));
        } catch (Throwable ignored) {
        }
        try {
            int light = feetBlock.getLightFromBlocks();
            int skyLight = feetBlock.getLightFromSky();
            sb.append(String.format("光照: 方块光=%d, 天空光=%d\n", light, skyLight));
        } catch (Throwable ignored) {
        }

        // 6. 附近方块（3D 扫描）
        sb.append("=== 附近方块 (半径 ").append(config.getScanRadius()).append(") ===\n");
        sb.append(scanNearbyBlocks(loc, config.getScanRadius()));

        // 7. 视线前方方块
        sb.append("=== 视线前方方块 ===\n");
        Block target = entity.getTargetBlockExact(8);
        if (target != null && !target.getType().isAir()) {
            sb.append(String.format("  %s @ %d,%d,%d (距离 %d)\n",
                    target.getType().name(), target.getX(), target.getY(), target.getZ(),
                    (int) LocationUtil.safeDistance(target.getLocation(), loc)));
        } else {
            sb.append("  (8 格内无目标方块)\n");
        }

        // 8. 附近实体
        sb.append("=== 附近实体 (半径 ").append(config.getEntityScanRadius()).append(") ===\n");
        sb.append(scanNearbyEntities(entity, config.getEntityScanRadius()));

        // 9. 附近玩家 + 服务器所有玩家
        sb.append("=== 附近玩家 ===\n");
        sb.append(scanNearbyPlayers(entity, config.getEntityScanRadius()));
        sb.append("=== 服务器所有玩家 ===\n");
        sb.append(listAllPlayers(entity, world));

        // P3：附加附近玩家档案摘要（半径 16）
        if (plugin != null && plugin.getPlayerProfileManager() != null) {
            List<Player> nearby = new ArrayList<>();
            for (Player p : entity.getWorld().getPlayers()) {
                if (p.equals(entity)) continue;
                double d;
                try {
                    d = p.getLocation().distance(loc);
                } catch (Exception e) {
                    continue;
                }
                if (d <= 16) nearby.add(p);
            }
            String profileSummary = plugin.getPlayerProfileManager()
                    .getNearbySummary(entity.getLocation(), 16, nearby);
            if (!profileSummary.isEmpty()) {
                sb.append(profileSummary);
            }
        }

        sb.append("=== 数据结束 ===\n");
        String result = sb.toString();
        collectCache.put(entityId, new CacheEntry(result, System.currentTimeMillis()));
        return result;
    }

    private String formatTime(long ticks) {
        long hours = ((ticks / 1000) + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    private String timeOfDay(long ticks) {
        if (ticks < 1000) return "黎明";
        if (ticks < 6000) return "上午";
        if (ticks < 12000) return "下午";
        if (ticks < 13000) return "黄昏";
        if (ticks < 23000) return "黑夜";
        return "黎明";
    }

    private String weatherText(World world) {
        if (world.isThundering()) return "雷雨";
        if (world.hasStorm()) return "下雨/雪";
        return "晴朗";
    }

    private String facingDirection(float yaw) {
        // Bukkit yaw: 0=南(S), 90=西(W), 180=北(N), -90/270=东(E)
        double y = ((yaw % 360) + 360) % 360;
        if (y < 22.5 || y >= 337.5) return "南 (S)";
        if (y < 67.5) return "西南 (SW)";
        if (y < 112.5) return "西 (W)";
        if (y < 157.5) return "西北 (NW)";
        if (y < 202.5) return "北 (N)";
        if (y < 247.5) return "东北 (NE)";
        if (y < 292.5) return "东 (E)";
        return "东南 (SE)";
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
        return item.getType().name() + (item.getAmount() > 1 ? " x" + item.getAmount() : "");
    }

    /**
     * 扫描附近方块：列出"重要"方块（非空气、非普通地形）
     * 同时输出脚下方块（站立面）和头顶方块
     */
    private String scanNearbyBlocks(Location center, int radius) {
        StringBuilder sb = new StringBuilder();
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = (int) center.getY();
        int cz = center.getBlockZ();

        // 站立方块 + 头顶
        Block feet = world.getBlockAt(cx, cy, cz);
        Block below = world.getBlockAt(cx, cy - 1, cz);
        Block above = world.getBlockAt(cx, cy + 1, cz);
        sb.append(String.format("  脚下方块: %s @ %d,%d,%d\n",
                below.getType().name(), below.getX(), below.getY(), below.getZ()));
        sb.append(String.format("  头顶方块: %s @ %d,%d,%d\n",
                above.getType().name(), above.getX(), above.getY(), above.getZ()));

        // 3D 扫描
        int count = 0;
        int maxCount = 50;
        Set<String> seen = new HashSet<>();

        for (int dy = -radius; dy <= radius && count < maxCount; dy++) {
            for (int dx = -radius; dx <= radius && count < maxCount; dx++) {
                for (int dz = -radius; dz <= radius && count < maxCount; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                    // 跳过自己脚下方块（已输出）
                    if (dx == 0 && dz == 0 && (dy == -1 || dy == 0 || dy == 1)) continue;
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    Material type = b.getType();
                    if (type.isAir()) continue;
                    // 过滤普通地形
                    if (isCommonTerrain(type)) continue;
                    // 只列出"重要"方块
                    if (!isImportantBlock(type)) continue;

                    String key = type.name();
                    if (seen.contains(key) && count > 10) continue;  // 同类型只列一次（前 10 个除外）
                    seen.add(key);

                    double dist = LocationUtil.safeDistance(b.getLocation(), center);
                    sb.append(String.format("  %s @ %d,%d,%d (距 %.1f)\n",
                            type.name(), b.getX(), b.getY(), b.getZ(), dist));
                    count++;
                }
            }
        }
        if (count == 0) sb.append("  (周围只有普通地形)\n");
        return sb.toString();
    }

    private boolean isCommonTerrain(Material type) {
        return type == Material.GRASS_BLOCK || type == Material.DIRT
                || type == Material.STONE || type == Material.SAND
                || type == Material.GRAVEL || type == Material.SANDSTONE
                || type == Material.WATER || type == Material.LAVA
                || type == Material.BEDROCK || type == Material.NETHERRACK
                || type == Material.END_STONE || type == Material.AIR
                || type.name().contains("LEAVES") && !type.name().contains("FLOWER");
    }

    private boolean isImportantBlock(Material type) {
        String name = type.name();
        return name.contains("ORE")             // 矿石
                || name.contains("LOG")          // 原木
                || name.contains("PLANKS")       // 木板
                || name.contains("BRICK")        // 砖块
                || type == Material.CHEST || type == Material.ENDER_CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.CRAFTING_TABLE || type == Material.FURNACE
                || type == Material.BLAST_FURNACE || type == Material.SMOKER
                || type == Material.BREWING_STAND || type == Material.ANVIL
                || type == Material.ENCHANTING_TABLE || type == Material.BOOKSHELF
                || type == Material.TORCH || type == Material.LANTERN
                || type == Material.GLOWSTONE || type == Material.SEA_LANTERN
                || type == Material.COBBLESTONE || type == Material.MOSSY_COBBLESTONE
                || type == Material.OBSIDIAN
                || type == Material.DIAMOND_BLOCK || type == Material.GOLD_BLOCK
                || type == Material.IRON_BLOCK || type == Material.EMERALD_BLOCK
                || type == Material.NETHERITE_BLOCK
                || type == Material.TNT || type == Material.SPONGE
                || type == Material.CAKE || type == Material.JUKEBOX
                || type == Material.NOTE_BLOCK
                || type == Material.HOPPER || type == Material.DROPPER || type == Material.DISPENSER
                || type == Material.PISTON || type == Material.STICKY_PISTON
                || type == Material.LEVER || type.name().endsWith("_BUTTON")
                || type.name().endsWith("_DOOR") || type.name().endsWith("_TRAPDOOR")
                || type.name().endsWith("_FENCE") || type.name().endsWith("_FENCE_GATE")
                || type.name().endsWith("_STAIRS") || type.name().endsWith("_SLAB")
                || type.name().endsWith("_WALL")
                || type == Material.LADDER || type == Material.VINE
                || type == Material.WHEAT || type == Material.CARROTS
                || type == Material.POTATOES || type == Material.BEETROOTS
                || type == Material.MELON || type == Material.PUMPKIN
                || type == Material.SUGAR_CANE || type == Material.CACTUS
                || type == Material.BAMBOO
                || type == Material.SPAWNER || type == Material.BEE_NEST
                || type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME
                || type.name().endsWith("PORTAL")
                || type.name().endsWith("_BED")
                || type == Material.FIRE
                || type == Material.COBWEB
                || type == Material.IRON_BARS
                || type == Material.GLASS || type.name().endsWith("_GLASS")
                || type == Material.ICE || type == Material.BLUE_ICE || type == Material.PACKED_ICE
                || type == Material.SNOW_BLOCK
                || type.isSolid() && !isCommonTerrain(type);
    }

    private String scanNearbyEntities(LivingEntity self, int radius) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        List<Entity> nearby = self.getNearbyEntities(radius, radius, radius);
        // 按距离排序
        nearby.sort((a, b) -> {
            try {
                double da = LocationUtil.safeDistanceSquared(a.getLocation(), self.getLocation());
                double db = LocationUtil.safeDistanceSquared(b.getLocation(), self.getLocation());
                return Double.compare(da, db);
            } catch (Exception ex) {
                return 0;
            }
        });
        // 兼容 1.21+ 新 Attribute API
        org.bukkit.attribute.Attribute maxHealthAttr = resolveAttribute("max_health");
        for (Entity e : nearby) {
            if (e.equals(self)) continue;
            Location l = e.getLocation();
            double dist = LocationUtil.safeDistance(self.getLocation(), l);
            String type = e.getType().name();
            String name = e.getName();
            String extra = "";
            if (e instanceof LivingEntity le) {
                double max = 20.0;
                if (maxHealthAttr != null) {
                    var attr = le.getAttribute(maxHealthAttr);
                    if (attr != null) max = attr.getValue();
                }
                extra = String.format(", 血量 %.1f/%.1f", le.getHealth(), max);
                EntityEquipment leEq = le.getEquipment();
                if (leEq != null) {
                    ItemStack hand = leEq.getItemInMainHand();
                    if (hand != null && hand.getType() != Material.AIR) {
                        extra += ", 持 " + hand.getType().name();
                    }
                }
            }
            sb.append(String.format("  %s [%s] @ %d,%d,%d (距 %.1f%s)\n",
                    name, type, l.getBlockX(), l.getBlockY(), l.getBlockZ(), dist, extra));
            count++;
            if (count >= 20) {
                sb.append("  ...(更多实体省略)\n");
                break;
            }
        }
        if (count == 0) sb.append("  (附近无实体)\n");
        return sb.toString();
    }

    private String scanNearbyPlayers(LivingEntity self, int radius) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Player p : self.getWorld().getPlayers()) {
            if (p.equals(self)) continue;
            double dist = LocationUtil.safeDistance(p.getLocation(), self.getLocation());
            if (dist > radius) continue;
            Location l = p.getLocation();
            ItemStack hand = p.getInventory().getItemInMainHand();
            sb.append(String.format("  %s (血量 %.1f/20, 持 %s, 朝 %s) @ %d,%d,%d (距 %.1f)\n",
                    p.getName(), p.getHealth(),
                    hand != null && hand.getType() != Material.AIR ? hand.getType().name() : "无",
                    facingDirection(l.getYaw()),
                    l.getBlockX(), l.getBlockY(), l.getBlockZ(), dist));
            count++;
        }
        if (count == 0) sb.append("  (附近无玩家)\n");
        return sb.toString();
    }

    private String listAllPlayers(LivingEntity self, World world) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (p.equals(self)) continue;
            Location l = p.getLocation();
            double dist;
            try {
                dist = l.getWorld().equals(self.getWorld())
                        ? self.getLocation().distance(l) : -1;
            } catch (Exception e) {
                dist = -1;
            }
            String distStr = dist >= 0 ? String.format("%.1f", dist) : "异世界";
            sb.append(String.format("  %s @ %d,%d,%d (距 %s, 世界 %s)\n",
                    p.getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ(),
                    distStr, l.getWorld() != null ? l.getWorld().getName() : "?"));
            count++;
        }
        if (count == 0) sb.append("  (服务器无其他玩家)\n");
        return sb.toString();
    }

    /** 清除指定 NPC 的缓存（spawn/remove/死亡/世界切换时调用） */
    public void invalidateCache(UUID entityId) {
        collectCache.remove(entityId);
    }
}
