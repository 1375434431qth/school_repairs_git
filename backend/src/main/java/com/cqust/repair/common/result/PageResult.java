package com.cqust.repair.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应。
 *
 * @param <T> 记录类型
 * @author school-repair
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private Long total;
    /** 当前页码 */
    private Long pageNum;
    /** 每页条数 */
    private Long pageSize;
    /** 总页数 */
    private Long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long pageNum, Long pageSize) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total == null ? 0L : total;
        this.pageNum = pageNum == null ? 1L : pageNum;
        this.pageSize = pageSize == null ? 10L : pageSize;
        this.pages = this.pageSize == 0 ? 0L : (this.total + this.pageSize - 1) / this.pageSize;
    }

    /**
     * 由 MyBatis-Plus 分页对象转换，已转换的记录列表由调用方传入。
     *
     * @param page    分页对象（用于取 total/pageNum/pageSize）
     * @param records 已转换的记录
     * @return 分页响应
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 空分页结果。
     */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
    }
}
