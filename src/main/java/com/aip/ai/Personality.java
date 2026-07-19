package com.aip.ai;

/**
 * AI 个性枚举
 * <p>
 * 不同个性会影响 system prompt，从而影响 AI 的对话与决策风格。
 */
public enum Personality {

    BRAVE("你性格勇敢，面对危险不退缩，主动迎战"),
    TIMID("你性格胆小，遇到危险会逃跑， prefer avoiding combat"),
    GRUMPY("你性格暴躁，容易生气，说话直接"),
    GENTLE("你性格温和，友善，乐于助人");

    private final String prompt;

    Personality(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
