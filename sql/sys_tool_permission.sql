-- ============================================================
-- 工具功能权限表：把"哪个角色能调用哪个工具"从硬编码改为数据库配置
-- 行权限（按机构过滤）仍是业务逻辑，留在 Java 代码里，不在此配置
-- ============================================================
USE mcmsuser;

DROP TABLE IF EXISTS sys_tool_permission;
CREATE TABLE sys_tool_permission (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    tool_name   VARCHAR(64)  NOT NULL COMMENT '工具方法名',
    role        VARCHAR(32)  NOT NULL COMMENT '角色：ADMIN总管 / ORG_MANAGER机构管理员',
    enabled     TINYINT DEFAULT 1 COMMENT '1=生效 0=停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tool_role(tool_name, role)
);

INSERT INTO sys_tool_permission(tool_name, role) VALUES
('calcNewUserSalary',      'ADMIN'),
('calcNewUserSalary',      'ORG_MANAGER'),
('queryAgentPerformance',  'ADMIN'),
('queryAgentPerformance',  'ORG_MANAGER'),
('queryAgentBindOutlet',   'ADMIN'),
('queryAgentBindOutlet',   'ORG_MANAGER'),
('queryTableStructure',    'ADMIN');   -- 仅总管可查表结构
