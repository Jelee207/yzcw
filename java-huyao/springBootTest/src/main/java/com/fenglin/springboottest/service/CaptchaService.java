package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图像验证码服务：生成 4 位验证码图片（base64），内存存储并校验，5 分钟过期
 */
@Service
public class CaptchaService {

    /** 验证码存储：captchaId -> [code, 生成时间戳] */
    private final Map<String, Object[]> store = new ConcurrentHashMap<>();

    private static final long EXPIRE_MS = 5 * 60 * 1000; // 5 分钟有效期
    private static final String CHARS = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    /**
     * 生成验证码
     * @return Map：captchaId（验证码标识）、captchaImg（base64 图片，含 data:image/png;base64 前缀）
     */
    public Map<String, String> generate() {
        String code = randomCode(4);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new Object[]{code, System.currentTimeMillis()});

        String base64 = drawImage(code);
        return Map.of(
                "captchaId", captchaId,
                "captchaImg", base64
        );
    }

    /**
     * 校验验证码（忽略大小写）
     * @return 正确返回 true，错误或过期返回 false
     */
    public boolean verify(String captchaId, String code) {
        if (captchaId == null || code == null) return false;
        Object[] entry = store.get(captchaId);
        if (entry == null) return false;
        long createTime = (Long) entry[1];
        // 过期则删除并失效
        if (System.currentTimeMillis() - createTime > EXPIRE_MS) {
            store.remove(captchaId);
            return false;
        }
        boolean ok = ((String) entry[0]).equalsIgnoreCase(code.trim());
        // 一次性使用，校验后即删除
        store.remove(captchaId);
        return ok;
    }

    /** 生成随机验证码字符串（排除易混淆字符 0O1lI） */
    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 用 Java2D 绘制验证码图片并转 base64 */
    private String drawImage(String code) {
        int w = 120, h = 44;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景（浅米白）
        g.setColor(new Color(250, 245, 234));
        g.fillRect(0, 0, w, h);

        // 干扰线
        Color[] colors = {
                new Color(176, 58, 58), new Color(142, 44, 44),
                new Color(184, 146, 74), new Color(127, 154, 153), new Color(201, 88, 79)
        };
        for (int i = 0; i < 5; i++) {
            g.setColor(colors[random.nextInt(colors.length)]);
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(random.nextInt(w), random.nextInt(h), random.nextInt(w), random.nextInt(h));
        }

        // 绘制字符（随机旋转、偏移）
        g.setFont(new Font("宋体", Font.BOLD, 26));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(colors[random.nextInt(colors.length)]);
            double angle = (random.nextDouble() - 0.5) * 0.6;
            int x = 18 + i * 24 + random.nextInt(4);
            int y = 20 + random.nextInt(12);
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        // 噪点
        for (int i = 0; i < 40; i++) {
            g.setColor(colors[random.nextInt(colors.length)]);
            g.fillOval(random.nextInt(w), random.nextInt(h), 2, 2);
        }
        g.dispose();

        // 转 base64
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }
    }
}
