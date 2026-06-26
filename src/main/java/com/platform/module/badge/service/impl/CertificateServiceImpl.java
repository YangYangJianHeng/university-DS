package com.platform.module.badge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.module.badge.entity.Badge;
import com.platform.module.badge.entity.Certificate;
import com.platform.module.badge.mapper.BadgeMapper;
import com.platform.module.badge.mapper.CertificateMapper;
import com.platform.module.badge.service.BadgeService;
import com.platform.module.badge.service.CertificateService;
import com.platform.module.user.entity.User;
import com.platform.module.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class CertificateServiceImpl extends ServiceImpl<CertificateMapper, Certificate> implements CertificateService {

    private final CertificateMapper certificateMapper;
    private final BadgeMapper badgeMapper;
    private final UserMapper userMapper;
    private final BadgeService badgeService;

    @Value("${platform.file.upload-dir:./uploads}")
    private String uploadDir;

    public CertificateServiceImpl(CertificateMapper certificateMapper,
                                   BadgeMapper badgeMapper,
                                   UserMapper userMapper,
                                   BadgeService badgeService) {
        this.certificateMapper = certificateMapper;
        this.badgeMapper = badgeMapper;
        this.userMapper = userMapper;
        this.badgeService = badgeService;
    }

    @Override
    public List<Certificate> syncCertificates(Long badgeId) {
        List<Certificate> generated = new ArrayList<>();

        List<Badge> badges;
        if (badgeId != null) {
            Badge badge = badgeMapper.selectById(badgeId);
            badges = badge != null ? List.of(badge) : Collections.emptyList();
        } else {
            badges = badgeMapper.selectList(null);
        }

        // 获取所有活跃用户
        List<User> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        if (allUsers.isEmpty()) return generated;

        for (Badge badge : badges) {
            for (User user : allUsers) {
                // 检查是否已存在证书
                Long count = certificateMapper.selectCount(
                        new LambdaQueryWrapper<Certificate>()
                                .eq(Certificate::getUserId, user.getId())
                                .eq(Certificate::getBadgeId, badge.getId()));
                if (count > 0) continue;

                // 检查用户是否满足徽章条件
                if (!checkUserQualifiesForBadge(user.getId(), badge.getId())) continue;

                // 生成证书
                Certificate cert = doGenerateCertificate(user, badge);
                if (cert != null) {
                    generated.add(cert);
                }
            }
        }
        return generated;
    }

    @Override
    public void deleteCertificate(Long id) {
        Certificate cert = certificateMapper.selectById(id);
        if (cert != null) {
            // 删除图片文件
            if (cert.getImagePath() != null && !cert.getImagePath().isEmpty()) {
                try {
                    Path imgPath = Paths.get(uploadDir, cert.getImagePath().replace("/uploads/", ""));
                    Files.deleteIfExists(imgPath);
                } catch (IOException e) {
                    log.warn("Failed to delete certificate image: {}", e.getMessage());
                }
            }
            certificateMapper.deleteById(id);
        }
    }

    @Override
    public List<Certificate> listAllCertificates(int page, int size) {
        Page<Certificate> p = new Page<>(page, size);
        Page<Certificate> result = certificateMapper.selectPage(p,
                new LambdaQueryWrapper<Certificate>()
                        .orderByDesc(Certificate::getAwardedAt));
        return result.getRecords();
    }

    @Override
    public long countAllCertificates() {
        return certificateMapper.selectCount(null);
    }

    @Override
    public List<Certificate> getUserCertificates(Long userId) {
        return certificateMapper.selectList(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getUserId, userId)
                        .orderByDesc(Certificate::getAwardedAt));
    }

    @Override
    public Certificate generateCertificate(Long userId, Long badgeId) {
        // 检查是否已存在
        Long count = certificateMapper.selectCount(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getUserId, userId)
                        .eq(Certificate::getBadgeId, badgeId));
        if (count > 0) {
            return certificateMapper.selectOne(
                    new LambdaQueryWrapper<Certificate>()
                            .eq(Certificate::getUserId, userId)
                            .eq(Certificate::getBadgeId, badgeId));
        }

        User user = userMapper.selectById(userId);
        Badge badge = badgeMapper.selectById(badgeId);
        if (user == null || badge == null) return null;

        return doGenerateCertificate(user, badge);
    }

    @Override
    public Map<String, Object> getCertificateViewData(Long certificateId) {
        Certificate cert = certificateMapper.selectById(certificateId);
        if (cert == null) return Collections.emptyMap();

        Map<String, Object> data = new HashMap<>();
        data.put("id", cert.getId());
        data.put("userId", cert.getUserId());
        data.put("badgeId", cert.getBadgeId());
        data.put("badgeName", cert.getBadgeName());
        data.put("userName", cert.getUserName());
        data.put("studentId", cert.getStudentId());
        data.put("imagePath", cert.getImagePath());
        data.put("awardedAt", cert.getAwardedAt() != null ? cert.getAwardedAt().toString() : null);
        return data;
    }

    @Override
    public List<Certificate> regenerateAllCertificates() {
        log.info("===== 开始重新生成所有证书 =====");

        // 1. 删除所有旧证书记录和图片文件
        List<Certificate> allCerts = certificateMapper.selectList(null);
        for (Certificate cert : allCerts) {
            if (cert.getImagePath() != null && !cert.getImagePath().isEmpty()) {
                try {
                    Path imgPath = Paths.get(uploadDir, cert.getImagePath().replace("/uploads/", ""));
                    Files.deleteIfExists(imgPath);
                } catch (IOException e) {
                    log.warn("删除旧证书图片失败: {}", e.getMessage());
                }
            }
        }
        // 清空表
        certificateMapper.delete(null);
        log.info("已删除 {} 条旧证书记录", allCerts.size());

        // 2. 重新同步生成
        List<Certificate> generated = syncCertificates(null);
        log.info("已重新生成 {} 张证书", generated.size());
        log.info("===== 证书重新生成完成 =====");

        return generated;
    }

    // ===== 内部方法 =====

    /**
     * 检测用户是否满足徽章条件
     */
    private boolean checkUserQualifiesForBadge(Long userId, Long badgeId) {
        List<Map<String, Object>> userBadges = badgeService.getUserBadges(userId);
        return userBadges.stream()
                .anyMatch(b -> Long.valueOf(b.get("id").toString()).equals(badgeId)
                        && Boolean.TRUE.equals(b.get("unlocked")));
    }

    private Certificate doGenerateCertificate(User user, Badge badge) {
        try {
            // 确保上传目录存在
            Path certDir = Paths.get(uploadDir, "certificates");
            Files.createDirectories(certDir);

            String fileName = "cert_" + user.getId() + "_" + badge.getId() + "_" + System.currentTimeMillis() + ".png";
            Path outputPath = certDir.resolve(fileName);

            // 生成奖状图片
            BufferedImage image = generateCertificateImage(user.getUsername(),
                    user.getStudentId(), badge.getName());
            ImageIO.write(image, "png", outputPath.toFile());

            // 保存证书记录
            Certificate cert = new Certificate();
            cert.setUserId(user.getId());
            cert.setBadgeId(badge.getId());
            cert.setBadgeName(badge.getName());
            cert.setUserName(user.getUsername());
            cert.setStudentId(user.getStudentId());
            cert.setImagePath("/uploads/certificates/" + fileName);
            cert.setAwardedAt(LocalDateTime.now());
            certificateMapper.insert(cert);

            log.info("Certificate generated: user={}, badge={}, path={}",
                    user.getUsername(), badge.getName(), cert.getImagePath());
            return cert;
        } catch (Exception e) {
            log.error("Failed to generate certificate for user={}, badge={}: {}",
                    user.getId(), badge.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用 zhengshu.png 作为背景模板，在其上叠加文字生成证书
     * 根据模板已有文字，只添加动态内容
     */
    private BufferedImage generateCertificateImage(String userName, String studentId, String badgeName) {
        // 加载背景模板
        BufferedImage bgImage = loadBackgroundImage();
        if (bgImage == null) {
            throw new RuntimeException("无法加载证书背景图片 zhengshu.png");
        }

        int width = bgImage.getWidth();
        int height = bgImage.getHeight();
        int centerX = width / 2;

        // 创建新图片，复制背景
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // 开启抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // 绘制背景
        g.drawImage(bgImage, 0, 0, null);

        FontRenderContext frc = g.getFontRenderContext();

        // 根据模板宽度动态计算字体大小
        double fontSizeRatio = width / 1200.0;
        Font contentFont = new Font("楷体", Font.PLAIN, (int) (28 * fontSizeRatio));
        Font nameFont = new Font("楷体", Font.BOLD, (int) (40 * fontSizeRatio));
        Font awardFont = new Font("楷体", Font.BOLD, (int) (48 * fontSizeRatio));
        Font numberFont = new Font("楷体", Font.PLAIN, (int) (16 * fontSizeRatio));
        Font dateFont = new Font("楷体", Font.PLAIN, (int) (28 * fontSizeRatio));  // 日期字号放大

        // 正文颜色 - 深棕墨色
        Color textColor = new Color(60, 40, 30);
        Color accentColor = new Color(170, 40, 35);  // 红色高亮

        // 坐标比例（根据模板布局微调）
        double certNoXRatio = 0.195;    // 证书编号右侧（模板"证书编号："冒号后面，再右移）
        double certNoYRatio = 0.138;    // 证书编号Y位置
        double nameYRatio = 0.425;      // 姓名Y位置（横线上方）
        double honorYRatio = 0.535;     // 荣誉内容行
        double badgeYRatio = 0.605;     // 奖项名称行
        double certifyYRatio = 0.68;    // 特发此证行
        double bottomInfoRatio = 0.835; // 日期/公章行（往下移）

        // ===== 1. 证书编号（模板已有"证书编号："，只追加编号）=====
        g.setColor(textColor);
        g.setFont(numberFont);
        String certNo = generateCertNo();
        g.drawString(certNo, (int) (width * certNoXRatio), (int) (height * certNoYRatio));

        // ===== 2. 姓名（模板已有"同学："和横线，只把名字写在横线上）=====
        g.setColor(textColor);
        g.setFont(nameFont);
        String nameText = (userName != null && !userName.isEmpty()) ? userName : "        ";
        Rectangle2D nameBounds = nameFont.getStringBounds(nameText, frc);

        // 姓名在横线上居中（横线在"同学"前面，所以姓名要偏左一点）
        // 模板布局：_______同学，姓名写在横线上，整体居中
        int nameX = (int) (centerX - nameBounds.getWidth() / 2) - (int) (25 * fontSizeRatio);
        int nameY = (int) (height * nameYRatio);
        g.drawString(nameText, nameX, nameY);

        // ===== 3. 荣誉内容（在平台竞赛活动中表现优异，荣获）=====
        g.setColor(textColor);
        g.setFont(contentFont);
        String honorText = "在平台竞赛活动中表现优异，荣获";
        Rectangle2D honorBounds = contentFont.getStringBounds(honorText, frc);
        g.drawString(honorText, (int) (centerX - honorBounds.getWidth() / 2), (int) (height * honorYRatio));

        // ===== 4. 奖项名称（红色大字「badgeName」）=====
        g.setColor(accentColor);
        g.setFont(awardFont);
        String awardText = "「" + badgeName + "」";
        Rectangle2D awardBounds = awardFont.getStringBounds(awardText, frc);
        g.drawString(awardText, (int) (centerX - awardBounds.getWidth() / 2), (int) (height * badgeYRatio));

        // ===== 5. 特发此证，以资鼓励！=====
        g.setColor(textColor);
        g.setFont(contentFont);
        String certifyText = "特发此证，以资鼓励！";
        Rectangle2D certifyBounds = contentFont.getStringBounds(certifyText, frc);
        g.drawString(certifyText, (int) (centerX - certifyBounds.getWidth() / 2), (int) (height * certifyYRatio));

        // ===== 6. 底部：日期（模板有"日期："，只追加日期）+ 公章 =====
        // 日期（在"日期："文字右侧，稍微偏右）
        g.setColor(textColor);
        g.setFont(dateFont);
        String dateStr = DateTimeFormatter.ofPattern("yyyy年MM月dd日").format(LocalDateTime.now());
        g.drawString(dateStr, (int) (width * 0.175), (int) (height * bottomInfoRatio));

        // 红色圆形公章
        int sealDiameter = (int) (95 * fontSizeRatio);
        int sealX = (int) (width * 0.795 - sealDiameter / 2);
        int sealY = (int) (height * bottomInfoRatio) - sealDiameter + (int) (38 * fontSizeRatio);
        drawSeal(g, sealX, sealY, sealDiameter, "竞赛组队平台");

        g.dispose();
        return image;
    }

    /**
     * 加载证书背景图片
     */
    private BufferedImage loadBackgroundImage() {
        try {
            // 尝试多个可能的路径
            Path[] possiblePaths = {
                Paths.get("zhengshu.png"),
                Paths.get(uploadDir).getParent().resolve("zhengshu.png"),
                Paths.get(System.getProperty("user.dir"), "zhengshu.png")
            };

            for (Path path : possiblePaths) {
                if (Files.exists(path)) {
                    log.info("加载证书背景图片: {}", path.toAbsolutePath());
                    return ImageIO.read(path.toFile());
                }
            }

            // 尝试从类路径加载
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("zhengshu.png");
            if (is != null) {
                return ImageIO.read(is);
            }

            log.error("无法找到证书背景图片 zhengshu.png");
            return null;
        } catch (IOException e) {
            log.error("加载证书背景图片失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成证书编号
     */
    private String generateCertNo() {
        return DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 绘制红色圆形公章，自适应尺寸
     */
    private void drawSeal(Graphics2D g, int x, int y, int diameter, String text) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = x + diameter / 2;
        int centerY = y + diameter / 2;
        Color sealRed = new Color(190, 30, 30);

        // 外圆 - 红色粗边框
        g2d.setColor(sealRed);
        float strokeWidth = diameter / 35.0f;
        g2d.setStroke(new BasicStroke(strokeWidth));
        g2d.drawOval(x, y, diameter, diameter);

        // 内圆
        int inset = diameter / 12;
        g2d.setStroke(new BasicStroke(strokeWidth * 0.5f));
        g2d.drawOval(x + inset, y + inset, diameter - inset * 2, diameter - inset * 2);

        // 印章文字 - 字号按公章比例
        int fontSize = diameter / 5;
        Font sealFont = new Font("楷体", Font.BOLD, fontSize);
        g2d.setFont(sealFont);
        FontRenderContext frc = g2d.getFontRenderContext();

        // 按字符数分行：3-4字一行，居中排列
        String line1, line2;
        if (text.length() <= 4) {
            line1 = text;
            line2 = "";
        } else {
            int mid = (text.length() + 1) / 2;
            line1 = text.substring(0, mid);
            line2 = text.substring(mid);
        }

        Rectangle2D bounds1 = sealFont.getStringBounds(line1, frc);
        g2d.drawString(line1, (int) (centerX - bounds1.getWidth() / 2), centerY - fontSize / 3);

        if (!line2.isEmpty()) {
            Rectangle2D bounds2 = sealFont.getStringBounds(line2, frc);
            g2d.drawString(line2, (int) (centerX - bounds2.getWidth() / 2), centerY + fontSize + fontSize / 3);
        }

        // 中心五角星装饰（半透明）
        g2d.setColor(new Color(190, 30, 30, 100));
        int starRadius = diameter / 10;
        drawStar(g2d, centerX, centerY, starRadius, 5);

        g2d.dispose();
    }

    /**
     * 绘制五角星
     */
    private void drawStar(Graphics2D g, int cx, int cy, int radius, int points) {
        int[] xPoints = new int[points * 2];
        int[] yPoints = new int[points * 2];

        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / points * i - Math.PI / 2;
            int r = (i % 2 == 0) ? radius : radius / 2;
            xPoints[i] = cx + (int) (Math.cos(angle) * r);
            yPoints[i] = cy + (int) (Math.sin(angle) * r);
        }

        g.fillPolygon(xPoints, yPoints, points * 2);
    }
}
