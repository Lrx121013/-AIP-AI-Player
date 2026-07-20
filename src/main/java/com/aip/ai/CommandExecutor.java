package com.aip.ai;

import com.aip.AIPlayerPlugin;
import com.aip.util.LocationUtil;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    /** P1：命令文档缓存（构造时一次性生成，避免每次对话都反射扫描） */
    private final String cachedDocs;

    public CommandExecutor(AIPlayerPlugin plugin) {
        this.plugin = plugin;
        this.cachedDocs = getCommandDocs();
    }

    /** P1：获取缓存的命令文档 */
    public String getCachedDocs() {
        return cachedDocs;
    }

    /**
     * 处理 AI 的完整回复（兼容入口，忽略返回值）：
     * 1. 提取所有 [COMMAND:...] 并按顺序执行
     * 2. 把剩余文字作为对话内容广播（不显示命令本身）
     * <p>
     * 内部委托给 {@link #executeWithResult}，并自动把最后一条命令的结果存入 aiPlayer。
     */
    public void execute(AIPlayer aiPlayer, String reply) {
        executeWithResult(aiPlayer, reply);
    }

    /**
     * 处理 AI 的完整回复并返回执行结果：
     * 1. 提取所有 [COMMAND:...] 并按顺序执行
     * 2. 把剩余文字作为对话内容广播（不显示命令本身）
     * 3. 把最后一条命令的执行结果存入 aiPlayer（供下一轮对话回流给 LLM）
     *
     * @return 最后一条命令的 {@link ExecutionResult}；若回复中无命令则返回 null
     */
    public ExecutionResult executeWithResult(AIPlayer aiPlayer, String reply) {
        if (reply == null || reply.isEmpty()) return null;

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

        // 广播前校验实体有效性，避免对已失效实体执行操作
        Player entityForChat = aiPlayer.getEntity();
        if (entityForChat == null || !entityForChat.isValid()) {
            plugin.getLogger().fine("AI " + aiPlayer.getName() + " 实体已失效，跳过本轮广播与命令执行");
            return null;
        }

        if (!spokenText.isEmpty()) {
            aiPlayer.sayInChat(spokenText);
        }

        // 埋点：对话计数（功能 1）—— 每轮 LLM 回复计为一次对话
        aiPlayer.getStats().incChat();

        // 2. 顺序执行命令，记录最后一条结果
        ExecutionResult lastResult = null;
        int skippedCommands = 0;
        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            ExecutionResult result;
            try {
                result = dispatchCommand(aiPlayer, cmd);
            } catch (Exception e) {
                // dispatchCommand 内部已捕获大部分异常；这里是兜底
                String cmdName = cmd.split("\\s+")[0].toLowerCase();
                result = new ExecutionResult(cmdName, false, e.getMessage());
            }
            if (result.isSuccess()) {
                // 埋点：命令成功（功能 1）
                aiPlayer.getStats().incCommand(true);
            } else {
                // 埋点：命令失败（功能 1）
                aiPlayer.getStats().incCommand(false);
                plugin.getLogger().warning("执行命令失败 [" + cmd + "]: " + result.getReason());
            }
            lastResult = result;

            // 实体死亡后跳过剩余命令（如 kill 命令后实体已失效）
            if (aiPlayer.getEntity() == null || !aiPlayer.getEntity().isValid()) {
                skippedCommands = commands.size() - i - 1;
                break;
            }
        }
        if (skippedCommands > 0) {
            plugin.getLogger().info("AI 已死亡，跳过剩余 " + skippedCommands + " 条命令");
        }

        // 3. 把最后一条命令结果存入 aiPlayer，供下一轮对话回流给 LLM
        if (lastResult != null) {
            aiPlayer.setLastCommandResult(lastResult);
        }
        return lastResult;
    }

    /**
     * 分发并执行单条 [COMMAND:xxx args] 命令，返回执行结果。
     * 未知命令返回 (cmd, false, "未知命令")；
     * 抛出的异常返回 (cmd, false, e.getMessage())。
     */
    private ExecutionResult dispatchCommand(AIPlayer aiPlayer, String fullCommand) {
        String[] parts = fullCommand.split("\\s+");
        if (parts.length == 0) {
            return new ExecutionResult("", false, "空命令");
        }
        String cmd = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Player entity = aiPlayer.getEntity();
        if (entity == null || !entity.isValid()) {
            plugin.getLogger().warning("AI 实体不存在: " + aiPlayer.getName());
            return new ExecutionResult(cmd, false, "AI 实体不存在");
        }

        try {
            // P4：审批检查（require-approval-for 列表内的命令需 OP 批准）
            if (cmd.equals("op") || cmd.equals("deop") || cmd.equals("ban") || cmd.equals("kick") || cmd.equals("tp_all")) {
                if (!plugin.getApprovalManager().requestApproval(aiPlayer, cmd)) {
                    return new ExecutionResult(cmd, false, "审批被拒绝");
                }
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
                case "swing" -> handleSwing(entity);
                case "look_at_player" -> handleLookAtPlayer(entity, args);
                case "approach" -> handleApproach(entity, args);
                // 新增动作
                case "face" -> handleFace(entity, args);
                case "pickup" -> handlePickup(entity);
                case "pickup_all" -> handlePickupAll(entity);
                case "use_item" -> handleUseItem(entity);
                case "interact" -> handleInteractBlock(entity, args);
                case "mount" -> handleMount(entity, args);
                case "dismount" -> handleDismount(entity);
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
                case "strategy" -> handleStrategy(aiPlayer, args);
                // 反射规则命令
                case "reflex_add" -> handleReflexAdd(aiPlayer, args);
                case "reflex_list" -> handleReflexList(aiPlayer);
                case "reflex_remove" -> handleReflexRemove(aiPlayer, args);
                case "reflex_clear" -> handleReflexClear(aiPlayer);
                case "reflex_toggle" -> handleReflexToggle(aiPlayer, args);
                // P3：查询命令（返回 String，结果回流给 AIPlayer 下一轮注入）
                case "query_players" -> {
                    String result = handleQueryPlayers(entity, args);
                    aiPlayer.setLastQueryResult(result);
                }
                case "query_nearby" -> {
                    String result = handleQueryNearby(entity, args);
                    aiPlayer.setLastQueryResult(result);
                }
                case "query_inventory" -> {
                    String result = handleQueryInventory(entity, args);
                    aiPlayer.setLastQueryResult(result);
                }
                case "query_block" -> {
                    String result = handleQueryBlock(entity, args);
                    aiPlayer.setLastQueryResult(result);
                }
                case "query_player" -> {
                    String result = handleQueryPlayer(entity, args);
                    aiPlayer.setLastQueryResult(result);
                }
                // P4：服务器级控制命令（受 allow-op-commands 控制）
                case "op" -> handleOp(entity, args);
                case "deop" -> handleDeop(entity, args);
                case "ban" -> handleBan(entity, args);
                case "kick" -> handleKick(entity, args);
                case "tp_all" -> handleTpAll(entity, args);
                case "gamemode_player" -> handleGamemodePlayer(entity, args);
                default -> {
                    plugin.getLogger().warning("未知 AI 命令: " + cmd);
                    return new ExecutionResult(cmd, false, "未知命令");
                }
            }
            return new ExecutionResult(cmd, true, "");
        } catch (Exception e) {
            return new ExecutionResult(cmd, false, e.getMessage());
        }
    }

    /**
     * 兼容 1.21+ 新 Attribute API：通过 Registry 按名取 Attribute，避免直接引用已移除的 GENERIC_* 常量。
     */
    private org.bukkit.attribute.Attribute resolveAttribute(String key) {
        try {
            return org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft(key));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 读取实体真实 maxHealth，失败回退 20.0 */
    private double readMaxHealth(org.bukkit.entity.LivingEntity entity) {
        try {
            org.bukkit.attribute.Attribute attr = resolveAttribute("max_health");
            if (attr != null) {
                var inst = entity.getAttribute(attr);
                if (inst != null) return inst.getValue();
            }
        } catch (Throwable ignored) {
        }
        return 20.0;
    }

    /**
     * 反射扫描本类所有带 {@link AICommand} 注解的方法，按 category 分组生成命令文档。
     * 文档格式与原 config.yml 中的硬编码命令清单保持一致，便于直接注入 system prompt。
     */
    public String getCommandDocs() {
        // 收集所有 @AICommand 注解
        List<AICommand> all = new ArrayList<>();
        for (Method method : this.getClass().getDeclaredMethods()) {
            AICommand ann = method.getAnnotation(AICommand.class);
            if (ann != null) all.add(ann);
        }

        // 预定义 category 顺序，保证文档可读性
        List<String> categoryOrder = List.of(
                "查询", "移动", "视角", "方块", "战斗", "物品", "聊天", "姿态", "动作", "自身状态", "OP", "策略", "其他", "反射"
        );
        Map<String, List<AICommand>> grouped = new LinkedHashMap<>();
        for (String cat : categoryOrder) grouped.put(cat, new ArrayList<>());
        for (AICommand ann : all) {
            grouped.computeIfAbsent(ann.category(), k -> new ArrayList<>()).add(ann);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 可用命令列表（自动生成，请勿修改）");
        for (String cat : categoryOrder) {
            List<AICommand> list = grouped.get(cat);
            if (list == null || list.isEmpty()) continue;
            // 同类命令按 name 排序，输出稳定
            list.sort(Comparator.comparing(AICommand::name));
            sb.append("\n# ").append(cat).append("\n");
            for (AICommand ann : list) {
                sb.append("- [COMMAND:").append(ann.name());
                if (!ann.args().isEmpty()) {
                    sb.append(" ").append(ann.args());
                }
                sb.append("] —— ").append(ann.desc());
                if (ann.op()) {
                    sb.append("（需要 OP 权限）");
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** face <x> <y> <z> 或 face_player <玩家名> —— 朝向某点或某玩家 */
    @AICommand(name = "face", desc = "朝向某点或某玩家", args = "x y z | player 玩家名", category = "视角")
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
    @AICommand(name = "pickup", desc = "捡起附近 5 格内掉落物", category = "物品")
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
    @AICommand(name = "pickup_all", desc = "捡起附近 10 格内所有掉落物", category = "物品")
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
    @AICommand(name = "use_item", desc = "使用手持物（吃食物、喝药水等）", category = "物品")
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
    @AICommand(name = "interact", desc = "与方块交互（开门、按按钮、开箱子等）", args = "x y z", category = "方块")
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
    @AICommand(name = "mount", desc = "骑乘附近实体（矿车、船、马等）", args = "实体名|nearest", category = "移动")
    private void handleMount(Player entity, String[] args) {
        if (args.length < 1) return;
        String target = args[0];
        Entity mount = null;
        if (target.equalsIgnoreCase("nearest")) {
            double minDist = Double.MAX_VALUE;
            for (Entity e : entity.getNearbyEntities(10, 10, 10)) {
                if (e instanceof org.bukkit.entity.Vehicle v) {
                    double d = LocationUtil.safeDistance(e.getLocation(), entity.getLocation());
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
    @AICommand(name = "eat", desc = "吃手里食物恢复饱食度", category = "物品")
    private void handleEat(Player entity) {
        handleUseItem(entity);
    }

    /** throw_item —— 丢出主手物品（朝看的方向） */
    @AICommand(name = "throw_item", desc = "朝看的方向丢出主手物品", category = "物品")
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
    @AICommand(name = "give_random", desc = "给自己加物品（测试用）", args = "物品类型 [数量]", category = "物品")
    private void handleGiveRandom(Player entity, String[] args) {
        if (args.length < 1) return;
        Material mat = Material.matchMaterial(args[0].toUpperCase());
        if (mat == null) return;
        int amount = args.length >= 2 ? Math.min(64, safeParse(args[1], 1)) : 1;
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(mat, amount);
        entity.getInventory().addItem(stack);
    }

    /** teleport_player <玩家名> <x> <y> <z> —— 把玩家传送到坐标（OP 用，安全） */
    @AICommand(name = "teleport_player", desc = "把玩家传送到坐标", args = "玩家名 x y z", category = "OP", op = true)
    private void handleTeleportPlayer(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
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
    @AICommand(name = "respawn", desc = "如果死了，强制重生", category = "OP", op = true)
    private void handleRespawn(Player entity) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (entity.isDead()) {
            try {
                entity.spigot().respawn();
            } catch (Exception ignored) {
            }
        }
    }

    /** set_health <数值> —— 设置血量（需要 OP 权限） */
    @AICommand(name = "set_health", desc = "设置血量", args = "数值", category = "OP", op = true)
    private void handleSetHealth(Player entity, String[] args) {
        if (entity.isDead()) return;
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 1) return;
        try {
            double val = Double.parseDouble(args[0]);
            if (Double.isNaN(val) || Double.isInfinite(val)) return;
            double maxHealth = readMaxHealth(entity);
            double finalVal = Math.min(maxHealth, Math.max(0, val));
            entity.setHealth(finalVal);
        } catch (NumberFormatException ignored) {
        }
    }

    /** set_food <数值> —— 设置饱食度（需要 OP 权限） */
    @AICommand(name = "set_food", desc = "设置饱食度", args = "数值", category = "OP", op = true)
    private void handleSetFood(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 1) return;
        try {
            int f = Math.min(20, Math.max(0, Integer.parseInt(args[0])));
            entity.setFoodLevel(f);
        } catch (NumberFormatException ignored) {
        }
    }

    /** weather <sun|rain|storm> —— 改变天气（需要 OP 权限） */
    @AICommand(name = "weather", desc = "改变天气", args = "sun|rain|storm", category = "OP", op = true)
    private void handleWeather(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
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
    @AICommand(name = "time", desc = "改变时间", args = "day|night|dawn|dusk|数值", category = "OP", op = true)
    private void handleTime(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
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
    @AICommand(name = "broadcast", desc = "服务器广播消息", args = "消息", category = "聊天")
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

    @AICommand(name = "walk", desc = "走到指定坐标（绝对坐标，会自动寻路绕过障碍）", args = "x y z", category = "移动")
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

    @AICommand(name = "walk_dir", desc = "朝指定方向走指定距离", args = "方向 距离", category = "移动")
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
        plugin.getLogger().warning("Citizens navigateTo 失败，回退到 teleport 模拟行走: " + aiPlayer.getName() + " -> " + target);

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

    @AICommand(name = "follow", desc = "跟随某个玩家", args = "玩家名", category = "移动")
    private void handleFollow(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        aiPlayer.setFollowing(args[0]);
        aiPlayer.startFollowTask();
    }

    @AICommand(name = "stop", desc = "停止移动", category = "移动")
    private void handleStop(AIPlayer aiPlayer) {
        aiPlayer.setFollowing(null);
    }

    @AICommand(name = "break", desc = "破坏指定坐标的方块", args = "x y z", category = "方块")
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

    @AICommand(name = "place", desc = "在指定坐标放置方块", args = "x y z 方块类型", category = "方块")
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

    @AICommand(name = "attack", desc = "攻击附近实体（实体名或 nearest）", args = "目标名", category = "战斗")
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
                    double d = LocationUtil.safeDistance(e.getLocation(), entity.getLocation());
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

    @AICommand(name = "jump", desc = "跳跃", category = "移动")
    private void handleJump(Player entity) {
        Vector v = entity.getVelocity();
        v.setY(0.5);
        entity.setVelocity(v);
    }

    @AICommand(name = "look", desc = "转身朝某方向看", args = "north|south|east|west|up|down", category = "视角")
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

    @AICommand(name = "say", desc = "在聊天框发言", args = "消息内容", category = "聊天")
    private void handleSay(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        String msg = String.join(" ", args);
        aiPlayer.sayInChat(msg);
        // 埋点：对话计数（功能 1）
        aiPlayer.getStats().incChat();
        // 情绪：说话后心情略微提升（功能 9）
        aiPlayer.adjustMood(2);
    }

    @AICommand(name = "cmd", desc = "执行服务器命令（慎用）", args = "服务器命令", category = "OP", op = true)
    private void handleCmd(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) return;
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        String fullCmd = String.join(" ", args);
        // 以控制台身份执行
        CommandSender console = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(console, fullCmd);
    }

    @AICommand(name = "equip", desc = "装备物品到指定槽位", args = "槽位 物品类型", category = "物品")
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

    @AICommand(name = "drop", desc = "丢弃某槽位物品", args = "槽位", category = "物品")
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

    @AICommand(name = "sit", desc = "坐下", category = "姿态")
    private void handleSit(Player entity) {
        plugin.getNpcAnimator().sit(entity);
    }

    @AICommand(name = "sleep", desc = "躺下睡觉", category = "姿态")
    private void handleSleep(Player entity) {
        plugin.getNpcAnimator().sleep(entity);
    }

    @AICommand(name = "sneak", desc = "潜行", category = "姿态")
    private void handleSneak(Player entity) {
        plugin.getNpcAnimator().sneak(entity);
    }

    @AICommand(name = "stand", desc = "恢复站立（取消坐下/睡觉/潜行）", category = "姿态")
    private void handleStand(Player entity) {
        plugin.getNpcAnimator().stand(entity);
    }

    @AICommand(name = "wave", desc = "挥手打招呼", category = "动作")
    private void handleWave(Player entity) {
        plugin.getNpcAnimator().wave(entity);
    }

    @AICommand(name = "dance", desc = "跳舞（连续挥手+跳跃+旋转）", category = "动作")
    private void handleDance(Player entity) {
        plugin.getNpcAnimator().dance(entity);
    }

    @AICommand(name = "swing", desc = "挥动主手", category = "动作")
    private void handleSwing(Player entity) {
        plugin.getNpcAnimator().swingArm(entity);
    }

    @AICommand(name = "look_at_player", desc = "持续看向某玩家 5 秒（聊天时用）", args = "玩家名", category = "视角")
    private void handleLookAtPlayer(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getLogger().info("look_at_player: 玩家不在线 " + args[0]);
            return;
        }
        plugin.getNpcAnimator().lookAtPlayerTemporarily(entity, target, 100);
    }

    @AICommand(name = "approach", desc = "走到玩家身边并挥手打招呼", args = "玩家名", category = "移动")
    private void handleApproach(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getLogger().info("approach: 玩家不在线 " + args[0]);
            return;
        }
        plugin.getNpcAnimator().approachPlayer(entity, target);
    }

    @AICommand(name = "dismount", desc = "下坐骑", category = "移动")
    private void handleDismount(Player entity) {
        entity.leaveVehicle();
    }

    // ===== 新增 20 个功能命令 =====

    /** kill —— 自杀（测试用） */
    @AICommand(name = "kill", desc = "自杀", category = "自身状态")
    private void handleKill(Player entity) {
        entity.setHealth(0);
    }

    /** heal [数值] —— 恢复血量，默认 20 */
    @AICommand(name = "heal", desc = "恢复血量（默认 20）", args = "数值", category = "自身状态")
    private void handleHeal(Player entity, String[] args) {
        if (entity.isDead()) return;
        double amount = args.length >= 1 ? safeParseDouble(args[0], 20) : 20;
        double maxHealth = readMaxHealth(entity);
        entity.setHealth(Math.min(maxHealth, entity.getHealth() + amount));
    }

    /** feed [数值] —— 恢复饱食度，默认 20 */
    @AICommand(name = "feed", desc = "恢复饱食度（默认 20）", args = "数值", category = "自身状态")
    private void handleFeed(Player entity, String[] args) {
        if (entity.isDead()) return;
        int amount = args.length >= 1 ? safeParse(args[0], 20) : 20;
        entity.setFoodLevel(Math.min(20, entity.getFoodLevel() + amount));
    }

    /** gamemode <survival|creative|adventure|spectator> */
    @AICommand(name = "gamemode", desc = "切换自己的游戏模式", args = "survival|creative|adventure|spectator", category = "自身状态")
    private void handleGamemode(Player entity, String[] args) {
        if (args.length < 1) return;
        try {
            org.bukkit.GameMode gm = org.bukkit.GameMode.valueOf(args[0].toUpperCase());
            entity.setGameMode(gm);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /** fly <true|false> —— 开关飞行 */
    @AICommand(name = "fly", desc = "开关飞行", args = "true|false", category = "自身状态")
    private void handleFly(Player entity, String[] args) {
        try {
            if (!entity.getGameMode().equals(org.bukkit.GameMode.CREATIVE)
                && !entity.getGameMode().equals(org.bukkit.GameMode.SPECTATOR)) {
                entity.setGameMode(org.bukkit.GameMode.CREATIVE);
            }
            entity.setAllowFlight(true);
            entity.setFlying(true);
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("handleFly 失败: " + e.getMessage());
        }
    }

    /** ignite [秒数] —— 着火 */
    @AICommand(name = "ignite", desc = "自燃（默认 5 秒）", args = "秒数", category = "自身状态")
    private void handleIgnite(Player entity, String[] args) {
        int seconds = args.length >= 1 ? safeParse(args[0], 5) : 5;
        entity.setFireTicks(Math.min(seconds, 3600) * 20);
    }

    /** extinguish —— 灭火 */
    @AICommand(name = "extinguish", desc = "灭火", category = "自身状态")
    private void handleExtinguish(Player entity) {
        entity.setFireTicks(0);
    }

    /** strike [玩家名|self] —— 雷击 */
    @AICommand(name = "strike", desc = "雷击目标（默认自己）", args = "玩家名|self", category = "战斗")
    private void handleStrike(Player entity, String[] args) {
        Player target = entity;
        if (args.length >= 1 && !args[0].equalsIgnoreCase("self")) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                throw new RuntimeException("玩家 " + args[0] + " 不在线");
            }
        }
        if (target != null) {
            target.getWorld().strikeLightning(target.getLocation());
        }
    }

    /** explode [威力] —— 在 AI 位置产生爆炸 */
    @AICommand(name = "explode", desc = "在自己位置产生爆炸（默认威力 2）", args = "威力", category = "战斗")
    private void handleExplode(Player entity, String[] args) {
        float power = args.length >= 1 ? (float) safeParseDouble(args[0], 2.0) : 2.0f;
        entity.getWorld().createExplosion(entity.getLocation(), power, false, true);
    }

    /** spawnmob <生物类型> [数量] */
    @AICommand(name = "spawnmob", desc = "召唤生物", args = "类型 数量", category = "物品")
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
    @AICommand(name = "xp", desc = "给自己经验", args = "数值", category = "物品")
    private void handleXp(Player entity, String[] args) {
        int amount = args.length >= 1 ? safeParse(args[0], 10) : 10;
        entity.giveExp(amount);
    }

    /** clearinv —— 清空背包 */
    @AICommand(name = "clearinv", desc = "清空自己的背包", category = "物品")
    private void handleClearInv(Player entity) {
        entity.getInventory().clear();
    }

    /** rename <新名字> —— 重命名 AI */
    @AICommand(name = "rename", desc = "修改自己的显示名称", args = "新名字", category = "其他")
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
    @AICommand(name = "ride", desc = "骑乘目标玩家", args = "玩家名", category = "其他")
    private void handleRide(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && !target.equals(entity)) {
            target.addPassenger(entity);
        }
    }

    /** carry <玩家名> —— 让目标骑乘 AI */
    @AICommand(name = "carry", desc = "让目标玩家骑自己", args = "玩家名", category = "其他")
    private void handleCarry(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && !target.equals(entity)) {
            entity.addPassenger(target);
        }
    }

    /** duplicate —— 复制主手物品 */
    @AICommand(name = "duplicate", desc = "复制主手物品", category = "物品")
    private void handleDuplicate(Player entity) {
        ItemStack hand = entity.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;
        entity.getInventory().addItem(hand.clone());
    }

    /** openinv <玩家名> —— 打开目标玩家背包 */
    @AICommand(name = "openinv", desc = "打开目标玩家的背包", args = "玩家名", category = "物品")
    private void handleOpenInv(Player entity, String[] args) {
        if (args.length < 1) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && !target.equals(entity)) {
            entity.openInventory(target.getInventory());
        }
    }

    /** home —— 传送到出生点 */
    @AICommand(name = "home", desc = "传送到出生点", category = "其他")
    private void handleHome(Player entity) {
        Location bed = entity.getRespawnLocation();
        if (bed != null) {
            entity.teleport(bed);
        } else {
            entity.teleport(entity.getWorld().getSpawnLocation());
        }
    }

    /** top —— 传送到头顶最高处 */
    @AICommand(name = "top", desc = "传送到头顶最高处", category = "其他")
    private void handleTop(Player entity) {
        Location loc = entity.getLocation();
        int y = entity.getWorld().getHighestBlockYAt(loc);
        loc.setY(y + 1);
        entity.teleport(loc);
    }

    /** combo <玩家名> [次数] —— 连续攻击 */
    @AICommand(name = "combo", desc = "连续攻击玩家（默认 3 次）", args = "玩家名 次数", category = "战斗")
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
                if (count >= times || !entity.isValid() || !target.isValid() || !target.isOnline() || target.isDead()) {
                    cancel();
                    return;
                }
                target.damage(damage, entity);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 8L);
    }

    /** emote <表情> —— 表情动作 */
    @AICommand(name = "emote", desc = "表情动作", args = "bow|clap|laugh|cry|angry", category = "其他")
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

    /** strategy <策略名> <玩家名> —— 执行预设策略（fake_friendly/backstab/trap/feint） */
    @AICommand(name = "strategy", desc = "执行预设策略", args = "策略名 玩家名", category = "策略")
    private void handleStrategy(AIPlayer aiPlayer, String[] args) {
        if (args.length < 2) {
            plugin.getLogger().warning("strategy 命令参数不足: 需要策略名和目标玩家名");
            return;
        }
        // args[0] = 策略名, args[1] = 目标玩家名
        String result = plugin.getStrategyEngine().startStrategy(aiPlayer, args[0], args[1]);
        plugin.getLogger().info("策略执行: " + result);
    }

    // ===== 反射规则命令 =====

    /** reflex_add <trigger> <condition> <action...> —— 添加反射规则，返回规则 ID */
    @AICommand(name = "reflex_add", desc = "添加反射规则（触发器 条件 动作）", args = "trigger condition action...", category = "反射")
    private void handleReflexAdd(AIPlayer aiPlayer, String[] args) {
        if (args.length < 3) {
            throw new RuntimeException("用法：reflex_add <trigger> <condition> <action...>");
        }
        String trigger = args[0];
        String condition = args[1];
        StringBuilder actionBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) actionBuilder.append(" ");
            actionBuilder.append(args[i]);
        }
        String action = actionBuilder.toString();
        // 默认冷却 2000ms（AI 可在 action 后追加 " cooldown <ms>" 调整，但本实现简化为固定默认值）
        int cooldownMs = 2000;
        // 检查 action 末尾是否有 " cooldown <ms>" 参数
        if (action.endsWith(" cooldown") || action.contains(" cooldown ")) {
            String[] parts = action.split("\\s+");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("cooldown")) {
                    try {
                        cooldownMs = Integer.parseInt(parts[i + 1]);
                        // 从 action 中移除 " cooldown <ms>" 部分
                        StringBuilder cleaned = new StringBuilder();
                        for (int j = 0; j < parts.length; j++) {
                            if (j == i || (j == i + 1)) continue;
                            if (cleaned.length() > 0) cleaned.append(" ");
                            cleaned.append(parts[j]);
                        }
                        action = cleaned.toString();
                    } catch (NumberFormatException ignored) {
                        // 用默认 2000
                    }
                    break;
                }
            }
        }
        String id = aiPlayer.getReflexManager().addRule(trigger, condition, action, cooldownMs);
        aiPlayer.setLastQueryResult("已添加反射规则 " + id + "：" + trigger + " " + condition + " 时执行 [" + action + "]，冷却 " + cooldownMs + "ms");
    }

    /** reflex_list —— 列出所有反射规则 */
    @AICommand(name = "reflex_list", desc = "列出所有反射规则", args = "", category = "反射")
    private void handleReflexList(AIPlayer aiPlayer) {
        String list = aiPlayer.getReflexManager().listRules();
        aiPlayer.setLastQueryResult(list);
    }

    /** reflex_remove <id> —— 删除指定反射规则 */
    @AICommand(name = "reflex_remove", desc = "删除指定反射规则", args = "id", category = "反射")
    private void handleReflexRemove(AIPlayer aiPlayer, String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("用法：reflex_remove <id>");
        }
        boolean ok = aiPlayer.getReflexManager().removeRule(args[0]);
        if (!ok) {
            throw new RuntimeException("未找到反射规则：" + args[0]);
        }
        aiPlayer.setLastQueryResult("已删除反射规则 " + args[0]);
    }

    /** reflex_clear —— 清空所有反射规则 */
    @AICommand(name = "reflex_clear", desc = "清空所有反射规则", args = "", category = "反射")
    private void handleReflexClear(AIPlayer aiPlayer) {
        aiPlayer.getReflexManager().clearRules();
        aiPlayer.setLastQueryResult("已清空所有反射规则");
    }

    /** reflex_toggle <id> <on|off> —— 启用/禁用反射规则 */
    @AICommand(name = "reflex_toggle", desc = "启用或禁用反射规则", args = "id on|off", category = "反射")
    private void handleReflexToggle(AIPlayer aiPlayer, String[] args) {
        if (args.length < 2) {
            throw new RuntimeException("用法：reflex_toggle <id> <on|off>");
        }
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        boolean ok = aiPlayer.getReflexManager().toggleRule(args[0], enabled);
        if (!ok) {
            throw new RuntimeException("未找到反射规则：" + args[0]);
        }
        aiPlayer.setLastQueryResult("反射规则 " + args[0] + " 已" + (enabled ? "启用" : "禁用"));
    }

    // ===== P3：查询命令（返回 String，结果回流给 AIPlayer 下一轮注入） =====

    /** 查询所有在线玩家（名字/坐标/血量/装备摘要） */
    @AICommand(name = "query_players", desc = "查询所有在线玩家（名字/坐标/血量/装备）", category = "查询")
    private String handleQueryPlayers(Player entity, String[] args) {
        StringBuilder sb = new StringBuilder("在线玩家：\n");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(entity)) continue;  // 跳过自己
            sb.append("- ").append(p.getName())
              .append("(x=").append((int) p.getLocation().getX())
              .append(",y=").append((int) p.getLocation().getY())
              .append(",z=").append((int) p.getLocation().getZ())
              .append(",health=").append((int) p.getHealth())
              .append(",world=").append(p.getWorld().getName())
              .append(")\n");
        }
        return sb.toString();
    }

    /** 查询附近实体与方块（半径由参数指定，默认 10） */
    @AICommand(name = "query_nearby", desc = "查询附近实体与方块", args = "[radius]", category = "查询")
    private String handleQueryNearby(Player entity, String[] args) {
        int radius = 10;
        if (args.length >= 1) {
            try { radius = Math.min(Integer.parseInt(args[0]), 50); } catch (NumberFormatException ignored) {}
        }
        StringBuilder sb = new StringBuilder("附近 ").append(radius).append(" 格内：\n");
        sb.append("实体：\n");
        int count = 0;
        for (Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (count++ >= 20) break;
            sb.append("- ").append(e.getName()).append("(").append(e.getType()).append(")")
              .append(" 距离 ").append((int) LocationUtil.safeDistance(e.getLocation(), entity.getLocation())).append("\n");
        }
        sb.append("方块：\n");
        count = 0;
        for (int dx = -radius; dx <= radius && count < 15; dx += 2) {
            for (int dy = -2; dy <= 2 && count < 15; dy++) {
                for (int dz = -radius; dz <= radius && count < 15; dz += 2) {
                    org.bukkit.block.Block b = entity.getWorld().getBlockAt(
                        entity.getLocation().getBlockX() + dx,
                        entity.getLocation().getBlockY() + dy,
                        entity.getLocation().getBlockZ() + dz);
                    if (!b.getType().isAir()) {
                        sb.append("- ").append(b.getType()).append(" at (")
                          .append(b.getX()).append(",").append(b.getY()).append(",").append(b.getZ()).append(")\n");
                        count++;
                    }
                }
            }
        }
        return sb.toString();
    }

    /** 查询自己背包 */
    @AICommand(name = "query_inventory", desc = "查询自己背包内容", category = "查询")
    private String handleQueryInventory(Player entity, String[] args) {
        StringBuilder sb = new StringBuilder("背包内容：\n");
        org.bukkit.inventory.PlayerInventory inv = entity.getInventory();
        ItemStack[] items = inv.getContents();
        int count = 0;
        for (int i = 0; i < items.length && count < 30; i++) {
            ItemStack item = items[i];
            if (item != null && !item.getType().isAir()) {
                sb.append("- 槽").append(i).append(": ").append(item.getType())
                  .append(" x").append(item.getAmount()).append("\n");
                count++;
            }
        }
        if (count == 0) sb.append("（背包为空）\n");
        return sb.toString();
    }

    /** 查询指定坐标方块 */
    @AICommand(name = "query_block", desc = "查询指定坐标方块类型", args = "<x> <y> <z>", category = "查询")
    private String handleQueryBlock(Player entity, String[] args) {
        if (args.length < 3) return "用法：query_block <x> <y> <z>";
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            org.bukkit.block.Block b = entity.getWorld().getBlockAt(x, y, z);
            return "方块 (" + x + "," + y + "," + z + ") = " + b.getType();
        } catch (NumberFormatException e) {
            return "坐标必须是整数";
        }
    }

    /** 查询指定玩家状态 */
    @AICommand(name = "query_player", desc = "查询指定玩家状态", args = "<玩家名>", category = "查询")
    private String handleQueryPlayer(Player entity, String[] args) {
        if (args.length < 1) return "用法：query_player <玩家名>";
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) return "玩家 " + args[0] + " 不在线";
        StringBuilder sb = new StringBuilder("玩家 ").append(target.getName()).append(" 状态：\n");
        sb.append("- 位置：(").append((int) target.getLocation().getX()).append(",")
          .append((int) target.getLocation().getY()).append(",")
          .append((int) target.getLocation().getZ()).append(") @ ")
          .append(target.getWorld().getName()).append("\n");
        sb.append("- 血量：").append((int) target.getHealth()).append("/20\n");
        sb.append("- 食物：").append(target.getFoodLevel()).append("/20\n");
        sb.append("- 游戏模式：").append(target.getGameMode()).append("\n");
        sb.append("- 装备：");
        org.bukkit.inventory.EntityEquipment eq = target.getEquipment();
        if (eq != null) {
            sb.append("手持=").append(eq.getItemInMainHand().getType())
              .append(" 头盔=").append(eq.getHelmet() != null ? eq.getHelmet().getType() : "无")
              .append(" 胸甲=").append(eq.getChestplate() != null ? eq.getChestplate().getType() : "无");
        }
        return sb.toString();
    }

    // ===== P4：服务器级控制命令（均受 allow-op-commands 控制，以控制台身份执行） =====

    /** op <玩家名> —— 给玩家 OP 权限 */
    @AICommand(name = "op", desc = "给玩家 OP 权限", args = "玩家名", op = true, category = "OP")
    private void handleOp(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 1) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "op " + args[0]);
    }

    /** deop <玩家名> —— 取消玩家 OP 权限 */
    @AICommand(name = "deop", desc = "取消玩家 OP 权限", args = "玩家名", op = true, category = "OP")
    private void handleDeop(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 1) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "deop " + args[0]);
    }

    /** ban <玩家名> [原因] —— 封禁玩家 */
    @AICommand(name = "ban", desc = "封禁玩家", args = "玩家名 [原因]", op = true, category = "OP")
    private void handleBan(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 1) return;
        StringBuilder cmd = new StringBuilder("ban " + args[0]);
        if (args.length > 1) {
            StringBuilder reason = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) reason.append(" ");
                reason.append(args[i]);
            }
            cmd.append(" ").append(reason);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.toString());
    }

    /** kick <玩家名> [原因] —— 踢出玩家 */
    @AICommand(name = "kick", desc = "踢出玩家", args = "玩家名 [原因]", op = true, category = "OP")
    private void handleKick(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 1) return;
        StringBuilder cmd = new StringBuilder("kick " + args[0]);
        if (args.length > 1) {
            StringBuilder reason = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) reason.append(" ");
                reason.append(args[i]);
            }
            cmd.append(" ").append(reason);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.toString());
    }

    /** tp_all —— 把所有在线玩家传送到 AI 位置 */
    @AICommand(name = "tp_all", desc = "把所有玩家传送到 AI 位置", op = true, category = "OP")
    private void handleTpAll(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        Location loc = entity.getLocation();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(entity)) continue;
            online.teleport(loc);
        }
    }

    /** gamemode_player <玩家名> <survival|creative|adventure|spectator> —— 设置指定玩家的游戏模式 */
    @AICommand(name = "gamemode_player", desc = "设置指定玩家的游戏模式", args = "玩家名 模式", op = true, category = "OP")
    private void handleGamemodePlayer(Player entity, String[] args) {
        if (!plugin.getConfigManager().isAllowOpCommands()) {
            throw new RuntimeException("OP 命令已被禁用");
        }
        if (args.length < 2) return;
        String playerName = args[0];
        String mode = args[1].toLowerCase();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "gamemode " + mode + " " + playerName);
    }

    private double safeParseDouble(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }
}
