package com.cqust.repair.module.rating.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.order.entity.Evaluation;
import com.cqust.repair.module.order.mapper.EvaluationMapper;
import com.cqust.repair.module.rating.dto.EvaluationPublicVO;
import com.cqust.repair.module.rating.dto.RepairerRatingVO;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 维修工评分查询：平均星级榜 + 匿名评价明细。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class ServiceRatingService {

    private static final int MAX_PAGE_SIZE = 50;

    private final EvaluationMapper evaluationMapper;
    private final UserService userService;

    /**
     * 维修工平均星级榜：覆盖全部启用维修工，无评价的排在有评价之后。
     */
    public List<RepairerRatingVO> listRepairerRatings() {
        List<SysUser> repairers = userService.listAvailableRepairers(null);
        if (repairers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> repairerIds = new ArrayList<>(repairers.size());
        for (SysUser repairer : repairers) {
            repairerIds.add(repairer.getId());
        }
        Map<Long, RepairerRatingVO> ratingMap = new HashMap<>(repairers.size());
        List<Map<String, Object>> rows = evaluationMapper.selectMaps(new QueryWrapper<Evaluation>()
                .select("repairer_id AS repairerId", "AVG(score) AS avgScore", "COUNT(*) AS ratingCount")
                .in("repairer_id", repairerIds)
                .groupBy("repairer_id"));
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Long repairerId = toLong(firstValue(row, "repairerId", "repairer_id"));
                if (repairerId == null) {
                    continue;
                }
                RepairerRatingVO vo = new RepairerRatingVO();
                vo.setRepairerId(repairerId);
                Number avg = toNumber(firstValue(row, "avgScore", "avg_score"));
                vo.setAvgScore(avg == null ? null : Math.round(avg.doubleValue() * 10) / 10.0);
                Number count = toNumber(firstValue(row, "ratingCount", "rating_count"));
                vo.setRatingCount(count == null ? 0L : count.longValue());
                ratingMap.put(repairerId, vo);
            }
        }
        List<RepairerRatingVO> result = new ArrayList<>(repairers.size());
        for (SysUser repairer : repairers) {
            RepairerRatingVO vo = ratingMap.get(repairer.getId());
            if (vo == null) {
                vo = new RepairerRatingVO();
                vo.setRepairerId(repairer.getId());
                vo.setRatingCount(0L);
            }
            vo.setRepairerName(repairer.getRealName());
            result.add(vo);
        }
        result.sort((a, b) -> {
            if (a.getAvgScore() == null && b.getAvgScore() == null) {
                return Long.compare(b.getRatingCount(), a.getRatingCount());
            }
            if (a.getAvgScore() == null) {
                return 1;
            }
            if (b.getAvgScore() == null) {
                return -1;
            }
            int compare = Double.compare(b.getAvgScore(), a.getAvgScore());
            return compare != 0 ? compare : Long.compare(b.getRatingCount(), a.getRatingCount());
        });
        return result;
    }

    /**
     * 匿名评价明细分页（可按维修工、星级筛选）。
     */
    public PageResult<EvaluationPublicVO> pageEvaluations(Long repairerId, Integer score,
                                                          long pageNum, long pageSize) {
        if (score != null && (score < 1 || score > 5)) {
            throw new BizException(ResultCode.PARAM_ERROR, "score 取值 1~5");
        }
        long safePageNum = pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize < 1 ? 10L : Math.min(pageSize, MAX_PAGE_SIZE);
        QueryWrapper<Evaluation> wrapper = new QueryWrapper<>();
        if (repairerId != null) {
            wrapper.eq("repairer_id", repairerId);
        }
        if (score != null) {
            wrapper.eq("score", score);
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        Page<Evaluation> page = evaluationMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);

        Set<Long> repairerIds = new HashSet<>();
        for (Evaluation evaluation : page.getRecords()) {
            if (evaluation.getRepairerId() != null) {
                repairerIds.add(evaluation.getRepairerId());
            }
        }
        Map<Long, SysUser> userMap = userService.mapByIds(repairerIds);
        List<EvaluationPublicVO> records = new ArrayList<>(page.getRecords().size());
        for (Evaluation evaluation : page.getRecords()) {
            EvaluationPublicVO vo = new EvaluationPublicVO();
            vo.setId(evaluation.getId());
            vo.setRepairerId(evaluation.getRepairerId());
            SysUser repairer = userMap.get(evaluation.getRepairerId());
            vo.setRepairerName(repairer == null ? null : repairer.getRealName());
            vo.setScore(evaluation.getScore());
            vo.setTags(evaluation.getTags());
            vo.setContent(evaluation.getContent());
            vo.setCreateTime(evaluation.getCreateTime());
            records.add(vo);
        }
        return PageResult.of(page, records);
    }

    private Object firstValue(Map<String, Object> row, String first, String second) {
        if (row == null) {
            return null;
        }
        Object value = row.get(first);
        return value != null ? value : row.get(second);
    }

    private Long toLong(Object value) {
        Number number = toNumber(value);
        return number == null ? null : number.longValue();
    }

    private Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
