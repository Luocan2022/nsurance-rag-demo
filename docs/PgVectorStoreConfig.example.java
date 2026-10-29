package com.insurance.rag.config;

// ============================================================
//  可选：切换到 PGVector 时使用。
//  使用步骤：
//   1. 放开 pom.xml 中 pgvector starter 与 postgresql 的注释；
//   2. 把本文件复制到 src/main/java/com/insurance/rag/config/ 并去掉整段注释；
//   3. 执行 sql/init-pgvector.sql 初始化数据库；
//   4. 启动加参数 --spring.profiles.active=pgvector
//  注意：默认使用内存 SimpleVectorStore，本文件在 docs 目录，不参与编译。
// ============================================================
/*
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Profile("pgvector")
public class PgVectorStoreConfig {

    @Bean
    public PgVectorStore vectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(new JdbcTemplate(dataSource), embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
    }
}
*/
