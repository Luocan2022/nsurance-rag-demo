package com.insurance.rag.controller;

import com.insurance.rag.context.SecurityContext;
import com.insurance.rag.enums.UserRoleEnum;
import com.insurance.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 接口：
 * POST /rag/upload  上传规则文档（multipart 字段名 file）
 * GET  /rag/chat?q=问题  RAG + Function Calling 问答
 * GET  /rag/testDb  直连数据库测试（定位“计算失败”用）
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final com.insurance.rag.service.ChatHistoryService chatHistoryService;
    private final JdbcTemplate jdbcTemplate;
    private final com.insurance.rag.security.ToolGuard guard;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {
        return ragService.uploadDoc(file);
    }

    /**
     * 问答接口。demo 用请求参数模拟登录身份（生产环境从 JWT 解析）：
     * role=ADMIN 总管（全量数据）
     * role=ORG_MANAGER 机构管理员 + orgId=本网点id（只能看本机构）
     */
    @GetMapping("/chat")
    public String chat(@RequestParam("q") String question,
                       @RequestParam(defaultValue = "default") String sessionId,
                       @RequestParam(defaultValue = "ORG_MANAGER") String role,
                       @RequestParam(required = false) Long orgId,
                       @RequestParam(defaultValue = "tester") String user) {
        try {
            SecurityContext.set(UserRoleEnum.valueOf(role.toUpperCase()), orgId, user);
            return ragService.chat(question, sessionId);
        } finally {
            SecurityContext.clear();
        }
    }

    /**
     * 直连数据库测试：查 tbl_commision_account 全部配置。
     * 浏览器访问 http://localhost:8080/rag/testDb
     * - 返回两行数据 = 数据库连接和表都正常，问题出在大模型传参；
     * - 返回异常 = 直接看到真实报错（密码错/表不存在/连不上）。
     */
    @GetMapping("/testDb")
    public String testDb() {
        try {
            List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList("select * from tbl_commision_account");
            List<String> lines = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                lines.add(row.toString());
            }
            return "数据库连接正常，tbl_commision_account 共 " + rows.size() + " 行：\n" +
                    String.join("\n", lines);
        } catch (Exception e) {
            return "数据库测试失败：" + e.getClass().getName() + " : " + e.getMessage();
        }
    }

    /**
     * 热刷新权限矩阵：改完 sys_tool_permission 表后调用，无需重启。
     * 浏览器访问 http://localhost:8080/rag/refreshAuth
     */
    @GetMapping("/refreshAuth")
    public String refreshAuth() {
        guard.refresh();
        return "权限矩阵已刷新";
    }

    /**
     * 清理 N 天前的对话历史，防表膨胀。
     * http://localhost:8080/rag/cleanHistory?days=30
     */
    @GetMapping("/cleanHistory")
    public String cleanHistory(@RequestParam(defaultValue = "30") int days) {
        int n = chatHistoryService.cleanBeforeDays(days);
        return "已清理 " + days + " 天前的历史，删除 " + n + " 条";
    }
}