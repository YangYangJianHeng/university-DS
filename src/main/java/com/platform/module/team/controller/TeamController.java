package com.platform.module.team.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.team.dto.CreateTeamDTO;
import com.platform.module.team.dto.UpdateTeamDTO;
import com.platform.module.team.service.TeamService;
import com.platform.module.team.vo.TeamMemberVO;
import com.platform.module.team.vo.TeamVO;
import com.platform.module.team.vo.TimelineEvent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public Result<TeamVO> createTeam(@Valid @RequestBody CreateTeamDTO dto) {
        TeamVO teamVO = teamService.createTeam(dto);
        return Result.success(teamVO);
    }

    @GetMapping("/{id}")
    public Result<TeamVO> getTeamById(@PathVariable("id") Long id) {
        TeamVO teamVO = teamService.getTeamById(id);
        return Result.success(teamVO);
    }

    @PutMapping("/{id}")
    public Result<TeamVO> updateTeam(@PathVariable("id") Long id,
                                     @RequestBody UpdateTeamDTO dto) {
        TeamVO teamVO = teamService.updateTeam(id, dto);
        return Result.success(teamVO);
    }

    @GetMapping("/my")
    public Result<List<TeamVO>> getMyTeams() {
        Long userId = UserContext.getCurrentUserId();
        List<TeamVO> teams = teamService.getMyTeams(userId);
        return Result.success(teams);
    }

    @GetMapping("/joined")
    public Result<List<TeamVO>> getJoinedTeams() {
        Long userId = UserContext.getCurrentUserId();
        List<TeamVO> teams = teamService.getJoinedTeams(userId);
        return Result.success(teams);
    }

    @PostMapping("/{id}/join")
    public Result<Void> joinTeam(@PathVariable("id") Long teamId) {
        Long userId = UserContext.getCurrentUserId();
        teamService.joinTeam(teamId, userId);
        return Result.success("Application submitted successfully", null);
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approveMember(@PathVariable("id") Long teamId,
                                      @RequestParam Long memberId,
                                      @RequestParam boolean approved) {
        teamService.approveMember(teamId, memberId, approved);
        String message = approved ? "Member approved successfully" : "Member rejected successfully";
        return Result.success(message, null);
    }

    @DeleteMapping("/{id}/member/{userId}")
    public Result<Void> removeMember(@PathVariable("id") Long teamId,
                                     @PathVariable("userId") Long userId) {
        teamService.removeMember(teamId, userId);
        return Result.success("Member removed successfully", null);
    }

    @PostMapping("/{id}/quit")
    public Result<Void> quitTeam(@PathVariable("id") Long teamId) {
        Long userId = UserContext.getCurrentUserId();
        teamService.quitTeam(teamId, userId);
        return Result.success("Quit team successfully", null);
    }

    @GetMapping("/{id}/members")
    public Result<List<TeamMemberVO>> getMembers(@PathVariable("id") Long teamId) {
        List<TeamMemberVO> members = teamService.getMembers(teamId);
        return Result.success(members);
    }

    @PostMapping("/{id}/disband")
    public Result<Void> disbandTeam(@PathVariable("id") Long teamId) {
        teamService.disbandTeam(teamId);
        return Result.success("Team disbanded successfully", null);
    }

    @GetMapping("/{id}/timeline")
    public Result<List<TimelineEvent>> getTimeline(@PathVariable("id") Long teamId) {
        List<TimelineEvent> timeline = teamService.getTimeline(teamId);
        return Result.success(timeline);
    }
}
