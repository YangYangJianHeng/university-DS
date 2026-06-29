package com.platform.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.module.achievement.entity.Achievement;
import com.platform.module.achievement.mapper.AchievementMapper;
import com.platform.module.badge.entity.Badge;
import com.platform.module.badge.mapper.BadgeMapper;
import com.platform.module.admin.entity.AuditLog;
import com.platform.module.admin.mapper.AuditLogMapper;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.competition.entity.Competition;
import com.platform.module.competition.mapper.CompetitionMapper;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.recruit.entity.Application;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.ApplicationMapper;
import com.platform.module.recruit.mapper.RecruitMapper;
import com.platform.module.teacher.entity.Teacher;
import com.platform.module.teacher.mapper.TeacherMapper;
import com.platform.module.team.entity.Team;
import com.platform.module.team.entity.TeamMember;
import com.platform.module.team.mapper.TeamMapper;
import com.platform.module.team.mapper.TeamMemberMapper;
import com.platform.module.user.entity.User;
import com.platform.module.user.entity.UserSkill;
import com.platform.module.user.mapper.UserMapper;
import com.platform.module.user.mapper.UserSkillMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final UserSkillMapper userSkillMapper;
    private final RecruitMapper recruitMapper;
    private final ApplicationMapper applicationMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final NotificationMapper notificationMapper;
    private final TeacherMapper teacherMapper;
    private final AchievementMapper achievementMapper;
    private final AuditLogMapper auditLogMapper;
    private final CompetitionMapper competitionMapper;
    private final BadgeMapper badgeMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, UserSkillMapper userSkillMapper,
                           RecruitMapper recruitMapper, ApplicationMapper applicationMapper,
                           TeamMapper teamMapper, TeamMemberMapper teamMemberMapper,
                           ProjectMapper projectMapper, TaskMapper taskMapper,
                           NotificationMapper notificationMapper,
                           TeacherMapper teacherMapper,
                           AchievementMapper achievementMapper,
                           AuditLogMapper auditLogMapper,
                           CompetitionMapper competitionMapper,
                           BadgeMapper badgeMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userSkillMapper = userSkillMapper;
        this.recruitMapper = recruitMapper;
        this.applicationMapper = applicationMapper;
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.notificationMapper = notificationMapper;
        this.teacherMapper = teacherMapper;
        this.achievementMapper = achievementMapper;
        this.auditLogMapper = auditLogMapper;
        this.competitionMapper = competitionMapper;
        this.badgeMapper = badgeMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // ---- 1. 初始化用户数据 ----
        initUsers();

        // ---- 2. 初始化技能数据 ----
        initSkills();

        // ---- 3. 初始化招募数据 ----
        initRecruits();

        // ---- 4. 初始化团队数据 ----
        initTeams();

        // ---- 5. 初始化项目数据 ----
        initProjects();

        // ---- 6. 初始化通知数据 ----
        initNotifications();

        // ---- 7. 初始化导师数据 ----
        initTeachers();

        // ---- 8. 初始化成果数据 ----
        initAchievements();

        // ---- 9. 初始化审核日志 ----
        initAuditLogs();

        // ---- 10. 初始化竞赛数据 ----
        initCompetitions();

        // ---- 11. 初始化徽章数据 ----
        initBadges();
    }

    private void initUsers() {
        // 不再自动创建任何假用户。
        // 系统启动时仅确保存在一个管理员账号（用于首次部署登录），其余所有用户均通过注册入口真实创建。
        User admin = findUserByPhone("13800000000");
        if (admin == null) {
            admin = createUser("13800000000", "ADMIN001", "管理员", "admin123", null, null, "ADMIN", 1);
            System.out.println("Admin user created (id=" + admin.getId() + ")");
        }
    }

    private void initSkills() {
        if (userSkillMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态查找所有 USER 角色且尚未添加技能的用户，添加默认技能
        List<User> normalUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "USER"));
        if (normalUsers.isEmpty()) {
            System.out.println("No USER role users found, skip skill init.");
            return;
        }

        String[][] defaultSkillPairs = {
            {"Java", "Python"}, {"前端开发", "数据库"}, {"数据分析", "机器学习"},
            {"C++", "算法设计"}, {"项目管理", "团队协作"}, {"UI设计", "原型设计"},
        };

        int added = 0;
        for (int i = 0; i < normalUsers.size(); i++) {
            User user = normalUsers.get(i);
            Long existing = userSkillMapper.selectCount(
                    new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, user.getId()));
            if (existing > 0) continue;

            String[] pair = defaultSkillPairs[i % defaultSkillPairs.length];
            addSkill(user.getId(), pair[0], "INTERMEDIATE");
            addSkill(user.getId(), pair[1], "BEGINNER");
            added++;
        }

        System.out.println("Seed skills created for " + added + " users.");
    }

    private void initRecruits() {
        if (recruitMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态从数据库查找所有USER角色用户
        List<User> userUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "USER"));
        if (userUsers.size() < 2) {
            System.out.println("Not enough USER role users for recruit init (need >=2), skip.");
            return;
        }

        // 招募模板数据（只有内容，不包含任何用户ID）
        String[][] recruitTemplates = {
            {"互联网+创新创业大赛项目组队", "我们正在准备2026年互联网+大赛，项目主题是AI驱动的教育平台。需要志同道合的队友一起完成项目开发和商业计划书撰写。",
             "INNOVATION", "[\"Java\",\"Python\",\"前端开发\",\"商业分析\"]", "6", "2026-07-15T23:59"},
            {"全国大学生数学建模竞赛队友招募", "寻找数模队友！本人有两次参赛经验，擅长建模和论文写作。希望找到擅长编程和数据分析的同学组队。",
             "ACADEMIC", "[\"数学建模\",\"Python\",\"数据分析\",\"LaTeX\"]", "3", "2026-08-20T23:59"},
            {"挑战杯-智慧校园项目组队", "基于物联网的智慧教室管理系统，已有一个原型。需要Web全栈、嵌入式开发和UI设计的同学加入。已有指导老师支持。",
             "ACADEMIC", "[\"Java\",\"Spring Boot\",\"Vue.js\",\"嵌入式开发\",\"UI设计\"]", "5", "2026-07-30T23:59"},
            {"AI辅助医学影像分析科研项目", "与医学院合作的交叉学科项目，用深度学习技术辅助CT影像分析。需要有机器学习和Python基础的同学，医学知识由合作方提供。",
             "RESEARCH", "[\"Python\",\"PyTorch\",\"机器学习\",\"图像处理\"]", "4", "2026-08-31T23:59"},
        };

        int recruitCount = Math.min(userUsers.size(), recruitTemplates.length);
        java.util.List<Recruit> createdRecruits = new java.util.ArrayList<>();
        for (int i = 0; i < recruitCount; i++) {
            User publisher = userUsers.get(i);
            String[] t = recruitTemplates[i];
            Recruit r = new Recruit();
            r.setPublisherId(publisher.getId());
            r.setTitle(t[0]);
            r.setDescription(t[1]);
            r.setProjectType(t[2]);
            r.setRequiredSkills(t[3]);
            r.setMaxMembers(Integer.parseInt(t[4]));
            r.setCurrentMembers(1);
            r.setDeadline(LocalDateTime.parse(t[5]));
            r.setStatus("OPEN");
            r.setViewCount(0);
            recruitMapper.insert(r);
            createdRecruits.add(r);
        }

        // 为招募创建申请：剩余用户申请第一个招募
        if (!createdRecruits.isEmpty()) {
            Recruit firstRecruit = createdRecruits.get(0);
            for (int i = recruitCount; i < userUsers.size(); i++) {
                Application app = new Application();
                app.setRecruitId(firstRecruit.getId());
                app.setApplicantId(userUsers.get(i).getId());
                app.setStatus("PENDING");
                applicationMapper.insert(app);
            }
            // 如果恰好有被招募用户，将其中一条设为ACCEPTED表示已接受
            List<Application> apps = applicationMapper.selectList(
                    new LambdaQueryWrapper<Application>().eq(Application::getRecruitId, firstRecruit.getId()));
            if (!apps.isEmpty()) {
                Application accepted = apps.get(0);
                accepted.setStatus("ACCEPTED");
                applicationMapper.updateById(accepted);
            }
        }

        System.out.println("Seed recruits created: " + createdRecruits.size());
    }

    private void initTeams() {
        if (teamMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态从数据库查第一个招募记录和第一个USER用户作为队长
        Recruit firstRecruit = recruitMapper.selectOne(
                new LambdaQueryWrapper<Recruit>().last("LIMIT 1"));
        if (firstRecruit == null) {
            System.out.println("No recruits found, skip team init.");
            return;
        }

        // 查找该招募发布者作为队长
        User leader = userMapper.selectById(firstRecruit.getPublisherId());
        if (leader == null) return;

        // 查找该招募下一条已接受的申请者作为成员
        Application acceptedApp = applicationMapper.selectOne(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getRecruitId, firstRecruit.getId())
                        .eq(Application::getStatus, "ACCEPTED"));
        User member = null;
        if (acceptedApp != null) {
            member = userMapper.selectById(acceptedApp.getApplicantId());
        }

        // 如果招募更新了currentMembers但实际没有已接受申请，就找第一条PENDING
        if (member == null) {
            Application pendingApp = applicationMapper.selectOne(
                    new LambdaQueryWrapper<Application>()
                            .eq(Application::getRecruitId, firstRecruit.getId())
                            .eq(Application::getStatus, "PENDING"));
            if (pendingApp != null) {
                member = userMapper.selectById(pendingApp.getApplicantId());
            }
        }

        // 团队名称由招募标题派生
        String teamName = firstRecruit.getTitle().contains("互联网+") ? "AI教育先锋队"
                : firstRecruit.getTitle().contains("数模") ? "数学建模精英队"
                : firstRecruit.getTitle().contains("挑战杯") ? "智慧校园创新队"
                : "科研探索队";

        Team team = new Team();
        team.setName(teamName);
        team.setLeaderId(leader.getId());
        team.setRecruitId(firstRecruit.getId());
        team.setStatus("ACTIVE");
        teamMapper.insert(team);

        // 队长作为成员
        TeamMember tmLeader = new TeamMember();
        tmLeader.setTeamId(team.getId());
        tmLeader.setUserId(leader.getId());
        tmLeader.setRole("LEADER");
        teamMemberMapper.insert(tmLeader);

        // 如果有成员，加入团队
        if (member != null) {
            TeamMember tmMember = new TeamMember();
            tmMember.setTeamId(team.getId());
            tmMember.setUserId(member.getId());
            tmMember.setRole("MEMBER");
            teamMemberMapper.insert(tmMember);

            firstRecruit.setCurrentMembers(2);
        } else {
            firstRecruit.setCurrentMembers(1);
        }
        recruitMapper.updateById(firstRecruit);

        System.out.println("Seed team created: " + teamName);
    }

    private void initProjects() {
        if (projectMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态从数据库查第一个团队和该团队的LEADER成员
        Team firstTeam = teamMapper.selectOne(
                new LambdaQueryWrapper<Team>().last("LIMIT 1"));
        if (firstTeam == null) {
            System.out.println("No teams found, skip project init.");
            return;
        }

        // 查找该团队的LEADER作为项目负责人
        TeamMember leaderMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, firstTeam.getId())
                        .eq(TeamMember::getRole, "LEADER"));
        if (leaderMember == null) {
            System.out.println("No team leader found, skip project init.");
            return;
        }
        User leader = userMapper.selectById(leaderMember.getUserId());
        if (leader == null) return;

        // 项目名称由团队名称派生
        String projectName = "基于AI的个性化学习推荐平台";

        Project project = new Project();
        project.setTeamId(firstTeam.getId());
        project.setLeaderId(leader.getId());
        project.setName(projectName);
        project.setDescription("利用机器学习算法分析学生学习行为，提供个性化的学习资源推荐。项目目标：完成核心推荐算法、Web端和移动端适配。");
        project.setStatus("ONGOING");
        project.setStartedAt(LocalDate.of(2026, 6, 1));
        project.setEndedAt(LocalDate.of(2026, 10, 30));
        projectMapper.insert(project);

        // 项目任务模板（只包含内容，不包含任何外键）
        String[][] taskTemplates = {
            {"需求分析与系统设计", "完成项目需求文档、系统架构设计和数据库设计", "Java,Spring Boot,系统设计", "HIGH", "DONE", "2026-06-15T23:59"},
            {"用户管理模块开发", "实现用户注册、登录、个人信息管理等功能", "Java,Spring Boot,Vue.js", "HIGH", "IN_PROGRESS", "2026-07-10T23:59"},
            {"学习行为数据采集", "设计并实现用户学习行为的数据采集和存储模块", "Python,数据分析,MySQL", "MEDIUM", "TODO", "2026-07-20T23:59"},
            {"推荐算法原型开发", "基于协同过滤和知识图谱的混合推荐算法实现", "Python,PyTorch,机器学习", "HIGH", "TODO", "2026-08-15T23:59"},
            {"前端页面开发", "实现Web端主要页面，包括首页、学习推荐、个人中心等", "Vue.js,TypeScript,前端开发", "MEDIUM", "TODO", "2026-08-30T23:59"},
        };

        for (String[] tt : taskTemplates) {
            Task task = new Task();
            task.setProjectId(project.getId());
            task.setCreatorId(leader.getId());
            task.setTitle(tt[0]);
            task.setDescription(tt[1]);
            task.setRequiredSkills(tt[2]);
            task.setPriority(tt[3]);
            task.setStatus(tt[4]);
            task.setDeadline(LocalDateTime.parse(tt[5]));
            taskMapper.insert(task);
        }

        System.out.println("Seed project created: " + projectName);
    }

    private void initNotifications() {
        if (notificationMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态查询数据库中所有用户，为每个人创建欢迎通知
        List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<>());
        if (allUsers.isEmpty()) {
            System.out.println("No users found, skip notification init.");
            return;
        }

        int sent = 0;
        for (User user : allUsers) {
            // 系统公告 — 仅给管理员发
            if ("ADMIN".equals(user.getRole())) {
                Notification sysNotice = new Notification();
                sysNotice.setUserId(user.getId());
                sysNotice.setTitle("系统上线通知");
                sysNotice.setContent("大学生竞赛/科研项目组队匹配与过程管理平台已正式上线，欢迎使用！");
                sysNotice.setType("SYSTEM");
                sysNotice.setIsRead(0);
                notificationMapper.insert(sysNotice);
                sent++;
                continue;
            }

            // 给所有普通用户/导师发欢迎通知
            Notification welcome = new Notification();
            welcome.setUserId(user.getId());
            welcome.setTitle("欢迎使用平台");
            welcome.setContent("欢迎加入！完善个人资料和技能标签，可以获得更精准的组队推荐。");
            welcome.setType("SYSTEM");
            welcome.setIsRead(0);
            notificationMapper.insert(welcome);
            sent++;
        }

        // 如果存在申请记录，为被接受的申请人发通知
        List<Application> acceptedApps = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>().eq(Application::getStatus, "ACCEPTED"));
        for (Application app : acceptedApps) {
            User applicant = userMapper.selectById(app.getApplicantId());
            if (applicant != null) {
                Notification appNotice = new Notification();
                appNotice.setUserId(applicant.getId());
                appNotice.setTitle("申请已通过");
                appNotice.setContent("你的组队申请已被接受，请尽快联系团队负责人。");
                appNotice.setType("APPLICATION");
                appNotice.setIsRead(0);
                notificationMapper.insert(appNotice);
                sent++;
            }
        }

        System.out.println("Seed notifications created: " + sent);
    }

    private void initTeachers() {
        if (teacherMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态查找数据库中所有 TEACHER 角色的用户
        List<User> teacherUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "TEACHER"));
        if (teacherUsers.isEmpty()) {
            System.out.println("No TEACHER role users found, skip teacher init.");
            return;
        }

        // 导师模板数据：每个导师有独特的职称、院系、研究方向、评分（按顺序匹配）
        // 格式: {职称, 院系, 研究方向JSON数组字符串, 最大团队数, 当前团队数, 评分}
        String[][] teacherTemplates = {
            {"教授",   "计算机科学与技术学院", "[\"人工智能\",\"机器学习\",\"深度学习\"]",           "6", "2", "4.8"},
            {"副教授", "人工智能学院",         "[\"自然语言处理\",\"大模型\",\"知识图谱\"]",           "5", "1", "4.5"},
            {"讲师",   "电子信息工程学院",     "[\"嵌入式系统\",\"物联网\",\"信号处理\"]",             "4", "0", "4.2"},
            {"教授",   "数学与统计学院",       "[\"应用数学\",\"数值计算\",\"优化算法\"]",             "5", "3", "4.9"},
            {"副教授", "软件工程学院",         "[\"软件工程\",\"DevOps\",\"云计算\"]",                 "5", "1", "4.3"},
            {"讲师",   "数据科学学院",         "[\"数据挖掘\",\"大数据分析\",\"数据可视化\"]",          "4", "0", "4.0"},
        };

        int created = 0;
        for (int i = 0; i < teacherUsers.size(); i++) {
            User user = teacherUsers.get(i);
            String[] tpl = teacherTemplates[i % teacherTemplates.length];

            Teacher t = new Teacher();
            t.setUserId(user.getId());
            t.setDepartment(tpl[1]);
            t.setTitle(tpl[0]);
            t.setResearchFields(tpl[2]);
            t.setMaxTeams(Integer.parseInt(tpl[3]));
            t.setCurrentTeams(Integer.parseInt(tpl[4]));
            t.setIsVerified(1);
            t.setRating(new java.math.BigDecimal(tpl[5]));
            teacherMapper.insert(t);
            created++;
        }

        System.out.println("Seed teachers created: " + created + " (from TEACHER role users)");
    }

    private void initAchievements() {
        if (achievementMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态从数据库查第一个团队
        Team firstTeam = teamMapper.selectOne(
                new LambdaQueryWrapper<Team>().last("LIMIT 1"));
        if (firstTeam == null) {
            System.out.println("No teams found, skip achievement init.");
            return;
        }

        // 成果模板数据
        String[][] achievementTemplates = {
            {"2025年全国大学生AI创新应用大赛一等奖", "AWARD", "NATIONAL",
             "团队开发的「智能学习助手」在全国大学生AI创新应用大赛中荣获一等奖。项目基于大语言模型和知识图谱技术，为学生提供个性化学习辅导。", "1"},
            {"基于深度强化学习的自适应学习路径推荐方法", "PAPER", "NATIONAL",
             "发表于《计算机教育》2025年第3期。提出了一种结合知识追踪和深度强化学习的自适应学习路径推荐方法。", "0"},
            {"智能学习推荐平台V1.0", "SOFTWARE", "NATIONAL",
             "登记号：2025SR1234567。基于协同过滤和知识图谱的混合推荐算法，实现了学习资源的精准推荐。", "0"},
            {"一种基于用户画像的学习资源个性化推荐方法", "PATENT", "PROVINCIAL",
             "发明专利，申请号：CN202510012345.6。通过构建多维用户画像，实现学习资源的智能推荐。", "0"},
        };

        for (int i = 0; i < achievementTemplates.length; i++) {
            String[] at = achievementTemplates[i];
            Achievement a = new Achievement();
            a.setTeamId(firstTeam.getId());
            a.setTitle(at[0]);
            a.setType(at[1]);
            a.setAwardLevel(at[2]);
            a.setDescription(at[3]);
            a.setIsFeatured(Integer.parseInt(at[4]));
            a.setIsPublic(1);
            a.setViewCount(50 + i * 30);
            a.setLikeCount(10 + i * 5);
            achievementMapper.insert(a);
        }

        System.out.println("Seed achievements created: " + achievementTemplates.length + " items");
    }

    private void initAuditLogs() {
        if (auditLogMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 动态从数据库查找ADMIN角色用户
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        if (admin == null) {
            System.out.println("No ADMIN user found, skip audit log init.");
            return;
        }

        // 动态从数据库查找实际存在的招募记录
        List<Recruit> recruits = recruitMapper.selectList(new LambdaQueryWrapper<>());
        if (recruits.isEmpty()) {
            System.out.println("No recruits found, skip audit log init.");
            return;
        }

        // 为每个招募创建一条审核日志（最多3条）
        String[][] auditData = {
            {"APPROVE", "符合平台规范，项目内容完整", "PENDING", "APPROVED"},
            {"REJECT", "项目描述不够详细，需要补充具体方案", "PENDING", "REJECTED"},
            {"APPROVE", "项目创新性好，符合大赛要求", "PENDING", "APPROVED"},
        };

        int count = 0;
        for (int i = 0; i < Math.min(recruits.size(), auditData.length); i++) {
            Recruit r = recruits.get(i);
            String[] ad = auditData[i];
            AuditLog log = new AuditLog();
            log.setTargetType("RECRUIT");
            log.setTargetId(r.getId());
            log.setOperatorId(admin.getId());
            log.setAction(ad[0]);
            log.setReason(ad[1]);
            log.setBeforeStatus(ad[2]);
            log.setAfterStatus(ad[3]);
            auditLogMapper.insert(log);
            count++;
        }

        System.out.println("Seed audit logs created: " + count + " records");
    }

    private void initCompetitions() {
        if (competitionMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // ---- 国家级竞赛 ----
        addCompetition("中国国际\"互联网+\"大学生创新创业大赛", "NATIONAL", "INNOVATION",
                "教育部", "https://cy.ncss.cn",
                "2026年中国国际\"互联网+\"大学生创新创业大赛由教育部等多部委联合主办，围绕\"更中国、更国际、更教育、更全面、更创新、更协同\"的目标，以赛促教、以赛促学、以赛促创，旨在深化高校创新创业教育改革，激发大学生创造力，培养造就\"大众创业、万众创新\"的生力军。",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 30),
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 11, 30),
                "REGISTERING");

        addCompetition("\"挑战杯\"全国大学生课外学术科技作品竞赛", "NATIONAL", "ACADEMIC",
                "共青团中央、中国科协、教育部", "https://www.tiaozhanbei.net",
                "\"挑战杯\"竞赛被誉为中国大学生科技的\"奥林匹克\"盛会，每两年举办一届。竞赛旨在引导和激励学生实事求是、刻苦钻研、勇于创新，培养学生创新精神和实践能力，并在此基础上促进高校学生课外学术科技活动的蓬勃开展。",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 30),
                "ENDED");

        addCompetition("全国大学生数学建模竞赛（CUMCM）", "NATIONAL", "ACADEMIC",
                "中国工业与应用数学学会", "http://www.mcm.edu.cn",
                "全国大学生数学建模竞赛创办于1992年，每年一届，是首批列入\"高校学科竞赛排行榜\"的19项竞赛之一。竞赛题目一般来源于科学与工程技术、人文与社会科学等领域经过适当简化加工的实际问题。",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 13),
                "UPCOMING");

        addCompetition("ACM-ICPC国际大学生程序设计竞赛（亚洲区域赛）", "NATIONAL", "ACADEMIC",
                "ACM（国际计算机学会）", "https://icpc.global",
                "ACM-ICPC是世界上公认的规模最大、水平最高的国际大学生程序设计竞赛，旨在展示大学生创新能力、团队精神和在压力下编写程序、分析和解决问题能力。每年吸引来自全球100多个国家的3000多所高校参赛。",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
                "UPCOMING");

        addCompetition("蓝桥杯全国软件和信息技术专业人才大赛", "NATIONAL", "ACADEMIC",
                "工业和信息化部人才交流中心", "https://dasai.lanqiao.cn",
                "蓝桥杯大赛是由工业和信息化部人才交流中心主办的全国性IT学科竞赛。大赛覆盖Java、C/C++、Python、Web、嵌入式、单片机等多个技术方向，是国内规模最大的IT类专业赛事之一。",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 6, 30),
                "ONGOING");

        addCompetition("全国大学生电子设计竞赛", "NATIONAL", "ACADEMIC",
                "教育部、工业和信息化部", "https://www.nuedc-training.com.cn",
                "全国大学生电子设计竞赛是教育部和工信部共同发起的大学生学科竞赛，是面向大学生的群众性科技活动。竞赛内容既有理论设计，又有实际制作，全面检验参赛学生的理论基础和实践创新能力。",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4),
                "UPCOMING");

        addCompetition("全国大学生英语竞赛（NECCS）", "NATIONAL", "ACADEMIC",
                "高等学校大学外语教学指导委员会", "https://www.chinaneccs.cn",
                "全国大学生英语竞赛是全国性大学英语学科竞赛，旨在促进大学英语教学改革，全面提高大学生英语综合运用能力。竞赛分为A、B、C、D四个类别，覆盖研究生、英语专业、非英语专业本科生等各类学生群体。",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 20),
                "ONGOING");

        addCompetition("全国大学生机器人大赛（RoboMaster）", "NATIONAL", "ACADEMIC",
                "共青团中央、大疆创新", "https://www.robomaster.com",
                "RoboMaster机甲大师赛是由大疆创新发起并承办的机器人竞技比赛。大赛充分融合\"机器视觉\"\"嵌入式系统设计\"\"机械控制\"\"惯性导航\"\"人机交互\"等众多机器人相关技术学科，同时创新性地将电竞呈现方式与机器人竞技相结合。",
                LocalDate.of(2025, 11, 1), LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30),
                "ENDED");

        addCompetition("全国大学生广告艺术大赛（大广赛）", "NATIONAL", "INNOVATION",
                "教育部高等教育司指导", "http://www.sun-ada.net",
                "全国大学生广告艺术大赛是迄今为止全国规模大、覆盖高等院校广、参与师生人数多的国家级大学生赛事。参赛作品分为平面类、视频类、动画类、互动类、广播类、策划案类、文案类、公益类等八大类。",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 15), LocalDate.of(2026, 10, 30),
                "REGISTERING");

        addCompetition("全国大学生化学实验创新设计大赛", "NATIONAL", "ACADEMIC",
                "教育部高等学校化学类专业教学指导委员会", "https://cid.nju.edu.cn",
                "全国大学生化学实验创新设计大赛旨在夯实大学生的化学基础知识、基本理论和基本技能，强化大学生发现问题、分析问题和解决问题的能力，培养大学生的创新意识、创新精神和创新能力。",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 11, 15),
                "REGISTERING");

        addCompetition("全国大学生物理实验竞赛（创新赛）", "NATIONAL", "ACADEMIC",
                "教育部高等学校大学物理课程教学指导委员会", "https://wlsycx.moocollege.com",
                "竞赛涵盖命题类创新作品、自选课题类创新作品和大学生物理实验讲课比赛三个类别，旨在激发大学生对物理实验的兴趣与潜能，培养学生的创新能力、实践能力和团队协作意识。",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                "UPCOMING");

        addCompetition("全国大学生结构设计竞赛", "NATIONAL", "ACADEMIC",
                "全国大学生结构设计竞赛委员会", "https://www.structure-contest.com",
                "全国大学生结构设计竞赛是由教育部、财政部首次联合批准发文的全国性学科竞赛项目。竞赛遵循\"3C\"精神（Creation, Cooperation, Communication），培养大学生创新意识、合作精神和工程实践能力。",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 31),
                "UPCOMING");

        // ---- 省级竞赛 ----
        addCompetition("省级大学生创新创业训练计划项目", "PROVINCIAL", "INNOVATION",
                "省教育厅", "",
                "省级大学生创新创业训练计划项目包括创新训练项目、创业训练项目和创业实践项目三类，旨在促进高校转变教育思想观念，改革人才培养模式，强化创新创业能力训练，培养适应创新型国家建设需要的高水平创新人才。",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30),
                "REGISTERING");

        addCompetition("省级高等数学竞赛", "PROVINCIAL", "ACADEMIC",
                "省数学学会", "",
                "省级高等数学竞赛旨在激发大学生学习数学的兴趣，培养学生分析问题和解决问题的能力，发现和选拔数学创新人才，为青年学子提供一个展示数学基础知识和思维能力的舞台。",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 6),
                "ONGOING");

        // ---- 校级竞赛 ----
        addCompetition("校级程序设计竞赛", "SCHOOL", "ACADEMIC",
                "校团委 / 计算机学院", "",
                "校级程序设计竞赛面向全校本科生，采用ACM-ICPC赛制，考察选手的算法设计、程序实现和团队协作能力。优秀选手将代表学校参加省级和国家级比赛。",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10),
                "REGISTERING");

        System.out.println("Seed competitions created: 15 entries (12 national + 2 provincial + 1 school)");
    }

    private void initBadges() {
        if (badgeMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        addBadge("新手入门", "完成第一个任务或加入第一个团队", "badge_newbie", "NEWBIE", 0, "入门", 1);
        addBadge("任务达人", "累计完成5个以上任务", "badge_task", "COMPLETED_TASKS", 4, "任务", 2);
        addBadge("任务终结者", "累计完成20个以上任务", "badge_task_master", "COMPLETED_TASKS", 19, "任务", 3);
        addBadge("团队之星", "参与3个以上团队", "badge_team", "TEAM_COUNT", 3, "团队", 4);
        addBadge("社交达人", "参与5个以上团队", "badge_social", "TEAM_COUNT", 5, "团队", 5);
        addBadge("技能先锋", "掌握5个以上技能", "badge_skill", "SKILL_COUNT", 5, "技能", 6);
        addBadge("技能专精", "至少一项技能达到大师等级", "badge_skill_master", "MASTER_SKILL", 0, "技能", 7);
        addBadge("成果贡献者", "在项目中提交过成果", "badge_achievement", "ACHIEVEMENT_COUNT", 0, "成果", 8);
        addBadge("资深贡献者", "提交3个以上成果", "badge_achievement_senior", "ACHIEVEMENT_COUNT", 2, "成果", 9);
        addBadge("竞赛达人", "获得竞赛类成果", "badge_competition", "COMPETITION_COUNT", 0, "竞赛", 10);
        addBadge("荣誉之星", "获得过带奖项等级的成果", "badge_honor", "HAS_HONOR", 0, "竞赛", 11);
        addBadge("招新能手", "发布过团队招募信息", "badge_recruit", "RECRUIT_COUNT", 0, "社交", 12);
        addBadge("项目老手", "参与5个以上项目", "badge_project", "PROJECT_COUNT", 5, "项目", 13);
        addBadge("经验丰富", "拥有3条以上项目经历", "badge_experience", "EXPERIENCE_COUNT", 3, "项目", 14);
        addBadge("全能选手", "同时满足多项平台参与条件", "badge_all_rounder", "ALL_ROUNDER", 0, "综合", 15);

        System.out.println("Seed badges created: 15 entries");
    }

    private void addBadge(String name, String description, String icon, String conditionType, int conditionValue, String category, int sortOrder) {
        Badge badge = new Badge();
        badge.setName(name);
        badge.setDescription(description);
        badge.setIcon(icon);
        badge.setConditionType(conditionType);
        badge.setConditionValue(conditionValue);
        badge.setCategory(category);
        badge.setSortOrder(sortOrder);
        badgeMapper.insert(badge);
    }

    // ---- 辅助方法 ----

    private User findUserByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    private User createUser(String phone, String studentId, String username,
                           String rawPassword, String major, String grade,
                           String role, Integer status) {
        User user = new User();
        user.setPhone(phone);
        user.setStudentId(studentId);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setMajor(major);
        user.setGrade(grade);
        user.setRole(role);
        user.setStatus(status);
        userMapper.insert(user);
        return user;
    }

    private void addSkill(Long userId, String skillName, String level) {
        UserSkill skill = new UserSkill();
        skill.setUserId(userId);
        skill.setSkillName(skillName);
        skill.setLevel(level);
        userSkillMapper.insert(skill);
    }

    private void addCompetition(String name, String level, String category,
                                String organizer, String officialUrl, String description,
                                LocalDate registerStart, LocalDate registerEnd,
                                LocalDate eventStart, LocalDate eventEnd, String status) {
        Competition c = new Competition();
        c.setName(name);
        c.setLevel(level);
        c.setCategory(category);
        c.setOrganizer(organizer);
        c.setOfficialUrl(officialUrl);
        c.setDescription(description);
        c.setRegisterStart(registerStart);
        c.setRegisterEnd(registerEnd);
        c.setEventStart(eventStart);
        c.setEventEnd(eventEnd);
        c.setStatus(status);
        c.setViewCount(0);
        c.setCollectCount(0);
        competitionMapper.insert(c);
    }
}
