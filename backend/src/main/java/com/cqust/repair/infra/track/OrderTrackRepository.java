package com.cqust.repair.infra.track;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单轨迹仓储（MongoDB）。
 *
 * @author school-repair
 */
@Repository
@RequiredArgsConstructor
public class OrderTrackRepository {

    private final MongoTemplate mongoTemplate;

    /**
     * 写入一条轨迹。
     */
    public void save(OrderTrack track) {
        if (track.getTs() == null) {
            track.setTs(LocalDateTime.now());
        }
        mongoTemplate.insert(track);
    }

    /**
     * 按工单 id 查询轨迹，按时间升序。
     */
    public List<OrderTrack> findByOrderId(Long orderId) {
        Query query = new Query(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Direction.ASC, "ts"));
        return mongoTemplate.find(query, OrderTrack.class);
    }
}
