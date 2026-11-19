package com.cqust.repair.module.order.service;

import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.order.dto.BoardColumnVO;
import com.cqust.repair.module.order.dto.BoardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 工单看板查询（8 列分状态计数 + 各列前 5 条），Redis 缓存 2 分钟。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private static final long CACHE_SECONDS = 120;
    private static final int TOP_LIMIT = 5;

    private final OrderService orderService;
    private final RedisCacheService redisCacheService;

    /**
     * 看板数据。
     *
     * @param buildingIds  管理员负责的楼栋（已校验权限）
     * @param cacheKeyPart 缓存键后缀（单楼栋用 id，多楼栋用逗号拼接）
     */
    public BoardVO board(List<Long> buildingIds, String cacheKeyPart) {
        String cacheKey = RedisKeys.ORDER_BOARD + cacheKeyPart;
        BoardVO cached = redisCacheService.get(cacheKey);
        if (cached != null && cached.getColumns() != null) {
            return cached;
        }
        Map<String, Long> counts = orderService.countGroupByStatus(buildingIds);
        List<BoardColumnVO> columns = new ArrayList<>(OrderStatusEnum.values().length);
        long total = 0L;
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            long count = counts.getOrDefault(status.name(), 0L);
            BoardColumnVO column = new BoardColumnVO();
            column.setStatus(status.name());
            column.setStatusLabel(status.getLabel());
            column.setCount(count);
            column.setOrders(count > 0
                    ? orderService.listTopByStatus(buildingIds, status.name(), TOP_LIMIT)
                    : Collections.emptyList());
            columns.add(column);
            total += count;
        }
        BoardVO board = new BoardVO();
        board.setColumns(columns);
        board.setTotal(total);
        redisCacheService.set(cacheKey, board, CACHE_SECONDS);
        return board;
    }
}
