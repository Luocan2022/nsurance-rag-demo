package com.insurance.rag.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：异步削峰写对话历史。
 * 开关：chat.history.mq=enabled 才生效；默认 disabled，本地没装 RabbitMQ 也能启动。
 */
@Configuration
@ConditionalOnProperty(prefix = "chat.history", name = "mq", havingValue = "enabled")
public class ChatHistoryMQConfig {

    public static final String EXCHANGE = "chat.history.exchange";
    public static final String QUEUE = "chat.history.queue";
    public static final String ROUTING = "chat.history.save";

    @Bean
    public Queue chatHistoryQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public DirectExchange chatHistoryExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding chatHistoryBinding(Queue chatHistoryQueue, DirectExchange chatHistoryExchange) {
        return BindingBuilder.bind(chatHistoryQueue).to(chatHistoryExchange).with(ROUTING);
    }
}
