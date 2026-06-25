package com.platform.module.team.service;

import com.platform.module.team.dto.CreateTeamDTO;
import com.platform.module.team.dto.UpdateTeamDTO;
import com.platform.module.team.vo.TeamMemberVO;
import com.platform.module.team.vo.TeamVO;
import com.platform.module.team.vo.TimelineEvent;

import java.util.List;

public interface TeamService {

    TeamVO createTeam(CreateTeamDTO dto);

    TeamVO getTeamById(Long id);

    TeamVO updateTeam(Long id, UpdateTeamDTO dto);

    List<TeamVO> getMyTeams(Long userId);

    List<TeamVO> getJoinedTeams(Long userId);

    void joinTeam(Long teamId, Long userId);

    void approveMember(Long teamId, Long memberId, boolean approved);

    void removeMember(Long teamId, Long userId);

    void quitTeam(Long teamId, Long userId);

    List<TeamMemberVO> getMembers(Long teamId);

    void disbandTeam(Long teamId);

    List<TimelineEvent> getTimeline(Long teamId);
}
