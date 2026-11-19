package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 看板数据：8 列（7 个流转状态 + 已取消）。
 *
 * @author school-repair
 */
@Data
public class BoardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<BoardColumnVO> columns;
    private Long total;
}
