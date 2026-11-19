package com.cqust.repair.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.module.order.dto.EvaluationVO;
import com.cqust.repair.module.order.entity.Evaluation;
import com.cqust.repair.module.order.mapper.EvaluationMapper;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 评价原子服务。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final UserService userService;

    /**
     * 查询工单评价（一单一评，可能为空）。
     */
    public Evaluation getByOrder(Long orderId) {
        return evaluationMapper.selectOne(new QueryWrapper<Evaluation>()
                .eq("order_id", orderId)
                .last("LIMIT 1"));
    }

    /**
     * 新增评价。
     */
    public void save(Evaluation evaluation) {
        evaluationMapper.insert(evaluation);
    }

    /**
     * 评价展示对象；匿名评价不返回学生姓名。
     */
    public EvaluationVO getVO(Long orderId) {
        Evaluation evaluation = getByOrder(orderId);
        if (evaluation == null) {
            return null;
        }
        EvaluationVO vo = new EvaluationVO();
        vo.setId(evaluation.getId());
        vo.setScore(evaluation.getScore());
        vo.setTags(evaluation.getTags());
        vo.setContent(evaluation.getContent());
        vo.setAnonymous(evaluation.getAnonymous());
        vo.setCreateTime(evaluation.getCreateTime());
        if (evaluation.getAnonymous() == null || evaluation.getAnonymous() != 1) {
            SysUser student = userService.findById(evaluation.getStudentId());
            vo.setStudentName(student == null ? null : student.getRealName());
        }
        return vo;
    }
}
