package com.cqust.repair.infra.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置（可选，默认关闭）。开启后用于通知异步解耦。
 *
 * @author school-repair
 */
@Configuration
@ConditionalOnProperty(prefix = "repair.mq", name = "enabled", havingValue = "true")
public class RabbitMQConfig {

    /** 通知交换机 */
    public static final String NOTICE_EXCHANGE = "notice.exchange";
    /** 通知队列 */
    public static final String NOTICE_QUEUE = "notice.push.queue";
    /** 通知路由键 */
    public static final String NOTICE_ROUTING_KEY = "notice.push";

    @Bean
    public DirectExchange noticeExchange() {
        return new DirectExchange(NOTICE_EXCHANGE, true, false);
    }

    @Bean
    public Queue noticeQueue() {
        return new Queue(NOTICE_QUEUE, true);
    }

    @Bean
    public Binding noticeBinding(Queue noticeQueue, DirectExchange noticeExchange) {
        return BindingBuilder.bind(noticeQueue).to(noticeExchange).with(NOTICE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
