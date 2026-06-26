package com.platform.module.collaboration.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmissionRequest {
    private Long taskId;
    private String title;
    private String description;
    /** 文件URL列表 */
    private List<String> fileUrls;
}
