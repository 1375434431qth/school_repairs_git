package com.cqust.repair.infra.track;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单流转轨迹文档（时间轴数据源，存放在 MongoDB order_track 集合）。
 *
 * @author school-repair
 */
@Data
@Document(collection = "order_track")
@CompoundIndex(name = "idx_order_ts", def = "{'orderId': 1, 'ts': 1}")
public class OrderTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private Long orderId;
    private String orderNo;
    /** 所属维修轮次 */
    private Integer roundNo;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorRole;
    private String operatorName;
    private String action;
    private String remark;
    private List<String> images;
    private String ip;
    private LocalDateTime ts;
}
