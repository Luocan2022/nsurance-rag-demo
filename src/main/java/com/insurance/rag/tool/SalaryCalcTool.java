package com.insurance.rag.tool;

import com.insurance.rag.security.ToolGuard;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 薪资计算工具：查 tbl_commision_account 配置，按出勤率计算新人首月薪资。
 * 权限：总管、机构管理员均可调用。
 */
@Component
@RequiredArgsConstructor
public class SalaryCalcTool {

    private final JdbcTemplate jdbcTemplate;
    private final ToolGuard guard;

    @Data
    public static class CommisionAccountConfig {
        private String configType;
        private Double baseSalary;
        private Double exhibitionFee;
        private Double socialSubsidy;
        private Double postFee;
    }

    @Tool(description = "根据代理制/劳动制，结合入职出勤率与网点出勤率，查询 tbl_commision_account 薪资配置，计算新人入职第一个月薪资。用户问薪资怎么算、薪资多少时调用")
    public String calcNewUserSalary(
            @ToolParam(description = "薪资类型：agent=代理制，labor=劳动制") String configType,
            @ToolParam(description = "入职出勤率，0到1之间的小数，例如0.8表示80%") Double attendRate,
            @ToolParam(description = "挂网点出勤率，0到1之间的小数，例如1.0表示100%") Double siteAttendRate
    ) {
        String denied = guard.checkToolPermission("calcNewUserSalary");
        if (denied != null) {
            return denied;
        }

        CommisionAccountConfig cfg;
        try {
            String sql = "select * from tbl_commision_account where config_type = ?";
            cfg = jdbcTemplate.queryForObject(sql,
                    new BeanPropertyRowMapper<>(CommisionAccountConfig.class), configType);
        } catch (Exception e) {
            e.printStackTrace();
            return "薪资配置查询失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
        if (cfg == null) {
            return "未找到薪资类型【" + configType + "】的配置";
        }
        double result = cfg.getBaseSalary() * attendRate
                + cfg.getExhibitionFee() * siteAttendRate
                + cfg.getSocialSubsidy() * attendRate
                + cfg.getPostFee() * siteAttendRate;
        result = Math.round(result * 100.0) / 100.0;
        guard.audit("calcNewUserSalary", "OK",
                configType + " rate=" + attendRate + "/" + siteAttendRate + " -> " + result);
        return String.format(
                "薪资计算结果：%.2f 元。明细：基础工资%.0f*%.2f + 展业服务费%.0f*%.2f + 社保补贴%.0f*%.2f + 岗位服务费%.0f*%.2f",
                result,
                cfg.getBaseSalary(), attendRate,
                cfg.getExhibitionFee(), siteAttendRate,
                cfg.getSocialSubsidy(), attendRate,
                cfg.getPostFee(), siteAttendRate);
    }
}
