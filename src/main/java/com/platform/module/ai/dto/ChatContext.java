package com.platform.module.ai.dto;

import com.platform.module.competition.entity.Competition;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.project.entity.Project;
import lombok.Data;

import java.util.List;

/**
 * AI Agent 对话上下文
 * 封装用户相关数据，注入到 Prompt 中
 */
@Data
public class ChatContext {
    private String userName;
    private String userMajor;
    private String userGrade;
    private List<Project> userProjects;
    private List<Task> userTasks;
    private List<Competition> recentCompetitions;
}
