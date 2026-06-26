package com.platform.module.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.team.dto.CreateTeamDTO;
import com.platform.module.team.dto.UpdateTeamDTO;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.entity.TeamTeacher;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.team.mapper.TeamTeacherMapper;
import com.platform.module.team.service.TeamService;
import com.platform.module.team.vo.TeamMemberVO;
import com.platform.module.team.vo.TeamVO;
import com.platform.module.team.vo.TimelineEvent;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamTeacherMapper teamTeacherMapper;
    private final UserMapper userMapper;

    public TeamServiceImpl(TeamMapper teamMapper,
                           TeamMemberMapper teamMemberMapper,
                           TeamTeacherMapper teamTeacherMapper,
                           UserMapper userMapper) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.teamTeacherMapper = teamTeacherMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO createTeam(CreateTeamDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();

        Team team = new Team();
        team.setName(dto.getName());
        team.setRecruitId(dto.getRecruitId());
        team.setLeaderId(currentUserId);
        team.setStatus("ACTIVE");
        teamMapper.insert(team);

        // Add leader as a member with role LEADER
        TeamMember leader = new TeamMember();
        leader.setTeamId(team.getId());
        leader.setUserId(currentUserId);
        leader.setRole("LEADER");
        teamMemberMapper.insert(leader);

        return buildTeamVO(team);
    }

    @Override
    public TeamVO getTeamById(Long id) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        return buildTeamVO(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO updateTeam(Long id, UpdateTeamDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        // 已关闭的团队不允许编辑
        if ("CLOSED".equals(team.getStatus())) {
            throw BusinessException.badRequest("团队已关闭，无法编辑。请先重新开启关联项目");
        }
        if (!team.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("Only team leader can update the team");
        }

        if (dto.getName() != null) {
            team.setName(dto.getName());
        }
        if (dto.getStatus() != null) {
            team.setStatus(dto.getStatus());
        }

        teamMapper.updateById(team);
        return buildTeamVO(team);
    }

    @Override
    public List<TeamVO> getMyTeams(Long userId) {
        // 查询用户作为队长的团队
        List<Team> leaderTeams = teamMapper.selectList(
                new LambdaQueryWrapper<Team>().eq(Team::getLeaderId, userId));
        // 查询用户作为成员加入的团队
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        List<Long> memberTeamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .filter(tid -> leaderTeams.stream().noneMatch(t -> t.getId().equals(tid)))
                .collect(Collectors.toList());
        List<Team> memberTeams = memberTeamIds.isEmpty()
                ? Collections.emptyList()
                : teamMapper.selectBatchIds(memberTeamIds);

        List<Team> allTeams = new ArrayList<>(leaderTeams);
        allTeams.addAll(memberTeams);
        return allTeams.stream().map(this::buildTeamVO).collect(Collectors.toList());
    }

    @Override
    public List<TeamVO> getJoinedTeams(Long userId) {
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getUserId, userId));
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> teamIds = members.stream()
                .map(TeamMember::getTeamId)
                .collect(Collectors.toList());
        List<Team> teams = teamMapper.selectBatchIds(teamIds);
        return teams.stream().map(this::buildTeamVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinTeam(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        // 检查团队状态
        if ("DISBANDED".equals(team.getStatus())) {
            throw BusinessException.badRequest("团队已解散，无法加入");
        }
        if ("CLOSED".equals(team.getStatus())) {
            throw BusinessException.badRequest("团队已关闭，无法加入");
        }

        // Check if user is already a member (包括PENDING状态)
        Long count = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId));
        if (count > 0) {
            throw BusinessException.conflict("User is already a member of this team");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole("PENDING");
        teamMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveMember(Long teamId, Long memberId, boolean approved) {
        Long currentUserId = UserContext.getCurrentUserId();
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        if (!team.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("Only team leader can approve or reject members");
        }

        TeamMember member = teamMemberMapper.selectById(memberId);
        if (member == null || !member.getTeamId().equals(teamId)) {
            throw BusinessException.notFound("Member record not found");
        }

        if (approved) {
            member.setRole("MEMBER");
            teamMemberMapper.updateById(member);
        } else {
            teamMemberMapper.deleteById(memberId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long teamId, Long userId) {
        Long currentUserId = UserContext.getCurrentUserId();
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        if (!team.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("Only team leader can remove members");
        }

        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitTeam(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        if (team.getLeaderId().equals(userId)) {
            throw BusinessException.badRequest("Team leader cannot quit the team, please disband instead");
        }

        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId));
    }

    @Override
    public List<TeamMemberVO> getMembers(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }

        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId));

        // 批量查询用户，避免N+1问题
        List<Long> userIds = members.stream()
                .map(TeamMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userNameMap.put(user.getId(), user.getUsername());
            }
        }

        Map<Long, String> finalUserNameMap = userNameMap;
        return members.stream().map(m -> {
            TeamMemberVO vo = new TeamMemberVO();
            vo.setId(m.getId());
            vo.setTeamId(m.getTeamId());
            vo.setUserId(m.getUserId());
            vo.setRole(m.getRole());
            vo.setJoinedAt(m.getJoinedAt());
            vo.setUserName(finalUserNameMap.getOrDefault(m.getUserId(), "Unknown"));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disbandTeam(Long teamId) {
        Long currentUserId = UserContext.getCurrentUserId();
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }
        if (!team.getLeaderId().equals(currentUserId)) {
            throw BusinessException.forbidden("Only team leader can disband the team");
        }

        team.setStatus("DISBANDED");
        teamMapper.updateById(team);

        // Remove all members
        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId));
    }

    @Override
    public List<TimelineEvent> getTimeline(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw BusinessException.notFound("Team not found");
        }

        List<TimelineEvent> events = new ArrayList<>();

        // Team creation event
        TimelineEvent createEvent = new TimelineEvent();
        createEvent.setType("TEAM_CREATED");
        createEvent.setDescription("Team \"" + team.getName() + "\" was created");
        createEvent.setTimestamp(team.getCreatedAt());
        events.add(createEvent);

        // Member join events
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .orderByAsc(TeamMember::getJoinedAt));

        for (TeamMember member : members) {
            TimelineEvent event = new TimelineEvent();
            event.setTimestamp(member.getJoinedAt());

            if ("LEADER".equals(member.getRole())) {
                event.setType("LEADER_JOINED");
                event.setDescription("Team leader joined the team");
            } else if ("MEMBER".equals(member.getRole())) {
                event.setType("MEMBER_JOINED");
                event.setDescription("Member joined the team");
            } else if ("PENDING".equals(member.getRole())) {
                event.setType("MEMBER_APPLIED");
                event.setDescription("User applied to join the team");
            }
            events.add(event);
        }

        // Teacher invitation events
        List<TeamTeacher> teachers = teamTeacherMapper.selectList(
                new LambdaQueryWrapper<TeamTeacher>()
                        .eq(TeamTeacher::getTeamId, teamId)
                        .orderByAsc(TeamTeacher::getInvitedAt));

        for (TeamTeacher teacher : teachers) {
            TimelineEvent inviteEvent = new TimelineEvent();
            inviteEvent.setType("TEACHER_INVITED");
            inviteEvent.setDescription("Teacher was invited to the team");
            inviteEvent.setTimestamp(teacher.getInvitedAt());
            events.add(inviteEvent);

            if (teacher.getApprovedAt() != null) {
                TimelineEvent approveEvent = new TimelineEvent();
                approveEvent.setType("TEACHER_APPROVED");
                approveEvent.setDescription("Teacher approved the invitation");
                approveEvent.setTimestamp(teacher.getApprovedAt());
                events.add(approveEvent);
            }
        }

        // Sort all events by timestamp
        events.sort(Comparator.comparing(TimelineEvent::getTimestamp));

        return events;
    }

    private TeamVO buildTeamVO(Team team) {
        TeamVO vo = new TeamVO();
        vo.setId(team.getId());
        vo.setName(team.getName());
        vo.setLeaderId(team.getLeaderId());
        vo.setRecruitId(team.getRecruitId());
        vo.setStatus(team.getStatus());
        vo.setCreatedAt(team.getCreatedAt());

        // Set member count
        Long memberCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, team.getId())
                        .ne(TeamMember::getRole, "PENDING"));
        vo.setMemberCount(memberCount.intValue());

        // Set leader name
        User leader = userMapper.selectById(team.getLeaderId());
        if (leader != null) {
            vo.setLeaderName(leader.getUsername());
        }

        return vo;
    }
}
