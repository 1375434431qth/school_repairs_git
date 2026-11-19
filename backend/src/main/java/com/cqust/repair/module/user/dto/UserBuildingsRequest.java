package com.cqust.repair.module.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理员楼栋授权请求（覆盖式保存）。
 *
 * @author school-repair
 */
@Data
public class UserBuildingsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 授权楼栋 id 列表，传空数组表示取消全部授权 */
    @NotNull(message = "buildingIds 不能为空")
    private List<Long> buildingIds;
}
