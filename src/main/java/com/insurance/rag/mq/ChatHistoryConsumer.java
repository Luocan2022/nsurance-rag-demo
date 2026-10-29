package com.insurance.rag.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 对话历史消费者：
 * 1. 手动 ACK：落库成功才确认，失败 NACK 重回队列，防丢消息；
 * 2. 幂等：靠 msg_id 唯一索引，重复投递不会重复落库。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.history", name = "mq", havingValue = "enabled")
public class ChatHistoryConsumer {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = ChatHistoryMQConfig.QUEUE)
    public void onMessage(String json,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Map<String, String> msg = objectMapper.readValue(json, Map.class);
            // 幂等：msg_id 唯一索引，重复消息插入时被忽略
            jdbcTemplate.update(
                    "insert ignore into chat_history(session_id, role, content, msg_id) values (?, ?, ?, ?)",
                    msg.get("sessionId"), msg.get("role"), msg.get("content"), msg.get("messageId"));
            // 成功，手动确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try {
                // 失败，重回队列（requeue=true）
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                System.out.println("[MQ_ACK_FAIL] " + ex.getMessage());
            }
            System.out.println("[MQ_CONSUME_FAIL] " + e.getMessage());
        }
    }
}
