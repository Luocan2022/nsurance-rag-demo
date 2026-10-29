package com.insurance.rag.tool;

import com.insurance.rag.context.SecurityContext;
import com.insurance.rag.security.ToolGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 人网关系查询工具。
 * - 权限：总管、机构管理员均可调用；
 * - 行权限：机构管理员只能看到本网点的挂靠关系。
 */
@Component
@RequiredArgsConstructor
public class QueryOutletTool {

    private final JdbcTemplate jdbcTemplate;
    private final ToolGuard guard;

    @Tool(description = "根据代理人姓名，查询该代理人挂靠的网点名称及挂靠类型（主挂靠/兼挂）。用户问某人挂在哪个网点、属于哪个网点时调用")
    public String queryAgentBindOutlet(
            @ToolParam(description = "代理人姓名，例如张伟、李娜") String agentName
    ) {
        String denied = guard.checkToolPermission("queryAgentBindOutlet");
        if (denied != null) {
            return denied;
        }
        String hit = guard.checkSensitive(agentName);
        if (hit != null) {
            return hit;
        }

        StringBuilder sql = new StringBuilder("""
                SELECT c.channrole_name, s.salesnet_name,
                       IF(sc.main_flag=1,'主挂靠','兼挂') AS bind_type
                FROM tbl_channrole c
                JOIN tbl_salesnet_channrole sc ON c.channrole_id = sc.channrole_id AND sc.status = 1
                JOIN tbl_salesnet s ON sc.salesnet_id = s.salesnet_id
                WHERE c.channrole_name = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(agentName);

        // 机构管理员只能看到本网点
        if (!SecurityContext.isAdmin()) {
            sql.append(" AND sc.salesnet_id = ? ");
            args.add(SecurityContext.getOrgId());
        }

        try {
            String result = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> String.format(
                    "代理人【%s】挂靠网点：%s（%s）",
                    rs.getString("channrole_name"),
                    rs.getString("salesnet_name"),
                    rs.getString("bind_type")), args.toArray())
                    .stream().reduce((a, b) -> a + "；" + b).orElse("未查到挂靠信息");
            guard.audit("queryAgentBindOutlet", "OK", agentName);
            return result;
        } catch (Exception e) {
            return "未查到代理人【" + agentName + "】的网点挂靠信息";
        }
    }
}
