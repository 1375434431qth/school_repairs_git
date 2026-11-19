package com.cqust.repair.module.basedata.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 房间展示对象。
 *
 * @author school-repair
 */
@Data
public class RoomVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long buildingId;
    private String roomNo;
    private Integer floor;
}
