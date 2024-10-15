package com.work.json.controller;

import com.work.json.dto.InputDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.work.json.service.JsonAsCodeService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author linux
 */
@CrossOrigin
@RestController
@RequestMapping("/api/json")
@Slf4j
public class JsonAsCodeController {

    private final JsonAsCodeService jsonService;

    public JsonAsCodeController(JsonAsCodeService jsonService) {
        this.jsonService = jsonService;
    }

    @PostMapping(path = "/converter/code", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseBody
    public void converter(@RequestParam(name = "file") MultipartFile file,
            @RequestParam(name = "packageName", defaultValue = "com.empresa.dto") String packageName,
            @RequestParam(name = "className", defaultValue = "Welcome") String className,
            HttpServletResponse response) throws IOException {
        log.info("Type: {}", file.getContentType());
        String out = "json-to-code.zip";
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + out + "\"");

        InputDTO request = InputDTO.builder()
                .className(className)
                .input(file)
                .output(response.getOutputStream())
                .packageName(packageName)
                .build();

        jsonService.converter(request);
    }
}
