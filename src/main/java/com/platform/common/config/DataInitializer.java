package com.platform.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.module.collaboration.entity.Task;
import com.platform.module.collaboration.mapper.TaskMapper;
import com.platform.module.notification.entity.Notification;
import com.platform.module.notification.mapper.NotificationMapper;
import com.platform.module.project.entity.Project;
import com.platform.module.project.mapper.ProjectMapper;
import com.platform.module.recruit.entity.Application;
import com.platform.module.recruit.entity.Recruit;
import com.platform.module.recruit.mapper.ApplicationMapper;
import com.platform.module.recruit.mapper.RecruitMapper;
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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, UserSkillMapper userSkillMapper,
                           RecruitMapper recruitMapper, ApplicationMapper applicationMapper,
                           TeamMapper teamMapper, TeamMemberMapper teamMemberMapper,
                           ProjectMapper projectMapper, TaskMapper taskMapper,
                           NotificationMapper notificationMapper,
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

        // 项目
        Project p1 = new Project();
        p1.setTeamId(t1.getId());
        p1.setName("基于AI的个性化学习推荐平台");
        p1.setDescription("利用机器学习算法分析学生学习行为，提供个性化的学习资源推荐。项目目标：完成核心推荐算法、Web端和移动端适配。");
        p1.setStatus("ONGOING");
        p1.setStartedAt(LocalDate.of(2026, 6, 1));
        p1.setEndedAt(LocalDate.of(2026, 10, 30));
        projectMapper.insert(p1);

        // 项目任务
        Task task1 = new Task();
        task1.setProjectId(p1.getId());
        task1.setTitle("需求分析与系统设计");
        task1.setDescription("完成项目需求文档、系统架构设计和数据库设计");
        task1.setAssigneeId(2L);
        task1.setPriority("HIGH");
        task1.setStatus("COMPLETED");
        task1.setDeadline(LocalDateTime.of(2026, 6, 15, 23, 59));
        taskMapper.insert(task1);

        Task task2 = new Task();
        task2.setProjectId(p1.getId());
        task2.setTitle("用户管理模块开发");
        task2.setDescription("实现用户注册、登录、个人信息管理等功能");
        task2.setAssigneeId(2L);
        task2.setPriority("HIGH");
        task2.setStatus("IN_PROGRESS");
        task2.setDeadline(LocalDateTime.of(2026, 7, 10, 23, 59));
        taskMapper.insert(task2);

        Task task3 = new Task();
        task3.setProjectId(p1.getId());
        task3.setTitle("学习行为数据采集");
        task3.setDescription("设计并实现用户学习行为的数据采集和存储模块");
        task3.setAssigneeId(3L);
        task3.setPriority("MEDIUM");
        task3.setStatus("PENDING");
        task3.setDeadline(LocalDateTime.of(2026, 7, 20, 23, 59));
        taskMapper.insert(task3);

        Task task4 = new Task();
        task4.setProjectId(p1.getId());
        task4.setTitle("推荐算法原型开发");
        task4.setDescription("基于协同过滤和知识图谱的混合推荐算法实现");
        task4.setAssigneeId(null);
        task4.setPriority("HIGH");
        task4.setStatus("PENDING");
        task4.setDeadline(LocalDateTime.of(2026, 8, 15, 23, 59));
        taskMapper.insert(task4);

        Task task5 = new Task();
        task5.setProjectId(p1.getId());
        task5.setTitle("前端页面开发");
        task5.setDescription("实现Web端主要页面，包括首页、学习推荐、个人中心等");
        task5.setAssigneeId(null);
        task5.setPriority("MEDIUM");
        task5.setStatus("PENDING");
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
}
