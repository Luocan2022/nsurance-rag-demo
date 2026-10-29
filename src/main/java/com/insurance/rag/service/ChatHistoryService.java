package com.insurance.rag.service;

import com.insurance.rag.mq.ChatHistoryProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话历史（MySQL 持久化 + 热缓存 + MQ削峰）。
 * 按开关自动切换：
 *  - cache=redis：热上下文读写走 Redis；否则走本地 Map
 *  - mq=enabled：写历史发 RabbitMQ 异步落库；否则直接写库
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final JdbcTemplate jdbcTemplate;

    /** 本地热缓存（cache=memory 时用） */
    private final ConcurrentHashMap<String, String> contextCache = new ConcurrentHashMap<>();

    /** 以下两个按条件装配，开关关闭时为 null */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private ChatHistoryProducer producer;

    private static final String REDIS_KEY_PREFIX = "chat:ctx:";

    /**
     * 写一条消息。
     * MQ 开关打开 -> 发队列（消费者落库）；否则直接写库兜底。
     */
    public void save(String sessionId, String role, String content) {
        if (producer != null) {
            producer.send(sessionId, role, content);
        } else {
            try {
                jdbcTemplate.update(
                        "insert into chat_history(session_id, role, content) values (?, ?, ?)",
                        sessionId, role, content);
            } catch (Exception e) {
                System.out.println("[CHAT_HISTORY_SAVE_FAIL] " + e.getMessage());
            }
        }
        // 更新缓存：本地缓存移除；Redis 直接追加新对话（key 常驻，10分钟过期）
        contextCache.remove(sessionId);
        if (redisTemplate != null) {
            String old = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + sessionId);
            String line = ("user".equals(role) ? "用户" : "助手") + "：" + content;
            String updated = (old == null || old.isBlank()) ? line : old + "\n" + line;
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + sessionId, updated,
                    Duration.ofMinutes(10));
        }
    }

    /** 取最近 limit 条历史：优先 Redis，其次本地缓存，最后查库并回填 */
    public String recentContext(String sessionId, int limit) {
        if (redisTemplate != null) {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + sessionId);
            if (cached != null) {
                return cached;
            }
        }
        String local = contextCache.get(sessionId);
        if (local != null) {
            return local;
        }
        List<String> rows = jdbcTemplate.query(
                "select role, content from (" +
                "  select id, role, content from chat_history " +
                "  where session_id = ? order by id desc limit ?" +
                ") t order by id asc",
                (rs, rowNum) -> {
                    String role = "user".equals(rs.getString("role")) ? "用户" : "助手";
                    return role + "：" + rs.getString("content");
                },
                sessionId, limit);
        String result = String.join("\n", rows);
        contextCache.put(sessionId, result);
        if (redisTemplate != null) {
            // 热缓存带 10 分钟过期，避免无限堆积
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + sessionId, result,
                    Duration.ofMinutes(10));
        }
        return result;
    }

    /** 清空某个会话 */
    public void clear(String sessionId) {
        jdbcTemplate.update("delete from chat_history where session_id = ?", sessionId);
        contextCache.remove(sessionId);
        if (redisTemplate != null) {
            redisTemplate.delete(REDIS_KEY_PREFIX + sessionId);
        }
    }

    /** 清理 N 天前历史，防表膨胀 */
    public int cleanBeforeDays(int days) {
        return jdbcTemplate.update(
                "delete from chat_history where create_time < (now() - interval ? day)", days);
    }
}
