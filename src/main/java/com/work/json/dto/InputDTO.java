package com.work.json.dto;

import java.io.OutputStream;
import lombok.Builder;
import lombok.ToString;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author linux
 */
@Builder
@Data
@ToString
public class InputDTO {
    private MultipartFile input; 
    private OutputStream output;
    private String packageName;
    private String className;
}
