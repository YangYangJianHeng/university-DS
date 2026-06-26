package com.platform.module.collaboration.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    /** APPROVED 或 REJECTED */
    private String action;
    /** 验收意见 */
    private String comment;
}
