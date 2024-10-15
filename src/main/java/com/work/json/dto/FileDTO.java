package com.work.json.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 *
 * @author linux
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileDTO {

    private byte[] file;
    private String fileName;

}
