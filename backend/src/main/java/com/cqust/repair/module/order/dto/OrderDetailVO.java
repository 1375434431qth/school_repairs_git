package com.cqust.repair.module.order.dto;

import com.cqust.repair.infra.track.OrderTrack;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单详情（学生 / 维修工 / 管理员共用，按角色裁剪）。
 *
 * @author school-repair
 */
@Data
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private String status;
    private String statusLabel;
    private Long studentId;
    private String studentName;
    private String contactPhone;
    private Long buildingId;
    private String buildingName;
    private Long roomId;
    private String roomNo;
    private Long faultTypeId;
    private String faultTypeName;
    private String faultCategory;
    private String description;
    private Integer priority;
    private Integer expectHours;
    private LocalDateTime deadlineTime;
    private Long assigneeId;
    private String assigneeName;
    private String dispatchType;
    private Integer timeoutFlag;
    private String timeoutType;
    private Integer isRework;
    private Long sourceOrderId;
    /** 根工单 id（首单=自身） */
    private Long rootOrderId;
    /** 当前维修轮次 */
    private Integer currentRound;
    private Integer evaluateFlag;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime arriveTime;
    private LocalDateTime finishTime;
    private LocalDateTime confirmTime;
    private LocalDateTime closeTime;
    private LocalDateTime cancelTime;

    /** 报修前照片 */
    private List<OrderImageVO> beforeImages;
    /** 完工照片 */
    private List<OrderImageVO> afterImages;
    /** 维修过程 + 配件 */
    private RepairRecordVO repairRecord;
    /** 学生评价 */
    private EvaluationVO evaluation;
    /** 派单/抢单/拒单/释放/调优先级记录 */
    private List<DispatchRecordVO> dispatchRecords;
    /** MongoDB 轨迹时间轴 */
    private List<OrderTrack> tracks;
    /** 维修轮次 */
    private List<RepairRoundVO> rounds;
    /** 每轮验收记录 */
    private List<AcceptanceVO> acceptances;
}
