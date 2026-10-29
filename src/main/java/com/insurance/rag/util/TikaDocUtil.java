package com.insurance.rag.util;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文档解析工具：基于 Tika 抽取 PDF / DOC / DOCX / TXT 等格式的纯文本。
 */
@Component
public class TikaDocUtil {

    private final Tika tika = new Tika();

    public String parseText(MultipartFile file) throws IOException {
        try {
            return tika.parseToString(file.getInputStream());
        } catch (TikaException e) {
            throw new RuntimeException(e);
        }
    }
}
