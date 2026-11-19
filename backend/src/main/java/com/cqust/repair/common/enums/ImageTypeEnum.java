package com.cqust.repair.common.enums;

import lombok.Getter;

/**
 * 图片业务类型。
 *
 * <p>BEFORE / AFTER 写入 t_order_image.img_type；AVATAR 只回写 t_user.avatar；CAROUSEL 只用于首页轮播。</p>
 *
 * @author school-repair
 */
@Getter
public enum ImageTypeEnum {

    BEFORE("报修前"),
    AFTER("维修后"),
    AVATAR("头像"),
    CAROUSEL("首页轮播");

    private final String label;

    ImageTypeEnum(String label) {
        this.label = label;
    }

    /**
     * 按字符串解析，非法值返回 null。
     */
    public static ImageTypeEnum of(String type) {
        if (type == null) {
            return null;
        }
        for (ImageTypeEnum item : values()) {
            if (item.name().equals(type)) {
                return item;
            }
        }
        return null;
    }
}
