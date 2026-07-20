package com.aip.ai;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 主线任务工厂类。
 * <p>
 * 根据 {@link Personality} 与对应 {@link AIPlayer} 生成主线性任务模板。
 * 每种人格对应一段剧情化的多阶段任务，包含描述、完成条件、动作列表和目标进度。
 * </p>
 * <p>
 * 若 {@code p == null} 或人格未匹配任何 case，返回 {@code null}（该 AI 不绑定主线任务）。
 * </p>
 */
public class MainQuestFactory {

    /**
     * 根据人格创建主线任务模板。
     *
     * @param p  AI 人格枚举
     * @param ai 所属 AI 玩家
     * @return 与人格对应的主线任务；若 {@code p == null} 或未匹配到 case 则返回 {@code null}
     */
    /**
     * 为指定人格创建主线任务
     * <p>
     * v2.1.3 故事模式起，邪恶类 AI（VILLAIN / CONQUEROR / MANIPULATOR / STRATEGIST）改走 StoryManager 流程，
     * 本工厂对这些人格返回 null，避免与故事模式冲突。
     * 普通 AI（BRAVE / TIMID / GRUMPY / GENTLE）的 MainQuest 保留不变。
     */
    public static MainQuest create(Personality personality, AIPlayer ai) {
        if (personality == null) return null;
        // v2.1.3：邪恶 AI 改走 StoryManager
        switch (personality) {
            case VILLAIN:
            case CONQUEROR:
            case MANIPULATOR:
            case STRATEGIST:
                return null;
            default:
                // 普通 AI 走原 MainQuest 流程
                return createDefault(personality, ai);
        }
    }

    private static MainQuest createDefault(Personality p, AIPlayer ai) {
        if (p == null) {
            return null;
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        UUID ownerId = ai.getEntityId();

        switch (p) {
            case VILLAIN: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "伪装接近玩家",
                                MainQuest.CompletionCondition.REACH_PLAYER,
                                Arrays.asList("approach <nearest_player>", "say 嘿"),
                                1),
                        new MainQuest.QuestStage(
                                "获取信任",
                                MainQuest.CompletionCondition.APPROACH_COUNT,
                                Arrays.asList("say 我可以帮你", "follow <nearest_player>"),
                                3),
                        new MainQuest.QuestStage(
                                "背叛时机",
                                MainQuest.CompletionCondition.KILL_TARGET,
                                Arrays.asList("attack <nearest_player>"),
                                1)
                );
                return new MainQuest(id, "潜入渗透", ownerId, stages);
            }
            case CONQUEROR: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "建立据点",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir north 5", "say 这里归我了"),
                                12),
                        new MainQuest.QuestStage(
                                "扩张领土",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("approach <nearest_player>", "say 这是我的地盘"),
                                18),
                        new MainQuest.QuestStage(
                                "攻击玩家",
                                MainQuest.CompletionCondition.KILL_TARGET,
                                Arrays.asList("attack <nearest_player>"),
                                1),
                        new MainQuest.QuestStage(
                                "统治服务器",
                                MainQuest.CompletionCondition.NONE,
                                Arrays.asList("say 我已统治一切"),
                                1)
                );
                return new MainQuest(id, "征服领土", ownerId, stages);
            }
            case MANIPULATOR: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "假装友好",
                                MainQuest.CompletionCondition.APPROACH_COUNT,
                                Arrays.asList("approach <nearest_player>", "say 嗨"),
                                2),
                        new MainQuest.QuestStage(
                                "挑拨离间",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("say 听说有人在背后说你坏话"),
                                10),
                        new MainQuest.QuestStage(
                                "收服玩家",
                                MainQuest.CompletionCondition.NONE,
                                Arrays.asList("say 跟着我"),
                                1)
                );
                return new MainQuest(id, "欺骗操控", ownerId, stages);
            }
            case STRATEGIST: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "侦察地形",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir north 8"),
                                8),
                        new MainQuest.QuestStage(
                                "建立联盟",
                                MainQuest.CompletionCondition.APPROACH_COUNT,
                                Arrays.asList("approach <random_player>", "say 我们结盟吧"),
                                2),
                        new MainQuest.QuestStage(
                                "布局陷阱",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir south 10", "say 计划进行中"),
                                10),
                        new MainQuest.QuestStage(
                                "总攻",
                                MainQuest.CompletionCondition.KILL_TARGET,
                                Arrays.asList("attack <nearest_player>"),
                                1)
                );
                return new MainQuest(id, "战略布局", ownerId, stages);
            }
            case BRAVE: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "探索地形",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir north 6"),
                                6),
                        new MainQuest.QuestStage(
                                "寻找资源",
                                MainQuest.CompletionCondition.NONE,
                                Arrays.asList("say 这里有什么资源"),
                                1)
                );
                return new MainQuest(id, "自由探索", ownerId, stages);
            }
            case GRUMPY: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "探索地形",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir east 6"),
                                6),
                        new MainQuest.QuestStage(
                                "寻找资源",
                                MainQuest.CompletionCondition.NONE,
                                Arrays.asList("say 这里有什么资源"),
                                1)
                );
                return new MainQuest(id, "自由探索", ownerId, stages);
            }
            case TIMID: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "收集物资",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir south 5"),
                                6),
                        new MainQuest.QuestStage(
                                "建立避难所",
                                MainQuest.CompletionCondition.NONE,
                                Arrays.asList("say 这里看起来安全"),
                                1)
                );
                return new MainQuest(id, "安全求生", ownerId, stages);
            }
            case GENTLE: {
                List<MainQuest.QuestStage> stages = Arrays.asList(
                        new MainQuest.QuestStage(
                                "探索地形",
                                MainQuest.CompletionCondition.ELAPSE_TIME,
                                Arrays.asList("walk_dir west 6"),
                                6),
                        new MainQuest.QuestStage(
                                "帮助玩家",
                                MainQuest.CompletionCondition.NONE,
                                Arrays.asList("say 需要帮忙吗"),
                                1)
                );
                return new MainQuest(id, "自由探索", ownerId, stages);
            }
            default:
                return null;
        }
    }
}
