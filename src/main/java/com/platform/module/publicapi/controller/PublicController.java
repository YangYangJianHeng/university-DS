package com.platform.module.publicapi.controller;

import com.platform.common.result.Result;
import com.platform.module.competition.mapper.CompetitionMapper;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开接口 —— 无需登录即可访问，用于落地页等场景
 */
@Tag(name = "Public API")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final ProjectMapper projectMapper;
    private final CompetitionMapper competitionMapper;

    public PublicController(UserMapper userMapper,
                            TeamMapper teamMapper,
                            ProjectMapper projectMapper,
                            CompetitionMapper competitionMapper) {
        this.userMapper = userMapper;
        this.teamMapper = teamMapper;
        this.projectMapper = projectMapper;
        this.competitionMapper = competitionMapper;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get public platform statistics (no auth required)")
    @Cacheable(value = "publicStats", key = "'platform'")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> data = new HashMap<>();

        long totalUsers = userMapper.selectCount(null);
        long totalTeams = teamMapper.selectCount(null);
        long totalProjects = projectMapper.selectCount(null);
        long totalCompetitions = competitionMapper.selectCount(null);

        data.put("totalUsers", totalUsers);
        data.put("totalTeams", totalTeams);
        data.put("totalProjects", totalProjects);
        data.put("totalCompetitions", totalCompetitions);

        return Result.success(data);
    }
}
