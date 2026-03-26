package com.aibuilder.util;

import com.aibuilder.dto.GeneratedFileResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class ZipBuilderUtil {

    public byte[] buildZip(List<GeneratedFileResponse> files) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (GeneratedFileResponse file : files) {
                zipOutputStream.putNextEntry(new ZipEntry(file.fileName()));
                zipOutputStream.write(file.fileContent().getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create ZIP file.", exception);
        }
    }
}
