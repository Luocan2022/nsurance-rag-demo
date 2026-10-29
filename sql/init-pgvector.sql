-- ============================================================
--  PGVector 初始化脚本（仅在切换到 PGVector 方案时执行）
--  本地无 PostgreSQL 时，默认内存向量库不需要本脚本。
--  执行：psql -U postgres -h localhost -p 5432 -f init-pgvector.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS rag_db;
\c rag_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- text-embedding-v3 为 1024 维；若换用 1536 维模型，请同步修改 dimensions
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1024)
);

CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx
    ON vector_store USING HNSW (embedding vector_cosine_ops);
