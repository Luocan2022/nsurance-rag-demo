package com.insurance.rag.tool;

import com.insurance.rag.security.ToolGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 表结构查询工具（仅总管 ADMIN 可用）。
 * 机构管理员调用会被权限矩阵直接拒绝。
 */
@Component
@RequiredArgsConstructor
public class QueryTableTool {

    private final JdbcTemplate jdbcTemplate;
    private final ToolGuard guard;

    @Tool(description = "查询指定数据库表的字段结构（字段名、类型、注释）。仅系统总管可调用，普通机构人员禁止使用")
    public String queryTableStructure(
            @ToolParam(description = "表名，例如 tbl_policy、tbl_channrole") String tableName
    ) {
        String denied = guard.checkToolPermission("queryTableStructure");
        if (denied != null) {
            return denied;
        }
        String hit = guard.checkSensitive(tableName);
        if (hit != null) {
            return hit;
        }
        // 防止传入非表名造成注入：只允许字母、数字、下划线
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return "非法表名：" + tableName;
        }
        try {
            String sql = "select column_name, column_type, column_comment " +
                    "from information_schema.columns where table_schema = database() " +
                    "and table_name = ? order by ordinal_position";
            return jdbcTemplate.query(sql, (rs, rowNum) -> String.format("%s (%s) %s",
                    rs.getString("column_name"),
                    rs.getString("column_type"),
                    rs.getString("column_comment")), tableName)
                    .stream().reduce((a, b) -> a + "；" + b)
                    .orElse("表【" + tableName + "】不存在或无字段");
        } catch (Exception e) {
            return "查询表结构失败：" + e.getMessage();
        }
    }
}
