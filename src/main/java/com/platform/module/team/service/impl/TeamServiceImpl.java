package com.platform.module.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.project.service.ProjectService;
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
import com.platform.module.collaboration.entity.Message;
import com.platform.module.collaboration.mapper.MessageMapper;
import com.platform.module.file.entity.FileEntity;
import com.platform.module.file.mapper.FileMapper;
import com.platform.module.recruit.entity.Application;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.ApplicationMapper;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
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
    private final ProjectMapper projectMapper;
    private final ProjectService projectService;
    private final TaskMapper taskMapper;
    private final FileMapper fileMapper;
    private final RecruitMapper recruitMapper;
    private final ApplicationMapper applicationMapper;
    private final AchievementMapper achievementMapper;
    private final MessageMapper messageMapper;

    public TeamServiceImpl(TeamMapper teamMapper,
                           TeamMemberMapper teamMemberMapper,
                           TeamTeacherMapper teamTeacherMapper,
                           UserMapper userMapper,
                           ProjectMapper projectMapper,
                           ProjectService projectService,
                           TaskMapper taskMapper,
                           FileMapper fileMapper,
                           RecruitMapper recruitMapper,
                           ApplicationMapper applicationMapper,
                           AchievementMapper achievementMapper,
                           MessageMapper messageMapper) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.teamTeacherMapper = teamTeacherMapper;
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
        this.projectService = projectService;
        this.taskMapper = taskMapper;
        this.fileMapper = fileMapper;
        this.recruitMapper = recruitMapper;
        this.applicationMapper = applicationMapper;
        this.achievementMapper = achievementMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO createTeam(CreateTeamDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();

        Team team = new Team();
        team.setName(dto.getName());
        team.setDescription(dto.getDescription());
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
        // 不能移除队长自身
        if (team.getLeaderId().equals(userId)) {
            throw BusinessException.badRequest("队长不能移除自己，请转让队长或解散团队");
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
            // Team leader quitting — auto-transfer leadership to earliest-joined member
            List<TeamMember> remainingMembers = teamMemberMapper.selectList(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, teamId)
                            .ne(TeamMember::getUserId, userId));
            if (!remainingMembers.isEmpty()) {
                // Pick the member with the oldest joinedAt as new leader
                TeamMember newLeader = remainingMembers.stream()
                        .filter(m -> m.getJoinedAt() != null)
                        .min(Comparator.comparing(TeamMember::getJoinedAt))
                        .orElse(remainingMembers.get(0));
                team.setLeaderId(newLeader.getUserId());
                teamMapper.updateById(team);
            } else {
                // No other members → cascade delete the entire team and all related data
                // 0. Delete projects referencing this team (FK: project_ibfk_1)
                List<Project> teamProjects = projectMapper.selectList(
                        new LambdaQueryWrapper<Project>().eq(Project::getTeamId, teamId));
                for (Project p : teamProjects) {
                    projectService.deleteProject(p.getId());
                }
                // 1. Delete achievements referencing this team
                achievementMapper.delete(
                        new LambdaQueryWrapper<Achievement>().eq(Achievement::getTeamId, teamId));
                // 2. Delete messages referencing this team
                messageMapper.delete(
                        new LambdaQueryWrapper<Message>().eq(Message::getTeamId, teamId));
                // 3. Delete applications referencing recruits of this team
                List<Recruit> teamRecruits = recruitMapper.selectList(
                        new LambdaQueryWrapper<Recruit>().eq(Recruit::getTeamId, teamId));
                if (!teamRecruits.isEmpty()) {
                    List<Long> recruitIds = teamRecruits.stream().map(Recruit::getId).collect(Collectors.toList());
                    applicationMapper.delete(
                            new LambdaQueryWrapper<Application>().in(Application::getRecruitId, recruitIds));
                }
                // —— Before deleting recruit: break team.recruit_id → recruit FK (MUST use explicit UpdateWrapper: MyBatis-Plus ignores null in updateById)
                if (team.getRecruitId() != null) {
                    teamMapper.update(null, new LambdaUpdateWrapper<Team>()
                            .set(Team::getRecruitId, null)
                            .eq(Team::getId, teamId));
                }
                // 4. Delete recruits referencing this team
                recruitMapper.delete(
                        new LambdaQueryWrapper<Recruit>().eq(Recruit::getTeamId, teamId));
                // 5. Delete files referencing this team
                fileMapper.delete(
                        new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getTeamId, teamId));
                // 6. Delete all team members (including leader)
                teamMemberMapper.delete(
                        new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
                // 7. Delete team teachers
                teamTeacherMapper.delete(
                        new LambdaQueryWrapper<TeamTeacher>().eq(TeamTeacher::getTeamId, teamId));
                // 8. Finally delete the team itself
                teamMapper.deleteById(teamId);
                return;
            }
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
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        return members.stream().map(m -> {
            User user = userMap.get(m.getUserId());
            TeamMemberVO vo = new TeamMemberVO();
            vo.setId(m.getId());
            vo.setTeamId(m.getTeamId());
            vo.setUserId(m.getUserId());
            vo.setRole(m.getRole());
            vo.setJoinedAt(m.getJoinedAt());
            vo.setUserName(user != null ? user.getUsername() : "Unknown");
            vo.setAvatar(user != null ? user.getAvatar() : null);
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
            User u = userMapper.selectById(member.getUserId());
            String uname = u != null ? u.getUsername() : "User#" + member.getUserId();

            TimelineEvent event = new TimelineEvent();
            event.setTimestamp(member.getJoinedAt());

            if ("LEADER".equals(member.getRole())) {
                event.setType("LEADER_JOINED");
                event.setDescription(uname + " 作为队长创建了团队");
            } else if ("MEMBER".equals(member.getRole())) {
                event.setType("MEMBER_JOINED");
                event.setDescription(uname + " 加入了团队");
            } else if ("PENDING".equals(member.getRole())) {
                event.setType("MEMBER_APPLIED");
                event.setDescription(uname + " 申请加入团队");
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

        // ---- 团队关联项目的动态 ----
        List<Project> teamProjects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getTeamId, teamId));
        if (!teamProjects.isEmpty()) {
            List<Long> projectIds = teamProjects.stream()
                    .map(Project::getId)
                    .collect(Collectors.toList());

            // 项目创建事件
            for (Project project : teamProjects) {
                User pLeader = userMapper.selectById(project.getLeaderId());
                String pname = pLeader != null ? pLeader.getUsername() : "User#" + project.getLeaderId();

                TimelineEvent projectEvent = new TimelineEvent();
                projectEvent.setType("PROJECT_CREATED");
                projectEvent.setDescription(pname + " 创建了项目: " + project.getName());
                projectEvent.setTimestamp(project.getCreatedAt());
                projectEvent.setProjectName(project.getName());
                events.add(projectEvent);
            }

            // 任务事件
            List<Task> tasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>()
                            .in(Task::getProjectId, projectIds)
                            .orderByDesc(Task::getCreatedAt));

            for (Task task : tasks) {
                User creator = userMapper.selectById(task.getCreatorId());
                String cname = creator != null ? creator.getUsername() : "User#" + task.getCreatorId();

                // 找到任务所属的项目名
                String projName = teamProjects.stream()
                        .filter(p -> p.getId().equals(task.getProjectId()))
                        .findFirst()
                        .map(Project::getName)
                        .orElse(null);

                // 任务发布事件
                TimelineEvent taskEvent = new TimelineEvent();
                taskEvent.setType("TASK_CREATED");
                taskEvent.setDescription(cname + " 发布了任务: " + task.getTitle());
                taskEvent.setTimestamp(task.getCreatedAt());
                taskEvent.setProjectName(projName);
                taskEvent.setTaskId(task.getId());
                taskEvent.setTaskTitle(task.getTitle());
                events.add(taskEvent);

                // 任务完成事件
                if ("DONE".equals(task.getStatus()) && task.getUpdatedAt() != null) {
                    TimelineEvent doneEvent = new TimelineEvent();
                    doneEvent.setType("TASK_COMPLETED");
                    doneEvent.setDescription("任务已完成: " + task.getTitle());
                    doneEvent.setTimestamp(task.getUpdatedAt());
                    doneEvent.setProjectName(projName);
                    doneEvent.setTaskId(task.getId());
                    doneEvent.setTaskTitle(task.getTitle());
                    events.add(doneEvent);
                }
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
