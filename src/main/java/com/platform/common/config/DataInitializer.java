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
        // 管理员
        User admin = findUserByPhone("13800000000");
        if (admin == null) {
            admin = createUser("13800000000", "ADMIN001", "管理员", "admin123", null, null, "ADMIN", 1);
            System.out.println("Admin user created: 13800000000 / admin123");
        }

        // 演示用户
        User demo = findUserByPhone("13800000001");
        if (demo == null) {
            demo = createUser("13800000001", "2024001", "张三", "123456",
                    "计算机科学与技术", "2023级", "USER", 1);
            System.out.println("Demo user created: 13800000001 / 123456");
        }

        // ---- 学生用户 ----
        if (findUserByPhone("13800000002") == null) {
            createUser("13800000002", "2024002", "李四", "123456",
                    "计算机科学与技术", "2023级", "USER", 1);
        }
        if (findUserByPhone("13800000003") == null) {
            createUser("13800000003", "2024003", "王五", "123456",
                    "软件工程", "2022级", "USER", 1);
        }
        if (findUserByPhone("13800000004") == null) {
            createUser("13800000004", "2024004", "赵六", "123456",
                    "人工智能", "2023级", "USER", 1);
        }
        if (findUserByPhone("13800000005") == null) {
            createUser("13800000005", "2024005", "陈七", "123456",
                    "数据科学与大数据技术", "2024级", "USER", 1);
        }
        if (findUserByPhone("13800000006") == null) {
            createUser("13800000006", "2024006", "刘八", "123456",
                    "电子信息工程", "2022级", "USER", 1);
        }
        if (findUserByPhone("13800000007") == null) {
            createUser("13800000007", "2024007", "孙九", "123456",
                    "数学与应用数学", "2023级", "USER", 1);
        }
        if (findUserByPhone("13800000008") == null) {
            createUser("13800000008", "2024008", "周十", "123456",
                    "物联网工程", "2024级", "USER", 1);
        }

        // ---- 教师用户 ----
        if (findUserByPhone("13800000010") == null) {
            createUser("13800000010", "T2024001", "王教授", "123456",
                    "计算机科学与技术", null, "TEACHER", 1);
        }
        if (findUserByPhone("13800000011") == null) {
            createUser("13800000011", "T2024002", "李教授", "123456",
                    "人工智能", null, "TEACHER", 1);
        }
        if (findUserByPhone("13800000012") == null) {
            createUser("13800000012", "T2024003", "张教授", "123456",
                    "电子信息工程", null, "TEACHER", 1);
        }
    }

    private void initSkills() {
        // 已存在的技能不再重复插入
        if (userSkillMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 李四 - 计算机科学与技术
        addSkill(3L, "Java", "ADVANCED");
        addSkill(3L, "Spring Boot", "INTERMEDIATE");
        addSkill(3L, "MySQL", "INTERMEDIATE");
        addSkill(3L, "Vue.js", "BEGINNER");

        // 王五 - 软件工程
        addSkill(4L, "Python", "ADVANCED");
        addSkill(4L, "Django", "INTERMEDIATE");
        addSkill(4L, "React", "INTERMEDIATE");
        addSkill(4L, "Docker", "INTERMEDIATE");

        // 赵六 - 人工智能
        addSkill(5L, "Python", "ADVANCED");
        addSkill(5L, "PyTorch", "INTERMEDIATE");
        addSkill(5L, "TensorFlow", "INTERMEDIATE");
        addSkill(5L, "Machine Learning", "ADVANCED");

        // 陈七 - 数据科学
        addSkill(6L, "Python", "INTERMEDIATE");
        addSkill(6L, "数据分析", "ADVANCED");
        addSkill(6L, "Tableau", "INTERMEDIATE");
        addSkill(6L, "SQL", "INTERMEDIATE");

        // 刘八 - 电子信息
        addSkill(7L, "C++", "ADVANCED");
        addSkill(7L, "嵌入式开发", "INTERMEDIATE");
        addSkill(7L, "FPGA", "BEGINNER");
        addSkill(7L, "电路设计", "INTERMEDIATE");

        // 孙九 - 数学
        addSkill(8L, "数学建模", "ADVANCED");
        addSkill(8L, "Python", "INTERMEDIATE");
        addSkill(8L, "R语言", "INTERMEDIATE");
        addSkill(8L, "统计分析", "ADVANCED");

        // 周十 - 物联网
        addSkill(9L, "嵌入式开发", "INTERMEDIATE");
        addSkill(9L, "Java", "INTERMEDIATE");
        addSkill(9L, "物联网通信", "INTERMEDIATE");
        addSkill(9L, "硬件设计", "BEGINNER");

        // 张三维现有技能
        addSkill(2L, "Java", "INTERMEDIATE");
        addSkill(2L, "前端开发", "INTERMEDIATE");
        addSkill(2L, "项目管理", "INTERMEDIATE");
    }

    private void initRecruits() {
        if (recruitMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 招募1: 互联网+竞赛组队
        Recruit r1 = new Recruit();
        r1.setPublisherId(2L);
        r1.setTitle("互联网+创新创业大赛项目组队");
        r1.setDescription("我们正在准备2026年互联网+大赛，项目主题是AI驱动的教育平台。需要志同道合的队友一起完成项目开发和商业计划书撰写。");
        r1.setProjectType("INNOVATION");
        r1.setRequiredSkills("[\"Java\",\"Python\",\"前端开发\",\"商业分析\"]");
        r1.setMaxMembers(6);
        r1.setCurrentMembers(1);
        r1.setDeadline(LocalDateTime.of(2026, 7, 15, 23, 59));
        r1.setStatus("OPEN");
        r1.setViewCount(0);
        recruitMapper.insert(r1);

        // 招募2: 数学建模竞赛
        Recruit r2 = new Recruit();
        r2.setPublisherId(8L);
        r2.setTitle("全国大学生数学建模竞赛队友招募");
        r2.setDescription("寻找数模队友！本人有两次参赛经验，擅长建模和论文写作。希望找到擅长编程和数据分析的同学组队。");
        r2.setProjectType("ACADEMIC");
        r2.setRequiredSkills("[\"数学建模\",\"Python\",\"数据分析\",\"LaTeX\"]");
        r2.setMaxMembers(3);
        r2.setCurrentMembers(1);
        r2.setDeadline(LocalDateTime.of(2026, 8, 20, 23, 59));
        r2.setStatus("OPEN");
        r2.setViewCount(0);
        recruitMapper.insert(r2);

        // 招募3: 挑战杯项目
        Recruit r3 = new Recruit();
        r3.setPublisherId(4L);
        r3.setTitle("挑战杯-智慧校园项目组队");
        r3.setDescription("基于物联网的智慧教室管理系统，已有一个原型。需要Web全栈、嵌入式开发和UI设计的同学加入。已有指导老师支持。");
        r3.setProjectType("ACADEMIC");
        r3.setRequiredSkills("[\"Java\",\"Spring Boot\",\"Vue.js\",\"嵌入式开发\",\"UI设计\"]");
        r3.setMaxMembers(5);
        r3.setCurrentMembers(1);
        r3.setDeadline(LocalDateTime.of(2026, 7, 30, 23, 59));
        r3.setStatus("OPEN");
        r3.setViewCount(0);
        recruitMapper.insert(r3);

        // 招募4: 科研项目
        Recruit r4 = new Recruit();
        r4.setPublisherId(5L);
        r4.setTitle("AI辅助医学影像分析科研项目");
        r4.setDescription("与医学院合作的交叉学科项目，用深度学习技术辅助CT影像分析。需要有机器学习和Python基础的同学，医学知识由合作方提供。");
        r4.setProjectType("RESEARCH");
        r4.setRequiredSkills("[\"Python\",\"PyTorch\",\"机器学习\",\"图像处理\"]");
        r4.setMaxMembers(4);
        r4.setCurrentMembers(1);
        r4.setDeadline(LocalDateTime.of(2026, 8, 31, 23, 59));
        r4.setStatus("OPEN");
        r4.setViewCount(0);
        recruitMapper.insert(r4);

        // ---- 申请记录 ----
        // 赵六申请招募1(互联网+)
        Application a1 = new Application();
        a1.setRecruitId(r1.getId());
        a1.setApplicantId(5L);
        a1.setStatus("PENDING");
        applicationMapper.insert(a1);

        // 陈七申请招募1
        Application a2 = new Application();
        a2.setRecruitId(r1.getId());
        a2.setApplicantId(6L);
        a2.setStatus("PENDING");
        applicationMapper.insert(a2);

        // 刘八申请招募2(数模)
        Application a3 = new Application();
        a3.setRecruitId(r2.getId());
        a3.setApplicantId(7L);
        a3.setStatus("PENDING");
        applicationMapper.insert(a3);

        // 王五申请招募3(挑战杯)
        Application a4 = new Application();
        a4.setRecruitId(r3.getId());
        a4.setApplicantId(3L);
        a4.setStatus("ACCEPTED");
        applicationMapper.insert(a4);

        System.out.println("Seed recruits and applications created.");
    }

    private void initTeams() {
        if (teamMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 查看招募1的信息
        Recruit r1 = recruitMapper.selectOne(
                new LambdaQueryWrapper<Recruit>().eq(Recruit::getTitle, "互联网+创新创业大赛项目组队"));
        if (r1 == null) return;

        // 团队1: 互联网+项目团队
        Team t1 = new Team();
        t1.setName("AI教育先锋队");
        t1.setLeaderId(2L);
        t1.setRecruitId(r1.getId());
        t1.setStatus("ACTIVE");
        teamMapper.insert(t1);

        // 队长作为成员
        TeamMember tm1 = new TeamMember();
        tm1.setTeamId(t1.getId());
        tm1.setUserId(2L);
        tm1.setRole("LEADER");
        teamMemberMapper.insert(tm1);

        // 已接受的申请者作为成员
        TeamMember tm2 = new TeamMember();
        tm2.setTeamId(t1.getId());
        tm2.setUserId(3L);
        tm2.setRole("MEMBER");
        teamMemberMapper.insert(tm2);

        // 更新招募current_members
        r1.setCurrentMembers(2);
        recruitMapper.updateById(r1);

        System.out.println("Seed team created: " + t1.getName());
    }

    private void initProjects() {
        if (projectMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        Team t1 = teamMapper.selectOne(
                new LambdaQueryWrapper<Team>().eq(Team::getName, "AI教育先锋队"));
        if (t1 == null) return;

        // 项目（带leader_id）
        Project p1 = new Project();
        p1.setTeamId(t1.getId());
        p1.setLeaderId(2L); // 张三作为项目负责人
        p1.setName("基于AI的个性化学习推荐平台");
        p1.setDescription("利用机器学习算法分析学生学习行为，提供个性化的学习资源推荐。项目目标：完成核心推荐算法、Web端和移动端适配。");
        p1.setStatus("ONGOING");
        p1.setStartedAt(LocalDate.of(2026, 6, 1));
        p1.setEndedAt(LocalDate.of(2026, 10, 30));
        projectMapper.insert(p1);

        // 项目任务（新结构：creator_id + required_skills）
        Task task1 = new Task();
        task1.setProjectId(p1.getId());
        task1.setCreatorId(2L);
        task1.setTitle("需求分析与系统设计");
        task1.setDescription("完成项目需求文档、系统架构设计和数据库设计");
        task1.setRequiredSkills("Java,Spring Boot,系统设计");
        task1.setPriority("HIGH");
        task1.setStatus("DONE");
        task1.setDeadline(LocalDateTime.of(2026, 6, 15, 23, 59));
        taskMapper.insert(task1);

        Task task2 = new Task();
        task2.setProjectId(p1.getId());
        task2.setCreatorId(2L);
        task2.setTitle("用户管理模块开发");
        task2.setDescription("实现用户注册、登录、个人信息管理等功能");
        task2.setRequiredSkills("Java,Spring Boot,Vue.js");
        task2.setPriority("HIGH");
        task2.setStatus("IN_PROGRESS");
        task2.setDeadline(LocalDateTime.of(2026, 7, 10, 23, 59));
        taskMapper.insert(task2);

        Task task3 = new Task();
        task3.setProjectId(p1.getId());
        task3.setCreatorId(2L);
        task3.setTitle("学习行为数据采集");
        task3.setDescription("设计并实现用户学习行为的数据采集和存储模块");
        task3.setRequiredSkills("Python,数据分析,MySQL");
        task3.setPriority("MEDIUM");
        task3.setStatus("TODO");
        task3.setDeadline(LocalDateTime.of(2026, 7, 20, 23, 59));
        taskMapper.insert(task3);

        Task task4 = new Task();
        task4.setProjectId(p1.getId());
        task4.setCreatorId(2L);
        task4.setTitle("推荐算法原型开发");
        task4.setDescription("基于协同过滤和知识图谱的混合推荐算法实现");
        task4.setRequiredSkills("Python,PyTorch,机器学习");
        task4.setPriority("HIGH");
        task4.setStatus("TODO");
        task4.setDeadline(LocalDateTime.of(2026, 8, 15, 23, 59));
        taskMapper.insert(task4);

        Task task5 = new Task();
        task5.setProjectId(p1.getId());
        task5.setCreatorId(2L);
        task5.setTitle("前端页面开发");
        task5.setDescription("实现Web端主要页面，包括首页、学习推荐、个人中心等");
        task5.setRequiredSkills("Vue.js,TypeScript,前端开发");
        task5.setPriority("MEDIUM");
        task5.setStatus("TODO");
        task5.setDeadline(LocalDateTime.of(2026, 8, 30, 23, 59));
        taskMapper.insert(task5);

        System.out.println("Seed project and tasks created: " + p1.getName());
    }

    private void initNotifications() {
        if (notificationMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 通知：王五的申请被接受
        Notification n1 = new Notification();
        n1.setUserId(3L);
        n1.setTitle("申请已通过");
        n1.setContent("你申请加入的「挑战杯-智慧校园项目」已通过，请尽快联系发布者。");
        n1.setType("APPLICATION");
        n1.setIsRead(0);
        notificationMapper.insert(n1);

        // 通知：系统公告
        Notification n2 = new Notification();
        n2.setUserId(1L);
        n2.setTitle("系统上线通知");
        n2.setContent("大学生竞赛/科研项目组队匹配与过程管理平台已正式上线，欢迎使用！");
        n2.setType("SYSTEM");
        n2.setIsRead(0);
        notificationMapper.insert(n2);

        // 更多通知
        List.of(2L, 3L, 4L, 5L).forEach(userId -> {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setTitle("欢迎使用平台");
            n.setContent("欢迎加入！完善个人资料和技能标签，可以获得更精准的组队推荐。");
            n.setType("SYSTEM");
            n.setIsRead(0);
            notificationMapper.insert(n);
        });

        System.out.println("Seed notifications created.");
    }

    private void initTeachers() {
        if (teacherMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 王教授 - 计算机科学与技术 (userId=10, phone=13800000010)
        Teacher t1 = new Teacher();
        t1.setUserId(10L);
        t1.setDepartment("计算机科学与技术学院");
        t1.setTitle("教授");
        t1.setResearchFields("[\"分布式系统\",\"云计算\",\"大数据\"]");
        t1.setMaxTeams(3);
        t1.setCurrentTeams(0);
        t1.setIsVerified(1);
        t1.setRating(new java.math.BigDecimal("4.8"));
        teacherMapper.insert(t1);

        // 李教授 - 人工智能 (userId=11, phone=13800000011)
        Teacher t2 = new Teacher();
        t2.setUserId(11L);
        t2.setDepartment("人工智能学院");
        t2.setTitle("副教授");
        t2.setResearchFields("[\"机器学习\",\"自然语言处理\",\"知识图谱\"]");
        t2.setMaxTeams(5);
        t2.setCurrentTeams(1);
        t2.setIsVerified(1);
        t2.setRating(new java.math.BigDecimal("4.5"));
        teacherMapper.insert(t2);

        // 张教授 - 电子信息工程 (userId=12, phone=13800000012)
        Teacher t3 = new Teacher();
        t3.setUserId(12L);
        t3.setDepartment("电子信息工程学院");
        t3.setTitle("教授");
        t3.setResearchFields("[\"嵌入式系统\",\"物联网\",\"信号处理\"]");
        t3.setMaxTeams(4);
        t3.setCurrentTeams(0);
        t3.setIsVerified(1);
        t3.setRating(new java.math.BigDecimal("4.6"));
        teacherMapper.insert(t3);

        System.out.println("Seed teachers created: 王教授, 李教授, 张教授");
    }

    private void initAchievements() {
        if (achievementMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        Team t1 = teamMapper.selectOne(
                new LambdaQueryWrapper<Team>().eq(Team::getName, "AI教育先锋队"));
        if (t1 == null) return;

        // 成果1: 竞赛获奖
        Achievement a1 = new Achievement();
        a1.setTeamId(t1.getId());
        a1.setTitle("2025年全国大学生AI创新应用大赛一等奖");
        a1.setType("AWARD");
        a1.setDescription("团队开发的「智能学习助手」在全国大学生AI创新应用大赛中荣获一等奖。项目基于大语言模型和知识图谱技术，为学生提供个性化学习辅导。");
        a1.setAwardLevel("NATIONAL");
        a1.setIsPublic(1);
        a1.setIsFeatured(1);
        a1.setViewCount(256);
        a1.setLikeCount(48);
        achievementMapper.insert(a1);

        // 成果2: 论文
        Achievement a2 = new Achievement();
        a2.setTeamId(t1.getId());
        a2.setTitle("基于深度强化学习的自适应学习路径推荐方法");
        a2.setType("PAPER");
        a2.setDescription("发表于《计算机教育》2025年第3期。提出了一种结合知识追踪和深度强化学习的自适应学习路径推荐方法。");
        a2.setAwardLevel("NATIONAL");
        a2.setIsPublic(1);
        a2.setIsFeatured(0);
        a2.setViewCount(132);
        a2.setLikeCount(23);
        achievementMapper.insert(a2);

        // 成果3: 软件著作权
        Achievement a3 = new Achievement();
        a3.setTeamId(t1.getId());
        a3.setTitle("智能学习推荐平台V1.0");
        a3.setType("SOFTWARE");
        a3.setDescription("登记号：2025SR1234567。基于协同过滤和知识图谱的混合推荐算法，实现了学习资源的精准推荐。");
        a3.setAwardLevel("NATIONAL");
        a3.setIsPublic(1);
        a3.setIsFeatured(0);
        a3.setViewCount(89);
        a3.setLikeCount(15);
        achievementMapper.insert(a3);

        // 成果4: 专利
        Achievement a4 = new Achievement();
        a4.setTeamId(t1.getId());
        a4.setTitle("一种基于用户画像的学习资源个性化推荐方法");
        a4.setType("PATENT");
        a4.setDescription("发明专利，申请号：CN202510012345.6。通过构建多维用户画像，实现学习资源的智能推荐。");
        a4.setAwardLevel("PROVINCIAL");
        a4.setIsPublic(1);
        a4.setIsFeatured(0);
        a4.setViewCount(67);
        a4.setLikeCount(10);
        achievementMapper.insert(a4);

        System.out.println("Seed achievements created: 4 items");
    }

    private void initAuditLogs() {
        if (auditLogMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        // 审核日志1: 审核通过的招募
        AuditLog log1 = new AuditLog();
        log1.setTargetType("RECRUIT");
        log1.setTargetId(1L);
        log1.setOperatorId(1L);
        log1.setAction("APPROVE");
        log1.setReason("符合平台规范，项目内容完整");
        log1.setBeforeStatus("PENDING");
        log1.setAfterStatus("APPROVED");
        auditLogMapper.insert(log1);

        // 审核日志2: 审核驳回的招募
        AuditLog log2 = new AuditLog();
        log2.setTargetType("RECRUIT");
        log2.setTargetId(2L);
        log2.setOperatorId(1L);
        log2.setAction("REJECT");
        log2.setReason("项目描述不够详细，需要补充具体方案");
        log2.setBeforeStatus("PENDING");
        log2.setAfterStatus("REJECTED");
        auditLogMapper.insert(log2);

        // 审核日志3: 审核通过的招募
        AuditLog log3 = new AuditLog();
        log3.setTargetType("RECRUIT");
        log3.setTargetId(3L);
        log3.setOperatorId(1L);
        log3.setAction("APPROVE");
        log3.setReason("项目创新性好，符合大赛要求");
        log3.setBeforeStatus("PENDING");
        log3.setAfterStatus("APPROVED");
        auditLogMapper.insert(log3);

        System.out.println("Seed audit logs created: 3 records");
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
