package com.cqust.repair.infra.mq.producer;

import com.cqust.repair.infra.mq.RabbitMQConfig;
import com.cqust.repair.infra.mq.message.OrderNoticeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 消息生产者（默认关闭）。发送失败只记 error，不阻塞工单主流程。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "repair.mq", name = "enabled", havingValue = "true")
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送通知消息（持久化投递）。
     */
    public void sendNotice(OrderNoticeMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTICE_EXCHANGE,
                    RabbitMQConfig.NOTICE_ROUTING_KEY, message,
                    msg -> {
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return msg;
                    });
        } catch (Exception e) {
            log.error("通知消息发送失败: orderId={}, type={}", message.getOrderId(), message.getType(), e);
        }
    }
}
