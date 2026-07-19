package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令解析器与执行器
 * <p>
 * 从 LLM 回复中提取 [COMMAND:...] 命令并执行，
 * 同时把非命令部分作为对话文字广播给玩家。
 */
public class CommandExecutor {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\[COMMAND:([^\\]]+)]");

    private final AIPlayerPlugin plugin;

    public CommandExecutor(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 处理 AI 的完整回复：
     * 1. 提取所有 [COMMAND:...] 并按顺序执行
     * 2. 把剩余文字作为对话内容广播（不显示命令本身）
     */
    public void execute(AIPlayer aiPlayer, String reply) {
        if (reply == null || reply.isEmpty()) return;

        Matcher matcher = COMMAND_PATTERN.matcher(reply);
        List<String> commands = new ArrayList<>();
        StringBuilder spoken = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // 命令前的文本作为对话内容
            if (matcher.start() > lastEnd) {
                spoken.append(reply, lastEnd, matcher.start());
            }
            commands.add(matcher.group(1).trim());
            lastEnd = matcher.end();
        }
        if (lastEnd < reply.length()) {
            spoken.append(reply.substring(lastEnd));
        }

        // 1. 广播对话文字
        String spokenText = spoken.toString().trim();
        if (!spokenText.isEmpty()) {
            aiPlayer.sayInChat(spokenText);
        }

        // 埋点：对话计数（功能 1）—— 每轮 LLM 回复计为一次对话
        aiPlayer.getStats().incChat();

        // 2. 顺序执行命令
        for (String cmd : commands) {
            try {
                executeCommand(aiPlayer, cmd);
                // 埋点：命令成功（功能 1）
                aiPlayer.getStats().incCommand(true);
            } catch (Exception e) {
                // 埋点：命令失败（功能 1）
                aiPlayer.getStats().incCommand(false);
                plugin.getLogger().warning("执行命令失败 [" + cmd + "]: " + e.getMessage());
            }
        }
    }

    private void executeCommand(AIPlayer aiPlayer, String fullCommand) {
        String[] parts = fullCommand.split("\\s+");
        if (parts.length == 0) return;
        String cmd = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Player entity = aiPlayer.getEntity();
        if (entity == null || !entity.isValid()) {
            plugin.getLogger().warning("AI 实体不存在: " + aiPlayer.getName());
            return;
        }

        switch (cmd) {
            case "walk" -> handleWalk(aiPlayer, args);
            case "walk_dir" -> handleWalkDir(aiPlayer, args);
            case "follow" -> handleFollow(aiPlayer, args);
            case "stop" -> handleStop(aiPlayer);
            case "break" -> handleBreak(entity, args);
            case "place" -> handlePlace(entity, args);
            case "attack" -> handleAttack(aiPlayer, args);
            case "jump" -> handleJump(entity);
            case "look" -> handleLook(entity, args);
            case "say" -> handleSay(aiPlayer, args);
            case "cmd" -> handleCmd(aiPlayer, args);
            case "equip" -> handleEquip(entity, args);
            case "drop" -> handleDrop(entity, args);
            // 姿态/动作
            case "sit" -> handleSit(entity);
            case "sleep" -> handleSleep(entity);
            case "sneak" -> handleSneak(entity);
            case "stand" -> handleStand(entity);
            case "wave" -> handleWave(entity);
            case "dance" -> handleDance(entity);
            case "swing" -> plugin.getNpcAnimator().swingArm(entity);
            case "look_at_player" -> handleLookAtPlayer(entity, args);
            case "approach" -> handleApproach(entity, args);
            // 新增动作
            case "face" -> handleFace(entity, args);
            case "pickup" -> handlePickup(entity);
            case "pickup_all" -> handlePickupAll(entity);
            case "use_item" -> handleUseItem(entity);
            case "interact" -> handleInteractBlock(entity, args);
            case "mount" -> handleMount(entity, args);
            case "dismount" -> entity.leaveVehicle();
            case "eat" -> handleEat(entity);
            case "throw_item" -> handleThrowItem(entity);
            case "give_random" -> handleGiveRandom(entity, args);
            case "teleport_player" -> handleTeleportPlayer(entity, args);
            case "respawn" -> handleRespawn(entity);
            case "set_health" -> handleSetHealth(entity, args);
            case "set_food" -> handleSetFood(entity, args);
            case "weather" -> handleWeather(entity, args);
            case "time" -> handleTime(entity, args);
            case "broadcast" -> handleBroadcast(aiPlayer, args);
            case "kill" -> handleKill(entity);
            case "heal" -> handleHeal(entity, args);
            case "feed" -> handleFeed(entity, args);
            case "gamemode" -> handleGamemode(entity, args);
            case "fly" -> handleFly(entity, args);
            case "ignite" -> handleIgnite(entity, args);
            case "extinguish" -> handleExtinguish(entity);
            case "strike" -> handleStrike(entity, args);
            case "explode" -> handleExplode(entity, args);
            case "spawnmob" -> handleSpawnMob(entity, args);
            case "xp" -> handleXp(entity, args);
            case "clearinv" -> handleClearInv(entity);
            case "rename" -> handleRename(aiPlayer, args);
            case "ride" -> handleRide(entity, args);
            case "carry" -> handleCarry(entity, args);
            case "duplicate" -> handleDuplicate(entity);
            case "openinv" -> handleOpenInv(entity, args);
            case "home" -> handleHome(entity);
            case "top" -> handleTop(entity);
            case "combo" -> handleCombo(entity, args);
            case "emote" -> handleEmote(entity, args);
            default -> plugin.getLogger().warning("未知 AI 命令: " + cmd);
        }
    }

    /** face <x> <y> <z> 或 face_player <玩家名> —— 朝向某点或某玩家 */
    private void handleFace(Player entity, String[] args) {
        if (args.length >= 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                Location target = new Location(entity.getWorld(), x, y, z);
                NpcHelper.faceLocation(entity, target);
                return;
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("player")) {
            // face player <name>
            if (args.length >= 2) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target != null) {
                    NpcHelper.faceLocation(entity, target.getLocation());
                }
            }
        }
    }

    /** 捡起附近 5 格内的所有掉落物 */
    private void handlePickup(Player entity) {
        double radius = 5.0;
        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof org.bukkit.entity.Item item) {
                // 直接把物品加入背包
                org.bukkit.inventory.ItemStack stack = item.getItemStack();
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow =
                        entity.getInventory().addItem(stack);
                if (overflow.isEmpty()) {
                    e.remove();
                } else {
                    // 装不下，更新剩余数量
                    int remaining = overflow.values().stream().mapToInt(org.bukkit.inventory.ItemStack::getAmount).sum();
                    item.getItemStack().setAmount(remaining);
                }
            }
        }
    }

    /** 捡起附近 10 格内所有掉落物 */
    private void handlePickupAll(Player entity) {
        for (Entity e : entity.getNearbyEntities(10, 10, 10)) {
            if (e instanceof org.bukkit.entity.Item item) {
                org.bukkit.inventory.ItemStack stack = item.getItemStack();
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow =
                        entity.getInventory().addItem(stack);
                if (overflow.isEmpty()) {
                    e.remove();
                } else {
                    int remaining = overflow.values().stream().mapToInt(org.bukkit.inventory.ItemStack::getAmount).sum();
                    item.getItemStack().setAmount(remaining);
                }
            }
        }
    }

    /** 使用手持物（吃食物、喝药水等） */
    private void handleUseItem(Player entity) {
        org.bukkit.inventory.ItemStack hand = entity.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;
        // 模拟：如果是食物，立即消耗并恢复饥饿
        if (hand.getType().isEdible()) {
            entity.setFoodLevel(Math.min(20, entity.getFoodLevel() + 5));
            if (hand.getAmount() > 1) {
                hand.setAmount(hand.getAmount() - 1);
            } else {
                entity.getInventory().setItemInMainHand(null);
            }
        }
        // 药水等需要复杂逻辑，这里简化
    }

    /** interact <x> <y> <z> —— 与方块交互（开门、按按钮、开箱子等） */
    private void handleInteractBlock(Player entity, String[] args) {
        if (args.length < 3) return;
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            Block b = entity.getWorld().getBlockAt(x, y, z);
            // 简化：如果是门/按钮/拉杆，模拟红石信号
            if (b.getType().name().endsWith("_DOOR") || b.getType().name().endsWith("_TRAPDOOR")
                    || b.getType().name().endsWith("_FENCE_GATE")
                    || b.getType().name().endsWith("_BUTTON") || b.getType() == Material.LEVER) {
                // 用 BlockState 切换开关
                org.bukkit.block.data.Openable openable =
                        (org.bukkit.block.data.Openable) b.getBlockData();
                openable.setOpen(!openable.isOpen());
                b.setBlockData(openable);
            }
        } catch (NumberFormatException ignored) {
        } catch (ClassCastException ignored) {
            // 不是 Openable，忽略
        }
    }

    /** mount <实体名|nearest> —— 骑乘附近实体（矿车、船、马等） */
    private void handleMount(Player entity, String[] args) {
        if (args.length < 1) return;
        String target = args[0];
        Entity mount = null;
        if (target.equalsIgnoreCase("nearest")) {
            double minDist = Double.MAX_VALUE;
            for (Entity e : entity.getNearbyEntities(10, 10, 10)) {
                if (e instanceof org.bukkit.entity.Vehicle v) {
                    double d = e.getLocation().distance(entity.getLocation());
                    if (d < minDist) {
                        minDist = d;
                        mount = e;
                    }
                }
            }
        } else {
            for (Entity e : entity.getNearbyEntities(10, 10, 10)) {
                if (e.getName().equalsIgnoreCase(target)) {
                    mount = e;
                    break;
                }
            }
        }
        if (mount != null) {
            mount.addPassenger(entity);
        }
    }

    /** eat —— 直接吃完手里食物并恢复饱食度 */
    private void handleEat(Player entity) {
        handleUseItem(entity);
    }

    /** throw_item —— 丢出主手物品（朝看的方向） */
    private void handleThrowItem(Player entity) {
        org.bukkit.inventory.ItemStack hand = entity.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;
        org.bukkit.inventory.ItemStack toThrow = hand.clone();
        // 留一份在手里
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            entity.getInventory().setItemInMainHand(null);
        }
        Location loc = entity.getLocation();
        loc.setY(loc.getY() + 1.5);
        org.bukkit.entity.Item dropped = entity.getWorld().dropItem(loc, toThrow);
        Vector dir = loc.getDirection().multiply(0.8);
        dropped.setVelocity(dir);
    }

    /** give_random <物品类型> [数量] —— 给 AI 加物品（测试用，可视为捡到） */
    private void handleGiveRandom(Player entity, String[] args) {
        if (args.length < 1) return;
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) return;
        int amount = args.length >= 2 ? Math.min(64, safeParse(args[1], 1)) : 1;
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(mat, amount);
        entity.getInventory().addItem(stack);
    }

    /** teleport_player <玩家名> <x> <y> <z> —— 把玩家传送到坐标（OP 用，安全） */
    private void handleTeleportPlayer(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) return;
        if (args.length < 4) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) return;
        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            Location loc = new Location(entity.getWorld(), x, y, z);
            target.teleport(loc);
        } catch (NumberFormatException ignored) {
        }
    }

    /** respawn —— 强制重生（如果死了） */
    private void handleRespawn(Player entity) {
        if (entity.isDead()) {
            try {
                entity.spigot().respawn();
            } catch (Exception ignored) {
            }
        }
    }

    /** set_health <数值> —— 设置血量（需要 OP 权限） */
    private void handleSetHealth(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) return;
        if (args.length < 1) return;
        try {
            double h = Math.min(20, Math.max(0, Double.parseDouble(args[0])));
            entity.setHealth(h);
        } catch (NumberFormatException ignored) {
        }
    }

    /** set_food <数值> —— 设置饱食度（需要 OP 权限） */
    private void handleSetFood(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) return;
        if (args.length < 1) return;
        try {
            int f = Math.min(20, Math.max(0, Integer.parseInt(args[0])));
            entity.setFoodLevel(f);
        } catch (NumberFormatException ignored) {
        }
    }

    /** weather <sun|rain|storm> —— 改变天气（需要 OP 权限） */
    private void handleWeather(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) return;
        if (args.length < 1) return;
        String w = args[0].toLowerCase();
        switch (w) {
            case "sun", "clear" -> {
                entity.getWorld().setStorm(false);
                entity.getWorld().setThundering(false);
            }
            case "rain" -> {
                entity.getWorld().setStorm(true);
                entity.getWorld().setThundering(false);
            }
            case "storm", "thunder" -> {
                entity.getWorld().setStorm(true);
                entity.getWorld().setThundering(true);
            }
        }
    }

    /** time <day|night|dawn|dusk|数值> —— 改变时间（需要 OP 权限） */
    private void handleTime(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) return;
        if (args.length < 1) return;
        String t = args[0].toLowerCase();
        long ticks = switch (t) {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "dawn" -> 0L;
            case "dusk" -> 12000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            default -> {
                try {
                    yield Long.parseLong(t);
                } catch (NumberFormatException e) {
                    yield -1L;
                }
            }
        };
        if (ticks >= 0) {
            entity.getWorld().setTime(ticks);
        }
    }

    /** broadcast <消息> —— 服务器广播消息 */
    private void handleBroadcast(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        String msg = String.join(" ", args);
        Bukkit.broadcastMessage("§e[" + aiPlayer.getName() + "] §f" + msg);
    }

    private int safeParse(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private void handleWalk(AIPlayer aiPlayer, String[] args) {
        if (args.length < 3) return;
        Player entity = aiPlayer.getEntity();
        if (entity == null) return;
        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            Location target = new Location(entity.getWorld(), x, y, z);
            walkTo(aiPlayer, target);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleWalkDir(AIPlayer aiPlayer, String[] args) {
        if (args.length < 2) return;
        Player entity = aiPlayer.getEntity();
        if (entity == null) return;
        String dir = args[0].toLowerCase();
        double dist;
        try {
            dist = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            return;
        }
        Location loc = entity.getLocation();
        Vector v;
        switch (dir) {
            case "north" -> v = new Vector(0, 0, -1);
            case "south" -> v = new Vector(0, 0, 1);
            case "east" -> v = new Vector(1, 0, 0);
            case "west" -> v = new Vector(-1, 0, 0);
            case "up" -> v = new Vector(0, 1, 0);
            case "down" -> v = new Vector(0, -1, 0);
            default -> {
                return;
            }
        }
        Location target = loc.clone().add(v.multiply(dist));
        walkTo(aiPlayer, target);
    }

    private void walkTo(AIPlayer aiPlayer, Location target) {
        Player entity = aiPlayer.getEntity();
        if (entity == null || !entity.isValid()) return;

        Location start = entity.getLocation();
        double totalDist;
        try {
            totalDist = start.distance(target);
        } catch (Exception e) {
            return;
        }

        // 埋点：统计行走距离（功能 1）
        aiPlayer.getStats().addWalk(totalDist);

        // 优先使用后端寻路（Citizens 的 A* 寻路会绕过障碍）
        double speed = plugin.getConfigManager().getMoveSpeed();
        boolean navigated = NpcHelper.navigateTo(entity, target, speed);
        if (navigated) return;

        // 回退方案：分帧 teleport 模拟"行走"（NMS 后端）
        if (totalDist < 0.5) return;
        int steps = Math.max(1, (int) Math.min(60, Math.ceil(totalDist / speed)));
        Vector step = target.toVector().subtract(start.toVector()).multiply(1.0 / steps);

        new org.bukkit.scheduler.BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= steps || !entity.isValid()) {
                    cancel();
                    return;
                }
                Location next = entity.getLocation().add(step);
                // 保留地面高度（避免穿墙下降）
                next.setY(entity.getLocation().getY());
                entity.teleport(next);
                i++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void handleFollow(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        aiPlayer.setFollowing(args[0]);
        aiPlayer.startFollowTask();
    }

    private void handleStop(AIPlayer aiPlayer) {
        aiPlayer.setFollowing(null);
    }

    private void handleBreak(Player entity, String[] args) {
        if (args.length < 3) return;
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            Block b = entity.getWorld().getBlockAt(x, y, z);
            Material type = b.getType();
            if (type.isAir() || type == Material.BEDROCK) return;
            // 模拟掉落
            b.breakNaturally(new ItemStack(Material.DIAMOND_PICKAXE));
        } catch (NumberFormatException ignored) {
        }
    }

    private void handlePlace(Player entity, String[] args) {
        if (args.length < 4) return;
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            Material mat = Material.matchMaterial(args[3].toLowerCase());
            if (mat == null || !mat.isBlock()) {
                plugin.getLogger().warning("无效方块类型: " + args[3]);
                return;
            }
            Block b = entity.getWorld().getBlockAt(x, y, z);
            b.setType(mat);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleAttack(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        Player entity = aiPlayer.getEntity();
        if (entity == null) return;
        String targetName = String.join(" ", args);
        LivingEntity target = null;
        if (targetName.equalsIgnoreCase("nearest")) {
            // 攻击最近的非玩家生物
            double radius = plugin.getConfigManager().getEntityScanRadius();
            double minDist = Double.MAX_VALUE;
            for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
                if (e instanceof LivingEntity && !(e instanceof Player)) {
                    double d = e.getLocation().distance(entity.getLocation());
                    if (d < minDist) {
                        minDist = d;
                        target = (LivingEntity) e;
                    }
                }
            }
        } else {
            // 按名称匹配玩家或实体
            Player p = Bukkit.getPlayerExact(targetName);
            if (p != null) {
                target = p;
            } else {
                for (Entity e : entity.getNearbyEntities(16, 16, 16)) {
                    if (e instanceof LivingEntity && e.getName().equalsIgnoreCase(targetName)) {
                        target = (LivingEntity) e;
                        break;
                    }
                }
            }
        }
        if (target == null) {
            plugin.getLogger().info("未找到攻击目标: " + targetName);
            return;
        }

        // 检查：如果目标也是 AI 玩家，应用队伍 / 关系约束（功能 4 / 功能 6）
        AIPlayer targetAi = plugin.getAiPlayerManager().getByEntity(target.getUniqueId());
        if (targetAi != null) {
            String attackerName = aiPlayer.getName();
            String targetAiName = targetAi.getName();
            // 同队不攻击（功能 4）
            if (plugin.getTeamManager().sameTeam(attackerName, targetAiName)) {
                plugin.getLogger().info(attackerName + " 与 " + targetAiName + " 同队，不攻击");
                return;
            }
            // 关系值 > -50 不攻击（功能 6）
            if (plugin.getRelationManager().get(attackerName, targetAiName) > -50) {
                plugin.getLogger().info(attackerName + " 对 " + targetAiName + " 关系值 > -50，不攻击");
                return;
            }
        }

        double damage = plugin.getConfigManager().getAttackDamage();
        target.damage(damage, entity);

        // 埋点：每次攻击都计为击杀（功能 1，简化口径）
        aiPlayer.getStats().incKill();
        // 情绪：攻击命中后心情提升（功能 9）
        aiPlayer.adjustMood(5);
    }

    private void handleJump(Player entity) {
        Vector v = entity.getVelocity();
        v.setY(0.5);
        entity.setVelocity(v);
    }

    private void handleLook(Player entity, String[] args) {
        if (args.length < 1) return;
        String dir = args[0].toLowerCase();
        Location loc = entity.getLocation();
        switch (dir) {
            case "north" -> loc.setYaw(180);
            case "south" -> loc.setYaw(0);
            case "east" -> loc.setYaw(-90);
            case "west" -> loc.setYaw(90);
            case "up" -> loc.setPitch(-90);
            case "down" -> loc.setPitch(90);
            default -> {
                return;
            }
        }
        entity.teleport(loc);
    }

    private void handleSay(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        String msg = String.join(" ", args);
        aiPlayer.sayInChat(msg);
        // 埋点：对话计数（功能 1）
        aiPlayer.getStats().incChat();
        // 情绪：说话后心情略微提升（功能 9）
        aiPlayer.adjustMood(2);
    }

    private void handleCmd(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            plugin.getLogger().warning("AI 尝试执行服务器命令，但已被配置禁用: " + String.join(" ", args));
            return;
        }
        String fullCmd = String.join(" ", args);
        // 以控制台身份执行
        CommandSender console = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(console, fullCmd);
    }

    private void handleEquip(Player entity, String[] args) {
        if (args.length < 2) return;
        String slot = args[0].toLowerCase();
        Material mat = Material.matchMaterial(args[1].toUpperCase());
        if (mat == null) {
            plugin.getLogger().warning("无效物品: " + args[1]);
            return;
        }
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;
        ItemStack item = new ItemStack(mat);
        switch (slot) {
            case "hand" -> eq.setItemInMainHand(item);
            case "helmet" -> eq.setHelmet(item);
            case "chest" -> eq.setChestplate(item);
            case "legs" -> eq.setLeggings(item);
            case "boots" -> eq.setBoots(item);
            default -> plugin.getLogger().warning("未知装备槽位: " + slot);
        }
    }

    private void handleDrop(Player entity, String[] args) {
        if (args.length < 1) return;
        String slot = args[0].toLowerCase();
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;
        ItemStack toDrop = null;
        switch (slot) {
            case "hand" -> {
                toDrop = eq.getItemInMainHand();
                eq.setItemInMainHand(null);
            }
            case "helmet" -> {
                toDrop = eq.getHelmet();
                eq.setHelmet(null);
            }
            case "chest" -> {
                toDrop = eq.getChestplate();
                eq.setChestplate(null);
            }
            case "legs" -> {
                toDrop = eq.getLeggings();
                eq.setLeggings(null);
            }
            case "boots" -> {
                toDrop = eq.getBoots();
                eq.setBoots(null);
            }
        }
        if (toDrop != null && !toDrop.getType().isAir()) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), toDrop);
        }
    }

    // ===== 姿态/动作 =====

    private void handleSit(Player entity) {
        plugin.getNpcAnimator().sit(entity);
    }

    private void handleSleep(Player entity) {
        plugin.getNpcAnimator().sleep(entity);
    }

    private void handleSneak(Player entity) {
        plugin.getNpcAnimator().sneak(entity);
    }

    private void handleStand(Player entity) {
        plugin.getNpcAnimator().stand(entity);
    }

    private void handleWave(Player entity) {
        plugin.getNpcAnimator().wave(entity);
    }

    private void handleDance(Player entity) {
        plugin.getNpcAnimator().dance(entity);
    }

    private void handleLookAtPlayer(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getLogger().info("look_at_player: 玩家不在线 " + args[0]);
            return;
        }
        plugin.getNpcAnimator().lookAtPlayerTemporarily(entity, target, 100);
    }

    private void handleApproach(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getLogger().info("approach: 玩家不在线 " + args[0]);
            return;
        }
        plugin.getNpcAnimator().approachPlayer(entity, target);
    }

    // ===== 新增 20 个功能命令 =====

    /** kill —— 自杀（测试用） */
    private void handleKill(Player entity) {
        entity.setHealth(0);
    }

    /** heal [数值] —— 恢复血量，默认 20 */
    private void handleHeal(Player entity, String[] args) {
        double amount = args.length >= 1 ? safeParseDouble(args[0], 20) : 20;
        entity.setHealth(Math.min(20, entity.getHealth() + amount));
    }

    /** feed [数值] —— 恢复饱食度，默认 20 */
    private void handleFeed(Player entity, String[] args) {
        int amount = args.length >= 1 ? safeParse(args[0], 20) : 20;
        entity.setFoodLevel(Math.min(20, entity.getFoodLevel() + amount));
    }

    /** gamemode <survival|creative|adventure|spectator> */
    private void handleGamemode(Player entity, String[] args) {
        if (args.length < 1) return;
        try {
            org.bukkit.GameMode gm = org.bukkit.GameMode.valueOf(args[0].toUpperCase());
            entity.setGameMode(gm);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /** fly <true|false> —— 开关飞行 */
    private void handleFly(Player entity, String[] args) {
        boolean fly = args.length < 1 || Boolean.parseBoolean(args[0]);
        entity.setAllowFlight(fly);
        entity.setFlying(fly);
    }

    /** ignite [秒数] —— 着火 */
    private void handleIgnite(Player entity, String[] args) {
        int seconds = args.length >= 1 ? safeParse(args[0], 5) : 5;
        entity.setFireTicks(seconds * 20);
    }

    /** extinguish —— 灭火 */
    private void handleExtinguish(Player entity) {
        entity.setFireTicks(0);
    }

    /** strike [玩家名|self] —— 雷击 */
    private void handleStrike(Player entity, String[] args) {
        Player target = entity;
        if (args.length >= 1 && !args[0].equalsIgnoreCase("self")) {
            target = Bukkit.getPlayerExact(args[0]);
        }
        if (target != null) {
            target.getWorld().strikeLightning(target.getLocation());
        }
    }

    /** explode [威力] —— 在 AI 位置产生爆炸 */
    private void handleExplode(Player entity, String[] args) {
        float power = args.length >= 1 ? (float) safeParseDouble(args[0], 2.0) : 2.0f;
        entity.getWorld().createExplosion(entity.getLocation(), power, false, true);
    }

    /** spawnmob <生物类型> [数量] */
    private void handleSpawnMob(Player entity, String[] args) {
        if (args.length < 1) return;
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.fromName(args[0].toUpperCase());
        if (type == null || !type.isAlive() || !type.isSpawnable()) return;
        int count = args.length >= 2 ? Math.min(10, safeParse(args[1], 1)) : 1;
        Location loc = entity.getLocation();
        for (int i = 0; i < count; i++) {
            entity.getWorld().spawnEntity(loc, type);
        }
    }

    /** xp <数值> —— 给经验 */
    private void handleXp(Player entity, String[] args) {
        int amount = args.length >= 1 ? safeParse(args[0], 10) : 10;
        entity.giveExp(amount);
    }

    /** clearinv —— 清空背包 */
    private void handleClearInv(Player entity) {
        entity.getInventory().clear();
    }

    /** rename <新名字> —— 重命名 AI */
    private void handleRename(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        String newName = String.join(" ", args);
        Player entity = aiPlayer.getEntity();
        if (entity != null) {
            entity.setCustomName("§e" + newName);
            entity.setCustomNameVisible(true);
        }
    }

    /** ride <玩家名> —— 骑乘玩家 */
    private void handleRide(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && !target.equals(entity)) {
            target.addPassenger(entity);
        }
    }

    /** carry <玩家名> —— 让目标骑乘 AI */
    private void handleCarry(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && !target.equals(entity)) {
            entity.addPassenger(target);
        }
    }

    /** duplicate —— 复制主手物品 */
    private void handleDuplicate(Player entity) {
        ItemStack hand = entity.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;
        entity.getInventory().addItem(hand.clone());
    }

    /** openinv <玩家名> —— 打开目标玩家背包 */
    private void handleOpenInv(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && !target.equals(entity)) {
            entity.openInventory(target.getInventory());
        }
    }

    /** home —— 传送到出生点 */
    private void handleHome(Player entity) {
        Location bed = entity.getRespawnLocation();
        if (bed != null) {
            entity.teleport(bed);
        } else {
            entity.teleport(entity.getWorld().getSpawnLocation());
        }
    }

    /** top —— 传送到头顶最高处 */
    private void handleTop(Player entity) {
        Location loc = entity.getLocation();
        int y = entity.getWorld().getHighestBlockYAt(loc);
        loc.setY(y + 1);
        entity.teleport(loc);
    }

    /** combo <玩家名> [次数] —— 连续攻击 */
    private void handleCombo(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(entity)) return;
        int times = args.length >= 2 ? Math.min(10, safeParse(args[1], 3)) : 3;
        double damage = plugin.getConfigManager().getAttackDamage();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= times || !entity.isValid() || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                target.damage(damage, entity);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 8L);
    }

    /** emote <表情> —— 表情动作 */
    private void handleEmote(Player entity, String[] args) {
        if (args.length < 1) return;
        String emote = args[0].toLowerCase();
        switch (emote) {
            case "bow" -> entity.swingMainHand();
            case "clap" -> {
                plugin.getNpcAnimator().swingArm(entity);
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getNpcAnimator().swingArm(entity), 5L);
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getNpcAnimator().swingArm(entity), 10L);
            }
            case "laugh" -> entity.sendMessage("§e哈哈哈哈哈！");
            case "cry" -> entity.sendMessage("§7呜呜呜...");
            case "angry" -> entity.sendMessage("§c哼！");
            default -> plugin.getNpcAnimator().wave(entity);
        }
    }

    private double safeParseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }
}
