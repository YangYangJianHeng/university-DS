package com.platform.module.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTeamDTO {
    @NotBlank(message = "团队名称不能为空")
    @Size(min = 2, max = 50, message = "团队名称长度需要2-50位")
    private String name;

    private Long recruitId;
}
