package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员派单请求。
 *
 * @author school-repair
 */
@Data
public class AssignRequest {

    @NotNull(message = "维修工不能为空")
    private Long assigneeId;

    @Size(max = 200, message = "备注不能超过 200 字")
    private String remark;
}
