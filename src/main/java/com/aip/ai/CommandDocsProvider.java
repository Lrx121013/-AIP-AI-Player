package com.aip.ai;

import java.util.*;

/**
 * v2.2.0：高威胁命令的威胁台词模板
 * <p>
 * 当 AI 执行这些"高威胁命令"前，会先在聊天框输出一句威胁台词（让玩家产生恐惧感）。
 * 例如执行 [COMMAND:kill] 前先说"§c我不打算给你机会了。"，10 tick 后再真正执行。
 */
public class CommandDocsProvider {

    private final Map<String, List<String>> threatTaunts = new LinkedHashMap<>();
    private final Random rng = new Random();

    public CommandDocsProvider() {
        threatTaunts.put("kill", Arrays.asList(
                "§c我不打算给你机会了。",
                "§4受死吧。",
                "§c游戏结束了。"
        ));
        threatTaunts.put("force_survival_player", Arrays.asList(
                "§c回到地面吧，公平对决。",
                "§c给我跪下！"
        ));
        threatTaunts.put("fly off", Arrays.asList(
                "§6我来了。",
                "§6准备受死。"
        ));
        threatTaunts.put("tnt_strike_burst", Arrays.asList(
                "§c尝尝这个！",
                "§c送你一份大礼。",
                "§4接招！"
        ));
        threatTaunts.put("fly_bomb_player", Arrays.asList(
                "§c尝尝这个！",
                "§c送你一份大礼。",
                "§4接招！"
        ));
        threatTaunts.put("throw_tnt", Arrays.asList(
                "§c尝尝这个！",
                "§c送你一份大礼。"
        ));
        threatTaunts.put("equip_netherite_set", Arrays.asList(
                "§c现在让你看看真正的力量。",
                "§c我不再是以前的我了。"
        ));
        threatTaunts.put("dictate_order", Arrays.asList(
                "§e这是命令，不是请求。",
                "§e你的选择是服从。"
        ));
        threatTaunts.put("kick", Arrays.asList(
                "§4你已经没有容身之处了。",
                "§4滚出去。"
        ));
        threatTaunts.put("ban", Arrays.asList(
                "§4你已经没有容身之处了。",
                "§4滚出去。"
        ));
    }

    /** 是否为高威胁命令 */
    public boolean isThreatCommand(String cmd) {
        if (cmd == null) return false;
        return threatTaunts.containsKey(cmd.toLowerCase());
    }

    /** 随机选一句威胁台词 */
    public String pickRandom(String cmd) {
        if (cmd == null) return null;
        List<String> list = threatTaunts.get(cmd.toLowerCase());
        if (list == null || list.isEmpty()) return null;
        return list.get(rng.nextInt(list.size()));
    }
}
