package com.insurance.rag.security;

import com.insurance.rag.context.SecurityContext;
import com.insurance.rag.enums.UserRoleEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统一安全守卫（企业标准做法）：
 * 1. 工具功能权限矩阵：存数据库 sys_tool_permission，启动加载到内存，支持热刷新；
 * 2. 敏感词拦截：禁止生成/执行破坏性 SQL（删库、删表等）——属安全策略，保留在代码中；
 * 3. 审计日志：谁、什么角色、问了什么、调了什么工具、结果如何，独立落盘；
 * 4. 数据脱敏：手机号等敏感字段返回前脱敏。
 *
 * 行数据权限（按机构过滤）是业务逻辑，仍在各 Tool 的 SQL 里实现。
 */
@Component
@RequiredArgsConstructor
public class ToolGuard {

    private final JdbcTemplate jdbcTemplate;

    /** 内存权限矩阵：toolName -> 允许的角色集合 */
    private volatile Map<String, Set<String>> permissionCache = new HashMap<>();

    /** 破坏性 SQL / 危险意图敏感词（小写匹配，属安全策略不放库，防误改） */
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "drop table", "drop database", "truncate", "delete from", "delete all",
            "alter table", "create table", "insert into", "update table",
            "exec ", "execute", "删库", "删表", "清空表", "删除所有数据",
            "生成sql", "写sql", "执行sql", "注入"
    );

    /** 启动时从数据库加载权限矩阵到内存 */
    @PostConstruct
    public void init() {
        refresh();
    }

    /** 热刷新权限矩阵（改完 sys_tool_permission 后调用，无需重启） */
    public void refresh() {
        try {
            Map<String, Set<String>> cache = new HashMap<>();
            jdbcTemplate.query(
                    "select tool_name, role from sys_tool_permission where enabled = 1",
                    rs -> {
                        String tool = rs.getString("tool_name");
                        String role = rs.getString("role");
                        cache.computeIfAbsent(tool, k -> new HashSet<>()).add(role);
                    });
            this.permissionCache = cache;
            audit("AUTH", "REFRESH", "权限矩阵加载完成，工具数=" + cache.size());
        } catch (Exception e) {
            // 表不存在时不阻断启动（首次执行 SQL 前兜底）
            audit("AUTH", "REFRESH_FAIL", e.getMessage());
        }
    }

    /**
     * 工具权限校验：无权限直接返回拒绝文案。
     * @return null 表示通过；否则返回拒绝原因
     */
    public String checkToolPermission(String toolName) {
        UserRoleEnum role = SecurityContext.getRole();
        Set<String> allow = permissionCache.get(toolName);
        if (allow == null || !allow.contains(role.name())) {
            String reason = "权限不足：当前角色【" + role + "】不允许调用工具【" + toolName + "】";
            audit(toolName, "REJECT", reason);
            return reason;
        }
        return null;
    }

    /** 敏感词/破坏性 SQL 拦截。@return null 通过；否则拒绝原因 */
    public String checkSensitive(String text) {
        if (text == null) {
            return null;
        }
        String lower = text.toLowerCase();
        for (String w : SENSITIVE_WORDS) {
            if (lower.contains(w)) {
                String reason = "违规请求：检测到破坏性/危险操作意图（" + w + "），已拒绝";
                audit("SENSITIVE", "BLOCK", reason);
                return reason;
            }
        }
        return null;
    }

    /** 数据脱敏：手机号 138****0001 */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 审计日志，独立追加到 logs/audit.log */
    public synchronized void audit(String toolOrEvent, String status, String detail) {
        String line = String.format("[%s] user=%s role=%s orgId=%s event=%s status=%s detail=%s%n",
                LocalDateTime.now(),
                SecurityContext.getUser(),
                SecurityContext.getRole(),
                SecurityContext.getOrgId(),
                toolOrEvent, status,
                detail == null ? "" : detail.replace("\n", " "));
        try {
            java.io.File dir = new java.io.File("logs");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(new java.io.File(dir, "audit.log"), true))) {
                pw.print(line);
            }
        } catch (Exception e) {
            System.out.println("[AUDIT_FAIL] " + line);
        }
    }
}
