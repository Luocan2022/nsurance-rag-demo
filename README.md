# 银保销管规则智能问答 RAG 平台（Spring AI + 百炼 DashScope）

面向银保代理人/内勤的规则问答助手：上传基本法、佣金方案、考核文件，自然语言查询佣金、晋升、扣罚规则，大模型只基于检索到的原文回答，带防幻觉约束。

> 默认使用**内存向量库（SimpleVectorStore）**，本地不用装 PostgreSQL，导入填一个百炼 Key 就能跑通。PGVector 作为进阶可选项，见文末。

---

## 一、你电脑上已具备的条件
- JDK17：`C:\Users\89433\.jdks\ms-17.0.19`
- IntelliJ IDEA Community 2024.2.4（自带 Maven，无需另装）
- 无需本地 Maven、无需本地数据库（默认内存库）

## 二、导入 IDEA（照着点）
1. 打开 IDEA → `Open` → 选择本文件夹 `insurance-rag-demo`（能看到 pom.xml 的那一层）→ Open。
2. 右下角弹窗提示「Maven projects need to be imported」点 **Load / Auto Import**；若没弹窗，右侧 Maven 面板点刷新（Reload All Maven Projects）。
3. 等待依赖下载完成（首次较慢，需联网）。
4. 配置 JDK17：`File → Project Structure → Project`，SDK 选 `ms-17.0.19`（17）；`Language level` 选 17。

> 镜像已配置：全局 `C:\Users\89433\.m2\settings.xml` 已设阿里云镜像，工程内另存 `settings-aliyun.xml` 备份。
> 若 IDEA 没读到：`Settings → Build → Build Tools → Maven → User settings file` 指向该文件再 Reload。
> 若仍拉不动，一般是本机需要开代理/VPN，开启系统代理后再 Reload 即可。

## 三、填百炼 API Key（唯一必填项）
1. 打开阿里云百炼控制台：https://bailian.console.aliyun.com/ ，注册/登录 → 左侧「API-KEY 管理」创建一个 Key。
2. 打开 `src/main/resources/application.yml`，把
   `api-key: 请替换为你的百炼API-KEY` 换成你自己的 Key（新账号有免费额度）。

## 四、运行 & 测试
1. 运行 `InsuranceRagApplication`（绿色三角），看到 Started 即成功，端口 8080。
2. 上传规则文档（Postman / Apifox）：
   - `POST http://localhost:8080/rag/upload`
   - Body 选 `form-data`，字段名 `file`，上传一份 PDF/Word（基本法、佣金制度）。
   - 返回「文档《xxx》入库成功，分片数量：n」。
3. 问答：浏览器直接访问
   `http://localhost:8080/rag/chat?q=新人前6个月佣金比例是多少`
   或 Postman `GET http://localhost:8080/rag/chat?q=维持考核要求是什么`。
4. 没检索到相关条款时会直接拒答，这就是防幻觉效果，面试重点讲。

> 注意：内存库重启后数据清空，重新上传即可；练手/演示足够。

## 五、目录结构
```
insurance-rag-demo
├── pom.xml
├── sql/init-pgvector.sql          # 切 PGVector 时执行
├── docs/PgVectorStoreConfig.example.java  # PGVector 配置参考（默认不编译）
└── src/main
    ├── java/com/insurance/rag
    │   ├── InsuranceRagApplication.java
    │   ├── config/AiConfig.java          # ChatClient 配置
    │   ├── service/RagService.java      # 分片/入库/检索/防幻觉prompt（核心）
    │   ├── controller/RagController.java
    │   └── util/TikaDocUtil.java        # PDF/Word 解析
    └── resources/application.yml
```

## 六、进阶：切换到 PGVector（简历写「生产级」时用）
1. 放开 pom.xml 里注释的 pgvector starter 和 postgresql 依赖。
2. 装 PostgreSQL + pgvector（或 Docker 一行起：
   `docker run -d --name pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg16`）。
3. 执行 `sql/init-pgvector.sql`。
4. 把 `docs/PgVectorStoreConfig.example.java` 复制到 config 包并去掉注释。
5. 启动加参数 `--spring.profiles.active=pgvector`。

## 七、面试可讲的技术点
- 分片 700 字符 + 100 重叠：规则条款不宜切碎，重叠防止跨段截断。
- 防幻觉三层：相似度阈值 0.75 拦截 + system prompt 强制「只依据原文、无据拒答」+ 结尾标注检索片段。
- 成本：热点问题可加 Redis 缓存、token 限流（后续扩展项）。
- 数据安全：金融文档敏感，生产应私有化模型（Ollama），不用公网 Key。
- 下一步亮点：Function Calling，让 Agent 直接调用你原有的佣金核算接口查实时数据。
