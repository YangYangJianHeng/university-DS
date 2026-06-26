package com.platform.module.recruit.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.recruit.dto.ConvertToProjectDTO;
import com.platform.module.recruit.dto.CreateRecruitDTO;
import com.platform.module.recruit.dto.UpdateRecruitDTO;
import com.platform.module.recruit.entity.Application;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.ApplicationMapper;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.recruit.service.RecruitService;
import com.platform.module.recruit.vo.RecruitVO;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitServiceImpl implements RecruitService {

    private final RecruitMapper recruitMapper;
    private final ApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;

    // 招募转项目需要的Mapper
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public RecruitVO createRecruit(CreateRecruitDTO dto) {
        Recruit recruit = new Recruit();
        recruit.setPublisherId(UserContext.getCurrentUserId());
        recruit.setTitle(dto.getTitle());
        recruit.setProjectIcon(dto.getProjectIcon());
        recruit.setProjectName(dto.getProjectName());
        recruit.setDescription(dto.getDescription());
        recruit.setProjectType(dto.getProjectType());

        // Convert skill list to JSON string for storage
        if (dto.getRequiredSkills() != null && !dto.getRequiredSkills().isEmpty()) {
            recruit.setRequiredSkills(JSON.toJSONString(dto.getRequiredSkills()));
        }

        recruit.setMaxMembers(dto.getMaxMembers());
        recruit.setCurrentMembers(1);
        recruit.setDeadline(dto.getDeadline());
        recruit.setStatus("OPEN");
        recruit.setAuditStatus("PENDING");
        recruit.setViewCount(0);

        recruitMapper.insert(recruit);
        return toVO(recruit);
    }

    @Override
    @Transactional
    public RecruitVO getRecruitById(Long id) {
        Recruit recruit = recruitMapper.selectById(id);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        // Increment view count asynchronously
        recruitMapper.incrementViewCount(id);
        recruit.setViewCount(recruit.getViewCount() + 1);
        return toVO(recruit);
    }

    @Override
    public IPage<RecruitVO> listRecruits(String projectType, String status, int page, int size) {
        Page<Recruit> recruitPage = new Page<>(page, size);

        LambdaQueryWrapper<Recruit> wrapper = new LambdaQueryWrapper<Recruit>()
                .eq(StringUtils.hasText(projectType), Recruit::getProjectType, projectType)
                .eq(StringUtils.hasText(status), Recruit::getStatus, status)
                .orderByDesc(Recruit::getCreatedAt);

        Page<Recruit> result = recruitMapper.selectPage(recruitPage, wrapper);

        // Convert to VO page
        Page<RecruitVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public RecruitVO updateRecruit(Long id, UpdateRecruitDTO dto) {
        Recruit recruit = recruitMapper.selectById(id);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        if (!recruit.getPublisherId().equals(UserContext.getCurrentUserId()) && !isAdmin()) {
            throw BusinessException.forbidden("无权修改此招募信息");
        }
        if (!"OPEN".equals(recruit.getStatus())) {
            throw BusinessException.badRequest("该招募已关闭，无法修改");
        }

        if (dto.getTitle() != null) {
            recruit.setTitle(dto.getTitle());
        }
        if (dto.getProjectIcon() != null) {
            recruit.setProjectIcon(dto.getProjectIcon());
        }
        if (dto.getProjectName() != null) {
            recruit.setProjectName(dto.getProjectName());
        }
        if (dto.getDescription() != null) {
            recruit.setDescription(dto.getDescription());
        }
        if (dto.getProjectType() != null) {
            recruit.setProjectType(dto.getProjectType());
        }
        if (dto.getRequiredSkills() != null) {
            recruit.setRequiredSkills(JSON.toJSONString(dto.getRequiredSkills()));
        }
        if (dto.getMaxMembers() != null) {
            recruit.setMaxMembers(dto.getMaxMembers());
        }
        if (dto.getDeadline() != null) {
            recruit.setDeadline(dto.getDeadline());
        }

        recruitMapper.updateById(recruit);
        return toVO(recruit);
    }

    @Override
    @Transactional
    public void closeRecruit(Long id) {
        Recruit recruit = recruitMapper.selectById(id);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        if (!recruit.getPublisherId().equals(UserContext.getCurrentUserId()) && !isAdmin()) {
            throw BusinessException.forbidden("无权关闭此招募信息");
        }
        if (!"OPEN".equals(recruit.getStatus())) {
            throw BusinessException.badRequest("该招募已关闭");
        }
        recruit.setStatus("CLOSED");
        recruitMapper.updateById(recruit);
    }

    @Override
    public List<Map<String, Object>> findMatchingUsers(Long recruitId) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }

        List<String> requiredSkills;
        try {
            requiredSkills = JSON.parseArray(recruit.getRequiredSkills(), String.class);
        } catch (Exception e) {
            requiredSkills = Collections.emptyList();
        }

        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return Collections.emptyList();
        }

        // Get all user skills from the user_skill table
        List<UserSkill> allUserSkills = userSkillMapper.selectList(null);

        // Group skills by user
        Map<Long, List<String>> userSkillsMap = allUserSkills.stream()
                .collect(Collectors.groupingBy(
                        UserSkill::getUserId,
                        Collectors.mapping(UserSkill::getSkillName, Collectors.toList())
                ));

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map.Entry<Long, List<String>> entry : userSkillsMap.entrySet()) {
            double similarity = cosineSimilarity(requiredSkills, entry.getValue());
            if (similarity > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", entry.getKey());

                User user = userMapper.selectById(entry.getKey());
                item.put("username", user != null ? user.getUsername() : "Unknown");
                item.put("major", user != null ? user.getMajor() : null);
                item.put("grade", user != null ? user.getGrade() : null);
                item.put("userSkills", entry.getValue());
                item.put("similarity", Math.round(similarity * 10000.0) / 10000.0);

                results.add(item);
            }
        }

        // Sort by similarity descending
        results.sort((a, b) -> Double.compare(
                (Double) b.get("similarity"),
                (Double) a.get("similarity")
        ));

        return results;
    }

    @Override
    @Transactional
    public void applyToRecruit(Long recruitId, Long userId) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        if (!"OPEN".equals(recruit.getStatus())) {
            throw BusinessException.badRequest("该招募已关闭，无法申请");
        }
        if (recruit.getPublisherId().equals(userId)) {
            throw BusinessException.badRequest("不能申请自己的招募");
        }
        if (recruit.getMaxMembers() != null && recruit.getCurrentMembers() != null
                && recruit.getCurrentMembers() >= recruit.getMaxMembers()) {
            throw BusinessException.badRequest("该招募成员已满");
        }

        boolean exists = applicationMapper.existsByRecruitAndUser(recruitId, userId);
        if (exists) {
            throw BusinessException.conflict("您已申请过该招募");
        }

        Application application = new Application();
        application.setRecruitId(recruitId);
        application.setApplicantId(userId);
        application.setStatus("PENDING");
        applicationMapper.insert(application);
    }

    @Override
    @Transactional
    public void approveApplication(Long applicationId, Long publisherId, boolean approved) {
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw BusinessException.notFound("申请记录不存在");
        }

        Recruit recruit = recruitMapper.selectById(application.getRecruitId());
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }

        // 只有发布者或管理员可以审核申请
        if (!recruit.getPublisherId().equals(publisherId) && !isAdmin()) {
            throw BusinessException.forbidden("只有发布者或管理员可以审核申请");
        }

        if (!"PENDING".equals(application.getStatus())) {
            throw BusinessException.badRequest("该申请已处理");
        }

        if (approved) {
            // 检查是否已满员
            if (recruit.getMaxMembers() != null && recruit.getCurrentMembers() != null
                    && recruit.getCurrentMembers() >= recruit.getMaxMembers()) {
                throw BusinessException.badRequest("该招募成员已满");
            }
            application.setStatus("ACCEPTED");
            recruit.setCurrentMembers((recruit.getCurrentMembers() != null ? recruit.getCurrentMembers() : 0) + 1);
            recruitMapper.updateById(recruit);

            // 如果该招募已经转为项目/团队，自动将申请者加入团队
            if (recruit.getTeamId() != null) {
                Team team = teamMapper.selectById(recruit.getTeamId());
                if (team != null) {
                    // 检查是否已是团队成员
                    Long exists = teamMemberMapper.selectCount(
                            new LambdaQueryWrapper<TeamMember>()
                                    .eq(TeamMember::getTeamId, recruit.getTeamId())
                                    .eq(TeamMember::getUserId, application.getApplicantId()));
                    if (exists == 0) {
                        TeamMember member = new TeamMember();
                        member.setTeamId(recruit.getTeamId());
                        member.setUserId(application.getApplicantId());
                        member.setRole("MEMBER");
                        teamMemberMapper.insert(member);
                        log.info("Applicant {} automatically joined team {} from recruit {}",
                                application.getApplicantId(), recruit.getTeamId(), recruit.getId());
                    }
                }
            }
        } else {
            application.setStatus("REJECTED");
        }

        applicationMapper.updateById(application);
    }

    @Override
    public List<RecruitVO> getMyRecruits(Long userId) {
        List<Recruit> recruits = recruitMapper.selectList(
                new LambdaQueryWrapper<Recruit>()
                        .eq(Recruit::getPublisherId, userId)
                        .orderByDesc(Recruit::getCreatedAt));
        return recruits.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<Application> getApplications(Long recruitId) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }
        // 发布者或管理员可以查看申请列表
        if (!recruit.getPublisherId().equals(UserContext.getCurrentUserId())
                && !isAdmin()) {
            throw BusinessException.forbidden("无权查看此招募的申请列表");
        }
        return applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getRecruitId, recruitId)
                        .orderByDesc(Application::getAppliedAt));
    }

    // -----------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------

    private RecruitVO toVO(Recruit recruit) {
        RecruitVO vo = new RecruitVO();
        vo.setId(recruit.getId());
        vo.setPublisherId(recruit.getPublisherId());
        vo.setTitle(recruit.getTitle());
        vo.setProjectIcon(recruit.getProjectIcon());
        vo.setProjectName(recruit.getProjectName());
        vo.setDescription(recruit.getDescription());
        vo.setProjectType(recruit.getProjectType());
        vo.setMaxMembers(recruit.getMaxMembers());
        vo.setCurrentMembers(recruit.getCurrentMembers());
        vo.setDeadline(recruit.getDeadline());
        vo.setStatus(recruit.getStatus());
        vo.setAuditStatus(recruit.getAuditStatus());
        vo.setViewCount(recruit.getViewCount());
        vo.setCreatedAt(recruit.getCreatedAt());

        // 转为项目后的关联字段
        vo.setProjectId(recruit.getProjectId());
        vo.setTeamId(recruit.getTeamId());
        vo.setContinueRecruit(recruit.getContinueRecruit());

        // Parse requiredSkills JSON string to list
        if (StringUtils.hasText(recruit.getRequiredSkills())) {
            try {
                vo.setRequiredSkills(JSON.parseArray(recruit.getRequiredSkills(), String.class));
            } catch (Exception e) {
                log.warn("Failed to parse requiredSkills JSON: {}", recruit.getRequiredSkills(), e);
                vo.setRequiredSkills(Collections.emptyList());
            }
        } else {
            vo.setRequiredSkills(Collections.emptyList());
        }

        // Look up publisher name
        try {
            User publisher = userMapper.selectById(recruit.getPublisherId());
            vo.setPublisherName(publisher != null ? publisher.getUsername() : "Unknown");
        } catch (Exception e) {
            log.warn("Failed to fetch publisher info for id: {}", recruit.getPublisherId(), e);
            vo.setPublisherName("Unknown");
        }

        return vo;
    }

    /**
     * Compute cosine similarity between two skill lists.
     * Each skill presence is treated as a binary vector (1 = present, 0 = absent).
     */
    private double cosineSimilarity(List<String> skillsA, List<String> skillsB) {
        Set<String> allSkills = new HashSet<>(skillsA);
        allSkills.addAll(skillsB);

        int[] vectorA = new int[allSkills.size()];
        int[] vectorB = new int[allSkills.size()];

        int index = 0;
        for (String skill : allSkills) {
            vectorA[index] = skillsA.contains(skill) ? 1 : 0;
            vectorB[index] = skillsB.contains(skill) ? 1 : 0;
            index++;
        }

        int dotProduct = 0;
        int normA = 0;
        int normB = 0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Check if the current user has ADMIN role.
     */
    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(UserContext.getCurrentUserRole());
    }

    // -----------------------------------------------------------
    // 招募转项目
    // -----------------------------------------------------------

    @Override
    @Transactional
    public Long convertToProject(Long recruitId, ConvertToProjectDTO dto) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }

        // 权限检查：只有发布者或管理员可以转换
        Long currentUserId = UserContext.getCurrentUserId();
        if (!recruit.getPublisherId().equals(currentUserId) && !isAdmin()) {
            throw BusinessException.forbidden("只有招募发布者或管理员可以将招募转为项目");
        }

        // 检查是否已经转换过了
        if (recruit.getProjectId() != null || recruit.getTeamId() != null) {
            throw BusinessException.conflict("该招募已经转为项目，请勿重复操作");
        }

        String projectName = dto.getProjectName() != null && !dto.getProjectName().trim().isEmpty()
                ? dto.getProjectName().trim()
                : recruit.getTitle().trim();

        String projectDescription = dto.getProjectDescription() != null
                ? dto.getProjectDescription()
                : recruit.getDescription();

        // ---------- 1. 创建团队 ----------
        Team team = new Team();
        team.setName(projectName + " 团队");
        team.setLeaderId(recruit.getPublisherId());
        team.setRecruitId(recruit.getId());
        team.setStatus("ACTIVE");
        teamMapper.insert(team);
        Long teamId = team.getId();

        // ---------- 2. 将发布者加入为队长 ----------
        TeamMember leader = new TeamMember();
        leader.setTeamId(teamId);
        leader.setUserId(recruit.getPublisherId());
        leader.setRole("LEADER");
        teamMemberMapper.insert(leader);

        // ---------- 3. 将已通过的申请者加入团队 ----------
        List<Application> acceptedApps = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getRecruitId, recruit.getId())
                        .eq(Application::getStatus, "ACCEPTED"));
        for (Application app : acceptedApps) {
            // 防止重复（发布者可能也申请了）
            if (app.getApplicantId().equals(recruit.getPublisherId())) {
                continue;
            }
            TeamMember member = new TeamMember();
            member.setTeamId(teamId);
            member.setUserId(app.getApplicantId());
            member.setRole("MEMBER");
            teamMemberMapper.insert(member);
        }

        // ---------- 4. 创建项目 ----------
        Project project = new Project();
        project.setTeamId(teamId);
        project.setLeaderId(recruit.getPublisherId());
        project.setName(projectName);
        // 如果招募时有项目图标，携带过去
        if (recruit.getProjectIcon() != null && !recruit.getProjectIcon().isEmpty()) {
            project.setIcon(recruit.getProjectIcon());
        }
        project.setDescription(projectDescription);
        project.setStatus("PLANNING");
        project.setStartedAt(LocalDate.now());
        projectMapper.insert(project);
        Long projectId = project.getId();

        // ---------- 5. 批量创建任务（分配给团队所有成员） ----------
        if (dto.getTasks() != null && !dto.getTasks().isEmpty()) {
            List<TeamMember> allMembers = teamMemberMapper.selectList(
                    new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId));
            int memberCount = allMembers.size();
            int taskIndex = 0;

            for (ConvertToProjectDTO.TaskAssignment taskAssignment : dto.getTasks()) {
                Task task = new Task();
                task.setProjectId(projectId);
                task.setCreatorId(currentUserId);
                task.setTitle(taskAssignment.getTitle());
                task.setDescription(taskAssignment.getDescription());
                task.setPriority(taskAssignment.getPriority() != null ? taskAssignment.getPriority() : "MEDIUM");
                task.setStatus("TODO");

                // 解析截止日期
                if (taskAssignment.getDeadline() != null && !taskAssignment.getDeadline().isEmpty()) {
                    try {
                        task.setDeadline(LocalDateTime.parse(taskAssignment.getDeadline(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        // 尝试只有日期的格式
                        try {
                            task.setDeadline(LocalDateTime.parse(taskAssignment.getDeadline() + " 23:59:59",
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        } catch (Exception ignored) {
                            log.warn("Failed to parse deadline: {}", taskAssignment.getDeadline());
                        }
                    }
                }

                // 轮转分配给团队成员
                if (!allMembers.isEmpty()) {
                    TeamMember assignee = allMembers.get(taskIndex % memberCount);
                    task.setCreatorId(assignee.getUserId()); // 利用creatorId记录负责人
                }

                taskMapper.insert(task);
                taskIndex++;
            }
            log.info("Created {} tasks for project {} from recruit {}", dto.getTasks().size(), projectId, recruitId);
        }

        // ---------- 6. 更新招募表关联 ----------
        recruit.setProjectId(projectId);
        recruit.setTeamId(teamId);
        recruit.setContinueRecruit(dto.getContinueRecruit());

        // 根据是否继续招募设置招募状态
        if (dto.getContinueRecruit() == 0) {
            // 不继续招募 -> 停止招募
            recruit.setStatus("CLOSED");
        } else {
            // 继续招募 -> 如果已满员则设为FULL，否则设为OPEN
            if (recruit.getMaxMembers() != null && recruit.getCurrentMembers() != null
                    && recruit.getCurrentMembers() >= recruit.getMaxMembers()) {
                recruit.setStatus("FULL");
            } else {
                recruit.setStatus("OPEN");
            }
        }

        recruitMapper.updateById(recruit);

        log.info("Recruit {} converted to project {} and team {}, continueRecruit={}",
                recruitId, projectId, teamId, dto.getContinueRecruit());

        return projectId;
    }

    @Override
    @Transactional
    public void toggleContinueRecruit(Long recruitId, Integer continueRecruit) {
        Recruit recruit = recruitMapper.selectById(recruitId);
        if (recruit == null) {
            throw BusinessException.notFound("招募信息不存在");
        }

        // 权限检查：只有发布者或管理员可以切换
        if (!recruit.getPublisherId().equals(UserContext.getCurrentUserId()) && !isAdmin()) {
            throw BusinessException.forbidden("只有招募发布者或管理员可以切换招募状态");
        }

        // 检查是否已经转为项目
        if (recruit.getProjectId() == null && recruit.getTeamId() == null) {
            throw BusinessException.badRequest("该招募尚未转为项目，无法切换继续招募状态");
        }

        if (continueRecruit != 0 && continueRecruit != 1) {
            throw BusinessException.badRequest("无效的招募状态: " + continueRecruit + "，请使用 0（停止）或 1（继续）");
        }

        recruit.setContinueRecruit(continueRecruit);

        if (continueRecruit == 0) {
            // 停止招募
            recruit.setStatus("CLOSED");
        } else {
            // 继续招募：检查是否已满员
            if (recruit.getMaxMembers() != null && recruit.getCurrentMembers() != null
                    && recruit.getCurrentMembers() >= recruit.getMaxMembers()) {
                recruit.setStatus("FULL");
            } else {
                recruit.setStatus("OPEN");
            }
        }

        recruitMapper.updateById(recruit);

        log.info("Recruit {} continueRecruit toggled to {} by user {}",
                recruitId, continueRecruit, UserContext.getCurrentUserId());
    }
}
