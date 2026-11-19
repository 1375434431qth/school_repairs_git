package com.cqust.repair.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.module.order.dto.AcceptanceVO;
import com.cqust.repair.module.order.dto.RepairRoundVO;
import com.cqust.repair.module.order.entity.OrderAcceptance;
import com.cqust.repair.module.order.entity.RepairRound;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.mapper.OrderAcceptanceMapper;
import com.cqust.repair.module.order.mapper.RepairRoundMapper;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 维修轮次与验收记录服务：同一工单内的返修轮次管理。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class OrderRoundService {

    private final RepairRoundMapper repairRoundMapper;
    private final OrderAcceptanceMapper orderAcceptanceMapper;
    private final UserService userService;

    /**
     * 当前轮次号（工单字段兜底为 1）。
     */
    public int currentRoundNo(WorkOrder order) {
        return order.getCurrentRound() == null || order.getCurrentRound() < 1
                ? 1 : order.getCurrentRound();
    }

    /**
     * 当前轮次记录。
     */
    public RepairRound currentRound(Long orderId, int roundNo) {
        return repairRoundMapper.selectOne(new QueryWrapper<RepairRound>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .last("LIMIT 1"));
    }

    /**
     * 新工单创建第 1 轮记录。
     */
    public void createFirstRound(WorkOrder order) {
        RepairRound round = new RepairRound();
        round.setOrderId(order.getId());
        round.setRoundNo(1);
        round.setStatus(OrderStatusEnum.CREATED.name());
        round.setTimeoutFlag(0);
        repairRoundMapper.insert(round);
    }

    /**
     * 学生驳回：登记本轮 REJECT + 生成下一轮（默认原维修工，ASSIGNED）。
     *
     * @return 新轮次号
     */
    public int startRework(WorkOrder order, String reason) {
        int oldRound = currentRoundNo(order);
        saveAcceptance(order.getId(), oldRound, order.getStudentId(), "REJECT", reason);
        int nextRound = oldRound + 1;
        LocalDateTime now = LocalDateTime.now();
        RepairRound round = new RepairRound();
        round.setOrderId(order.getId());
        round.setRoundNo(nextRound);
        round.setRepairerId(order.getAssigneeId());
        round.setStatus(OrderStatusEnum.ASSIGNED.name());
        round.setDispatchType("ASSIGN");
        round.setAssignTime(now);
        round.setReworkReason(reason);
        round.setTimeoutFlag(0);
        repairRoundMapper.insert(round);
        return nextRound;
    }

    /**
     * 验收通过：登记当前轮 PASS。
     */
    public void passCurrentRound(WorkOrder order, Integer roundNo) {
        int round = roundNo == null || roundNo < 1 ? currentRoundNo(order) : roundNo;
        saveAcceptance(order.getId(), round, order.getStudentId(), "PASS", null);
    }

    /**
     * 轮次派单/改派。
     */
    public void markAssigned(Long orderId, int roundNo, Long repairerId, String dispatchType,
                             LocalDateTime assignTime) {
        repairRoundMapper.update(null, new UpdateWrapper<RepairRound>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .set("repairer_id", repairerId)
                .set("dispatch_type", dispatchType)
                .set("assign_time", assignTime)
                .set("status", OrderStatusEnum.ASSIGNED.name()));
    }

    /**
     * 轮次接单。
     */
    public void markAccepted(Long orderId, int roundNo, LocalDateTime acceptTime) {
        repairRoundMapper.update(null, new UpdateWrapper<RepairRound>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .set("accept_time", acceptTime)
                .set("status", OrderStatusEnum.ACCEPTED.name()));
    }

    /**
     * 轮次到场。
     */
    public void markArrived(Long orderId, int roundNo, LocalDateTime arriveTime) {
        repairRoundMapper.update(null, new UpdateWrapper<RepairRound>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .set("arrive_time", arriveTime)
                .set("status", OrderStatusEnum.ARRIVED.name()));
    }

    /**
     * 轮次完工。
     */
    public void markFinished(Long orderId, int roundNo, LocalDateTime finishTime, String finishDesc) {
        repairRoundMapper.update(null, new UpdateWrapper<RepairRound>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .set("finish_time", finishTime)
                .set("finish_desc", finishDesc)
                .set("status", OrderStatusEnum.FINISHED.name()));
    }

    /**
     * 拒单/管理员释放：清空本轮受理信息，回到待派单。
     */
    public void resetRound(Long orderId, int roundNo) {
        repairRoundMapper.update(null, new UpdateWrapper<RepairRound>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .set("repairer_id", null)
                .set("dispatch_type", null)
                .set("assign_time", null)
                .set("accept_time", null)
                .set("arrive_time", null)
                .set("finish_time", null)
                .set("status", OrderStatusEnum.CREATED.name()));
    }

    /**
     * 轮次列表（按轮次升序）。
     */
    public List<RepairRoundVO> listRounds(Long orderId) {
        List<RepairRound> rounds = repairRoundMapper.selectList(new QueryWrapper<RepairRound>()
                .eq("order_id", orderId)
                .orderByAsc("round_no"));
        if (rounds == null || rounds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> repairerIds = new HashSet<>();
        for (RepairRound round : rounds) {
            if (round.getRepairerId() != null) {
                repairerIds.add(round.getRepairerId());
            }
        }
        Map<Long, SysUser> userMap = userService.mapByIds(repairerIds);
        List<RepairRoundVO> result = new ArrayList<>(rounds.size());
        for (RepairRound round : rounds) {
            RepairRoundVO vo = new RepairRoundVO();
            vo.setId(round.getId());
            vo.setRoundNo(round.getRoundNo());
            vo.setRepairerId(round.getRepairerId());
            SysUser repairer = round.getRepairerId() == null ? null : userMap.get(round.getRepairerId());
            vo.setRepairerName(repairer == null ? null : repairer.getRealName());
            vo.setStatus(round.getStatus());
            vo.setStatusLabel(roundStatusLabel(round.getStatus()));
            vo.setDispatchType(round.getDispatchType());
            vo.setAssignTime(round.getAssignTime());
            vo.setAcceptTime(round.getAcceptTime());
            vo.setArriveTime(round.getArriveTime());
            vo.setFinishTime(round.getFinishTime());
            vo.setFinishDesc(round.getFinishDesc());
            vo.setReworkReason(round.getReworkReason());
            vo.setTimeoutFlag(round.getTimeoutFlag());
            result.add(vo);
        }
        return result;
    }

    /**
     * 验收记录列表（按轮次升序）。
     */
    public List<AcceptanceVO> listAcceptances(Long orderId) {
        List<OrderAcceptance> list = orderAcceptanceMapper.selectList(new QueryWrapper<OrderAcceptance>()
                .eq("order_id", orderId)
                .orderByAsc("round_no"));
        List<AcceptanceVO> result = new ArrayList<>(list == null ? 0 : list.size());
        if (list == null) {
            return result;
        }
        for (OrderAcceptance item : list) {
            AcceptanceVO vo = new AcceptanceVO();
            vo.setId(item.getId());
            vo.setRoundNo(item.getRoundNo());
            vo.setResult(item.getResult());
            vo.setResultLabel("PASS".equals(item.getResult()) ? "验收通过" : "判定未完成");
            vo.setReason(item.getReason());
            vo.setCreateTime(item.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 轮次状态文案：FINISHED 表示本轮已完工待验收，避免与工单终态混淆。
     */
    private String roundStatusLabel(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "CREATED" -> "待派单";
            case "ASSIGNED" -> "待接单";
            case "ACCEPTED" -> "待上门";
            case "ARRIVED" -> "维修中";
            case "FINISHED" -> "本轮已完工";
            default -> status;
        };
    }

    private void saveAcceptance(Long orderId, int roundNo, Long studentId, String result, String reason) {        OrderAcceptance acceptance = new OrderAcceptance();
        acceptance.setOrderId(orderId);
        acceptance.setRoundNo(roundNo);
        acceptance.setStudentId(studentId);
        acceptance.setResult(result);
        acceptance.setReason(reason);
        try {
            orderAcceptanceMapper.insert(acceptance);
        } catch (DuplicateKeyException ignored) {
            // 同一轮重复提交时保持幂等
        }
    }
}
