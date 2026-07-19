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

        // 2. 顺序执行命令
        for (String cmd : commands) {
            try {
                executeCommand(aiPlayer, cmd);
            } catch (Exception e) {
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
            case "walk" -> handleWalk(entity, args);
            case "walk_dir" -> handleWalkDir(entity, args);
            case "follow" -> handleFollow(aiPlayer, args);
            case "stop" -> handleStop(aiPlayer);
            case "break" -> handleBreak(entity, args);
            case "place" -> handlePlace(entity, args);
            case "attack" -> handleAttack(entity, args);
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
            default -> plugin.getLogger().warning("未知 AI 命令: " + cmd);
        }
    }

    private void handleWalk(Player entity, String[] args) {
        if (args.length < 3) return;
        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            Location target = new Location(entity.getWorld(), x, y, z);
            walkTo(entity, target);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleWalkDir(Player entity, String[] args) {
        if (args.length < 2) return;
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
        walkTo(entity, target);
    }

    private void walkTo(Player entity, Location target) {
        // 简单模拟"行走"：分多帧小步移动到目标点
        Location start = entity.getLocation();
        double totalDist = start.distance(target);
        if (totalDist < 0.5) return;
        double speed = plugin.getConfigManager().getMoveSpeed();
        int steps = (int) Math.min(40, Math.ceil(totalDist / speed));
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

    private void handleAttack(Player entity, String[] args) {
        if (args.length < 1) return;
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
        double damage = plugin.getConfigManager().getAttackDamage();
        target.damage(damage, entity);
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
}
