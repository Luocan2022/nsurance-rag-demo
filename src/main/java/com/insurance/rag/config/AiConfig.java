package com.insurance.rag.config;

import com.insurance.rag.tool.QueryOutletTool;
import com.insurance.rag.tool.QueryPerformanceTool;
import com.insurance.rag.tool.QueryTableTool;
import com.insurance.rag.tool.SalaryCalcTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 基础配置：
 * - ChatModel / EmbeddingModel 由百炼 DashScope 自动装配；
 * - 注册 4 个 Function Calling 工具：薪资计算、业绩查询、人网关系查询、表结构查询(仅总管)；
 * - 权限、敏感词、审计、脱敏由各工具内部调用 ToolGuard 统一执行。
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 SalaryCalcTool salaryCalcTool,
                                 QueryPerformanceTool queryPerformanceTool,
                                 QueryOutletTool queryOutletTool,
                                 QueryTableTool queryTableTool) {
        return builder
                .defaultSystem("你是严谨的银保销管助手。")
                .defaultTools(salaryCalcTool, queryPerformanceTool, queryOutletTool, queryTableTool)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
