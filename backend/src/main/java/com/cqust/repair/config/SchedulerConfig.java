package com.cqust.repair.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关（超时扫描、自动确认/关闭、统计快照）。
 *
 * @author school-repair
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
