package com.platform.common.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

public class CaptchaUtil {

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int CODE_LENGTH = 4;
    private static final Random RANDOM = new Random();

    /**
     * Generate captcha image and return { code, base64Image }
     */
    public static CaptchaResult generate() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(248, 249, 252));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Interference lines
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            g.setColor(randomColor(180, 210));
            int x1 = RANDOM.nextInt(WIDTH / 2);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = WIDTH / 2 + RANDOM.nextInt(WIDTH / 2);
            int y2 = RANDOM.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // Interference dots
        for (int i = 0; i < 30; i++) {
            g.setColor(randomColor(160, 200));
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            g.fillOval(x, y, 2, 2);
        }

        // Draw characters
        StringBuilder code = new StringBuilder();
        Font[] fonts = {
            new Font("Arial", Font.BOLD, 26),
            new Font("Arial", Font.ITALIC | Font.BOLD, 26),
            new Font("Arial", Font.BOLD, 24),
        };

        int charWidth = WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < CODE_LENGTH; i++) {
            String ch = String.valueOf(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            code.append(ch);

            g.setFont(fonts[RANDOM.nextInt(fonts.length)]);
            g.setColor(randomColor(40, 100));

            // Random rotation
            double angle = (RANDOM.nextDouble() - 0.5) * 0.5;
            int x = charWidth * (i + 1);
            int y = HEIGHT - 14 + RANDOM.nextInt(8);

            g.rotate(angle, x, y);
            g.drawString(ch, x - 10, y);
            g.rotate(-angle, x, y);
        }

        // Border
        g.setColor(new Color(220, 224, 232));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 6, 6);

        g.dispose();

        // Encode to base64
        String base64;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }

        return new CaptchaResult(code.toString(), base64);
    }

    private static Color randomColor(int min, int max) {
        int r = min + RANDOM.nextInt(max - min);
        int g = min + RANDOM.nextInt(max - min);
        int b = min + RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }

    public record CaptchaResult(String code, String base64Image) {}
}
