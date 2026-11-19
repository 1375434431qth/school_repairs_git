package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单图片展示对象。
 *
 * @author school-repair
 */
@Data
public class OrderImageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileId;
    private String url;
    private String fileName;
    private Integer sort;
    /** 所属维修轮次 */
    private Integer roundNo;
}
