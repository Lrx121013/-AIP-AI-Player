package com.aip.ai;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 命令注解：标记 CommandExecutor 中的命令处理方法
 * 用于自动生成命令文档注入 LLM system prompt
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AICommand {
    /** 命令名称 */
    String name();
    /** 命令描述 */
    String desc();
    /** 参数说明 */
    String args() default "";
    /** 是否需要 OP 权限（allow-op-commands=true 才可用） */
    boolean op() default false;
    /** 命令分类 */
    String category() default "其他";
}
