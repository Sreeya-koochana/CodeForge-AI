package com.aibuilder.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CodeProcessorService {

    private static final Pattern FILE_PATTERN = Pattern.compile(
            "(?ms)(?:^|\\n)([\\w./-]+\\.(?:java|xml|yml|yaml|properties|md))\\s*\\n```[a-zA-Z]*\\n(.*?)\\n```"
    );

    public List<ProcessedFile> extractFiles(String generatedCode, String entityName) {
        Matcher matcher = FILE_PATTERN.matcher(generatedCode);
        List<ProcessedFile> files = new ArrayList<>();

        while (matcher.find()) {
            files.add(new ProcessedFile(matcher.group(1).trim(), matcher.group(2).trim()));
        }

        if (files.isEmpty()) {
            files.add(new ProcessedFile(entityName + "Generated.java", generatedCode.trim()));
        }

        return files;
    }

    public record ProcessedFile(String fileName, String content) {
    }
}
