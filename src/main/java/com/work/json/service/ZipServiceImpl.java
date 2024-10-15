package com.work.json.service;

import com.work.json.util.IOUtils;
import com.work.json.dto.FileDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/**
 *
 * @author linux
 */
@Service
public class ZipServiceImpl implements ZipService {

    @Override
    public void zip(List<FileDTO> files, OutputStream out) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (FileDTO file : files) {
                ZipEntry zipEntry = new ZipEntry(file.getFileName());
                zipOut.putNextEntry(zipEntry);
                IOUtils.copy(new ByteArrayInputStream(file.getFile()), zipOut);
                zipOut.closeEntry();
            }
        }
    }

   
}
