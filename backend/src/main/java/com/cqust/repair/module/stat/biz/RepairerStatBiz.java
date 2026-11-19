package com.cqust.repair.module.stat.biz;

import com.cqust.repair.module.stat.dto.RepairerStatVO;

import java.time.LocalDate;

/**
 * 维修工个人绩效编排：日期区间校验 + 展示字段填充。
 *
 * @author school-repair
 */
public interface RepairerStatBiz {

    /**
     * 当前维修工绩效，默认本月 1 号至今天。
     */
    RepairerStatVO mine(Long repairerId, LocalDate beginDate, LocalDate endDate);
}
