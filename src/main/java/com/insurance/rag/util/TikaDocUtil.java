package com.insurance.rag.util;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析工具：基于 Tika 抽取 PDF / DOC / DOCX / TXT 等格式的纯文本。
 */
@Component
public class TikaDocUtil {

    private final Tika tika = new Tika();

    public String parseText(MultipartFile file) throws IOException {
        return parseText(file.getInputStream());
    }

    /** 直接从输入流解析（启动加载 classpath 文档用） */
    public String parseText(InputStream stream) throws IOException {
        try {
            return tika.parseToString(stream);
        } catch (TikaException e) {
            throw new RuntimeException(e);
        }
    }
}
