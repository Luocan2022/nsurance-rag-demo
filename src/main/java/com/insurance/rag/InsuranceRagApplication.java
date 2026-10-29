package com.insurance.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 银保销管规则智能问答 RAG 平台启动类。
 */
@SpringBootApplication
@EnableAsync
public class InsuranceRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceRagApplication.class, args);
    }
}
