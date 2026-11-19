package com.cqust.repair.infra.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关键操作审计注解：标记的方法会被异步写入 MongoDB operation_log。
 *
 * @author school-repair
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 模块名，如 ORDER / USER */
    String module();

    /** 动作名，如 ASSIGN / CHANGE_PRIORITY */
    String action();
}
