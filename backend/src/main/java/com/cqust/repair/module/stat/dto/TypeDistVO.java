package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报修类型分布。
 *
 * @author school-repair
 */
@Data
public class TypeDistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long total;
    private List<Item> items;

    /**
     * 单个分类占比。
     */
    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private String category;
        private String categoryName;
        private Long count;
        private Double ratio;
    }
}
