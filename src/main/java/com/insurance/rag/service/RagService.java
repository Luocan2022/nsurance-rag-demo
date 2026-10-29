package com.insurance.rag.service;

import com.insurance.rag.util.TikaDocUtil;
import com.insurance.rag.security.ToolGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 核心服务：文档解析 -> 分片 -> 向量化入库 -> 相似度检索 -> 带防幻觉约束的问答。
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final TikaDocUtil tikaDocUtil;
    private final ToolGuard guard;
    private final ChatHistoryService chatHistory;

    @Value("${rag.chunk-size:700}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${rag.top-k:3}")
    private int topK;

    @Value("${rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    /**
     * 上传销管规则文档（基本法 / 佣金方案 / 考核文件），解析分片后向量化入库。
     */
    public String uploadDoc(MultipartFile file) throws Exception {
        String raw = tikaDocUtil.parseText(file);
        List<Document> chunks = split(raw, file.getOriginalFilename());
        vectorStore.add(chunks);
        return "文档《" + file.getOriginalFilename() + "》入库成功，分片数量：" + chunks.size();
    }

    /**
     * RAG + Function Calling 问答：
     * 1) 先向量检索规则文档片段；
     * 2) 无论是否检索到，都交给大模型，并由它自主决定是否调用查库/计算工具。
     */
    public String chat(String question, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }
        // 入口敏感词拦截：禁止生成/执行破坏性 SQL、删库等危险意图
        String hit = guard.checkSensitive(question);
        if (hit != null) {
            return hit;
        }
        guard.audit("CHAT", "ASK", question);

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> hits = vectorStore.similaritySearch(request);

        StringBuilder context = new StringBuilder();
        if (hits != null && !hits.isEmpty()) {
            int idx = 1;
            for (Document d : hits) {
                context.append("【检索片段").append(idx++).append("】\n")
                        .append(d.getText())
                        .append("\n\n");
            }
        }

        String systemPrompt = """
                你是银保销管智能助手，回答规则如下：
                1. 关于佣金、基本法、考核、晋升、扣罚等规则类问题，优先依据下方【参考规则原文】回答；原文没有就明说“规则文档无此条款”，禁止编造数字。
                2. 用户要核算新人薪资时，调用 calcNewUserSalary 工具，从数据库读配置并按出勤率计算，不要自己口算。
                3. 用户问某人的业绩、保单、佣金时，调用 queryAgentPerformance 工具查库后回答。
                4. 用户问某人挂靠哪个网点时，调用 queryAgentBindOutlet 工具查库后回答。
                5. 拿不准的信息不要猜，可先说明并调用工具核实。
                6. 【权限约束】若工具返回“权限不足”，如实转告用户，不要绕过、不要编造无权限的数据；机构管理员只能看到本机构数据。
                7. 严禁生成、改写、执行任何 SQL 语句，严禁输出表结构之外的库信息。

                ================== 参考规则原文（可能为空） ==================
                %s
                =================================================
                """.formatted(context);

        // 多轮上下文：取最近 10 条历史，拼到当前问题前
        String history = chatHistory.recentContext(sessionId, 10);
        String userInput = (history.isBlank() ? "" : "【历史对话】\n" + history + "\n\n")
                + "【当前问题】\n" + question;

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();

        // 持久化本轮一问一答，供下一轮上下文使用
        chatHistory.save(sessionId, "user", question);
        chatHistory.save(sessionId, "assistant", answer);
        return answer;
    }

    /**
     * 简单字符窗口分片：固定长度 + 重叠，避免规则条款被从中间截断。
     */
    private List<Document> split(String raw, String source) {
        String text = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        List<Document> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        int n = text.length();
        while (start < n) {
            int end = Math.min(start + chunkSize, n);
            String piece = text.substring(start, end);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", source);
            metadata.put("chunkIndex", index++);
            chunks.add(new Document(piece, metadata));
            if (end >= n) {
                break;
            }
            start = end - chunkOverlap;
        }
        return chunks;
    }
}
