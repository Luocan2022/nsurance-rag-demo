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
 * 代理人业绩查询工具。
 * - 权限：总管、机构管理员均可调用；
 * - 行权限：机构管理员自动追加"本网点"过滤，只能看本机构数据；
 * - 脱敏：手机号返回中间四位掩码。
 */
@Component
@RequiredArgsConstructor
public class QueryPerformanceTool {

    private final JdbcTemplate jdbcTemplate;
    private final ToolGuard guard;

    @Tool(description = "根据代理人姓名，查询该代理人的工号、手机号(脱敏)、保单件数、总保费、总佣金。用户问某人业绩、开了多少单、佣金多少时调用")
    public String queryAgentPerformance(
            @ToolParam(description = "代理人姓名，例如张伟、李娜") String agentName
    ) {
        // 1) 工具权限校验
        String denied = guard.checkToolPermission("queryAgentPerformance");
        if (denied != null) {
            return denied;
        }
        // 2) 入参敏感词拦截
        String hit = guard.checkSensitive(agentName);
        if (hit != null) {
            return hit;
        }

        StringBuilder sql = new StringBuilder("""
                SELECT c.channrole_no, c.channrole_name, c.phone,
                       COUNT(p.policy_no) AS policy_count,
                       IFNULL(SUM(p.prem_amount),0) AS total_prem,
                       IFNULL(SUM(p.commission),0) AS total_comm
                FROM tbl_channrole c
                LEFT JOIN tbl_policy p ON c.channrole_id = p.channrole_id
                """);
        List<Object> args = new ArrayList<>();

        // 3) 行权限：机构管理员只能查本网点挂靠的代理人业绩
        if (!SecurityContext.isAdmin()) {
            sql.append(" JOIN tbl_salesnet_channrole sc ON c.channrole_id = sc.channrole_id AND sc.status = 1 ");
            sql.append(" AND sc.salesnet_id = ? ");
            args.add(SecurityContext.getOrgId());
        }
        sql.append(" WHERE c.channrole_name = ? GROUP BY c.channrole_id, c.channrole_no, c.phone");
        args.add(agentName);

        try {
            String result = jdbcTemplate.queryForObject(sql.toString(), (rs, rowNum) -> String.format(
                    "代理人【%s】工号：%s，手机号：%s，保单件数：%d，总保费：%.2f，总佣金：%.2f",
                    rs.getString("channrole_name"),
                    rs.getString("channrole_no"),
                    guard.maskPhone(rs.getString("phone")),
                    rs.getInt("policy_count"),
                    rs.getDouble("total_prem"),
                    rs.getDouble("total_comm")), args.toArray());
            guard.audit("queryAgentPerformance", "OK", agentName);
            return result;
        } catch (Exception e) {
            guard.audit("queryAgentPerformance", "EMPTY", agentName);
            return "没有找到代理人【" + agentName + "】的业绩数据";
        }
    }
}
