package com.insurance.rag.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 对话历史生产者：发 JSON 字符串，每条消息带全局唯一 messageId（幂等用）。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.history", name = "mq", havingValue = "enabled")
public class ChatHistoryProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(String sessionId, String role, String content) {
        try {
            Map<String, String> msg = Map.of(
                    "messageId", UUID.randomUUID().toString(),
                    "sessionId", sessionId,
                    "role", role,
                    "content", content);
            String json = objectMapper.writeValueAsString(msg);
            rabbitTemplate.convertAndSend(
                    ChatHistoryMQConfig.EXCHANGE,
                    ChatHistoryMQConfig.ROUTING,
                    json);
        } catch (Exception e) {
            System.out.println("[MQ_SEND_FAIL] " + e.getMessage());
        }
    }
}
