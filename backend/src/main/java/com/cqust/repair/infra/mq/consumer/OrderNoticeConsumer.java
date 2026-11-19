package com.cqust.repair.infra.mq.consumer;

import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.infra.mq.RabbitMQConfig;
import com.cqust.repair.infra.mq.message.OrderNoticeMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 通知消息消费者（默认关闭）：按 messageId 幂等，手动 ACK。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "repair.mq", name = "enabled", havingValue = "true")
public class OrderNoticeConsumer {

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTICE_QUEUE)
    public void onMessage(OrderNoticeMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            String idempotentKey = RedisKeys.MQ_NOTICE_IDEM + message.getMessageId();
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 10, TimeUnit.MINUTES);
            if (!Boolean.TRUE.equals(first)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 预留：对接短信/邮件/大屏等外部通道
            log.info("收到通知消息: orderId={}, type={}, userId={}",
                    message.getOrderId(), message.getType(), message.getUserId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("通知消息消费失败: orderId={}", message.getOrderId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
