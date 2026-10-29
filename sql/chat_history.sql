-- ============================================================
-- 多轮对话历史表（MySQL 持久化 ChatMemory + 幂等消费）
-- 按 session_id 隔离，重启不丢失，支持多轮上下文
-- msg_id 唯一索引：MQ 重复投递时靠它幂等去重
-- ============================================================
USE mcmsuser;

-- 已有表执行这句加字段（新建表用下面建表语句）：
-- ALTER TABLE chat_history ADD COLUMN msg_id VARCHAR(64) NULL UNIQUE COMMENT '消息唯一ID，幂等用';

DROP TABLE IF EXISTS chat_history;
CREATE TABLE chat_history (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  VARCHAR(64)  NOT NULL COMMENT '会话ID',
    role        VARCHAR(20)  NOT NULL COMMENT 'user / assistant',
    content     TEXT         NOT NULL COMMENT '消息内容',
    msg_id      VARCHAR(64)  COMMENT '消息唯一ID，MQ幂等去重',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id)
);
CREATE INDEX idx_chat_history_session ON chat_history(session_id, id);


USE mcmsuser;
ALTER TABLE chat_history ADD COLUMN msg_id VARCHAR(64) NULL COMMENT '消息唯一ID';
ALTER TABLE chat_history ADD UNIQUE KEY uk_msg_id (msg_id);

