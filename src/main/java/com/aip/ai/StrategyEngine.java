package com.aip.ai;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * AI 策略与欺骗引擎
 * 提供预设策略模板，LLM 可通过 [COMMAND:strategy <名称> <参数>] 调用
 */
public class StrategyEngine {
    private final AIPlayerPlugin plugin;

    public StrategyEngine(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动策略
     * @param ai 执行策略的 AI
     * @param strategyName 策略名：fake_friendly/backstab/trap/feint
     * @param targetName 目标玩家名
     */
    public String startStrategy(AIPlayer ai, String strategyName, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return "目标玩家 " + targetName + " 不在线";

        return switch (strategyName.toLowerCase()) {
            case "fake_friendly" -> startFakeFriendly(ai, target);
            case "backstab" -> startBackstab(ai, target);
            case "trap" -> startTrap(ai, target);
            case "feint" -> startFeint(ai, target);
            default -> "未知策略：" + strategyName + "（可用：fake_friendly/backstab/trap/feint）";
        };
    }

    /** 假装友好：走到玩家身边，表现友好，等待背刺时机 */
    private String startFakeFriendly(AIPlayer ai, Player target) {
        plugin.getCommandExecutor().execute(ai, "[COMMAND:approach " + target.getName() + "]");
        ai.getMemory().addRecord(MemoryRecord.Type.DECEIVE, "开始对 " + target.getName() + " 假装友好", target.getName());
        return "已启动【假装友好】策略：正在接近 " + target.getName() + "。请在对话中表现友好，等对方放松警惕后执行 [COMMAND:strategy backstab " + target.getName() + "]";
    }

    /** 背刺：立即攻击目标 */
    private String startBackstab(AIPlayer ai, Player target) {
        plugin.getCommandExecutor().execute(ai, "[COMMAND:attack " + target.getName() + "]");
        ai.getMemory().addRecord(MemoryRecord.Type.DECEIVE, "背刺了 " + target.getName(), target.getName());
        return "已启动【背刺】策略：正在攻击 " + target.getName();
    }

    /** 陷阱：在目标附近放置 TNT 并引爆 */
    private String startTrap(AIPlayer ai, Player target) {
        // 走到目标附近
        plugin.getCommandExecutor().execute(ai, "[COMMAND:approach " + target.getName() + "]");
        ai.getMemory().addRecord(MemoryRecord.Type.DECEIVE, "在 " + target.getName() + " 附近设陷阱", target.getName());
        return "已启动【陷阱】策略：正在接近 " + target.getName() + "。到达后用 [COMMAND:place 脚下 tnt] 和 [COMMAND:ignite 1] 引爆";
    }

    /** 声东击西：AI 向反方向移动，吸引注意力 */
    private String startFeint(AIPlayer ai, Player target) {
        plugin.getCommandExecutor().execute(ai, "[COMMAND:walk_dir north 10]");
        ai.getMemory().addRecord(MemoryRecord.Type.DECEIVE, "对 " + target.getName() + " 声东击西", target.getName());
        return "已启动【声东击西】策略：向北移动吸引注意力。队友可从侧翼包抄 " + target.getName();
    }
}
