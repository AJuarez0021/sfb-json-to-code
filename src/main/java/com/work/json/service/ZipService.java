package com.work.json.service;

import com.work.json.dto.FileDTO;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 *
 * @author linux
 */
public interface ZipService {

    void zip(List<FileDTO> files, OutputStream out) throws IOException;
}
