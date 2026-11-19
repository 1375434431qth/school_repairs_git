package com.cqust.repair.module.stat.biz.impl;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.stat.biz.RepairerStatBiz;
import com.cqust.repair.module.stat.dto.RepairerStatVO;
import com.cqust.repair.module.stat.service.RepairerStatService;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 维修工个人绩效编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RepairerStatBizImpl implements RepairerStatBiz {

    private static final long MAX_RANGE_DAYS = 365;

    private final RepairerStatService repairerStatService;
    private final UserService userService;

    @Override
    public RepairerStatVO mine(Long repairerId, LocalDate beginDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate begin = beginDate == null ? end.withDayOfMonth(1) : beginDate;
        if (begin.isAfter(end)) {
            throw new BizException(ResultCode.ILLEGAL_DATE_RANGE, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(begin, end) > MAX_RANGE_DAYS) {
            throw new BizException(ResultCode.STAT_RANGE_TOO_LARGE);
        }
        RepairerStatVO vo = repairerStatService.stat(repairerId, begin, end);
        SysUser repairer = userService.getRequired(repairerId);
        vo.setRepairerName(repairer.getRealName());
        return vo;
    }
}
