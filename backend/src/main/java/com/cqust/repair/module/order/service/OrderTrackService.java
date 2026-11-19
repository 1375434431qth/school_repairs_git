package com.cqust.repair.module.order.service;

import com.cqust.repair.common.util.CurrentUser;
import com.cqust.repair.common.util.IpUtils;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.infra.track.OrderTrack;
import com.cqust.repair.infra.track.OrderTrackRepository;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

/**
 * 工单轨迹服务（MongoDB order_track）：失败降级为日志，不影响主流程。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTrackService {

    private final OrderTrackRepository orderTrackRepository;
    private final UserService userService;

    /**
     * 记录一条状态流转轨迹。
     */
    public void record(Long orderId, String orderNo, String fromStatus, String toStatus,
                       String action, String remark) {
        record(orderId, orderNo, fromStatus, toStatus, action, remark, null);
    }

    /**
     * 记录一条状态流转轨迹（带维修轮次）。
     */
    public void record(Long orderId, String orderNo, String fromStatus, String toStatus,
                       String action, String remark, Integer roundNo) {
        try {
            OrderTrack track = new OrderTrack();
            track.setOrderId(orderId);
            track.setOrderNo(orderNo);
            track.setRoundNo(roundNo);
            track.setFromStatus(fromStatus);
            track.setToStatus(toStatus);
            track.setAction(action);
            track.setRemark(remark);
            fillOperator(track);
            orderTrackRepository.save(track);
        } catch (Exception e) {
            log.warn("工单轨迹写入失败: orderId={}, action={}, error={}", orderId, action, e.getMessage());
        }
    }

    /**
     * 查询工单轨迹时间轴。
     */
    public List<OrderTrack> list(Long orderId) {
        try {
            return orderTrackRepository.findByOrderId(orderId);
        } catch (Exception e) {
            log.warn("工单轨迹查询失败: orderId={}, error={}", orderId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void fillOperator(OrderTrack track) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        track.setOperatorId(currentUser.userId());
        track.setOperatorRole(currentUser.role());
        if (currentUser.userId() != null) {
            SysUser user = userService.findById(currentUser.userId());
            track.setOperatorName(user == null ? currentUser.username() : user.getRealName());
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            track.setIp(IpUtils.getClientIp(attributes.getRequest()));
        }
    }
}
