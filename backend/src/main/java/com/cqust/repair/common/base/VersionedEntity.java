package com.cqust.repair.common.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带乐观锁的实体基类，仅用于并发写的表（工单、维修记录）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VersionedEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
