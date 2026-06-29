package com.platform.module.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 实时上下文增强服务
 * 在 System Prompt 中注入真实的日期、时间、天气等外部信息，
 * 让本地 Ollama 模型也能回答"现在几点""今天天气怎样"等实时问题。
 *
 * 原理：每次对话前将当前真实数据拼入 System Prompt，任何模型都能用（无需 function calling）。
 */
@Slf4j
@Service
public class ContextEnrichmentService {

    /** 默认城市（可配置） */
    private static final String DEFAULT_CITY = "Beijing";
    private static final String WTTR_URL = "https://wttr.in/";

    /** 天气缓存（减少请求） */
    private volatile String cachedWeather = null;
    private volatile long weatherCacheTime = 0;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10分钟

    // ==================== 时间 ====================

    /**
     * 获取当前完整时间信息
     */
    public TimeInfo getCurrentTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        TimeInfo info = new TimeInfo();
        info.datetime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        info.date = now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        info.time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        info.weekday = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);
        info.year = now.getYear();
        info.month = now.getMonthValue();
        info.day = now.getDayOfMonth();
        info.isWeekend = now.getDayOfWeek().getValue() >= 6;
        return info;
    }

    /**
     * 格式化时间信息为 System Prompt 片段
     */
    public String formatTimeContext() {
        TimeInfo t = getCurrentTime();
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前时间信息（真实数据，请直接使用）\n");
        sb.append("- 现在是：").append(t.date).append(" ").append(t.weekday).append(" ").append(t.time).append("\n");
        sb.append("- 年份：").append(t.year).append("年，").append(t.month).append("月").append(t.day).append("日\n");
        sb.append("- 星期：").append(t.weekday);
        if (t.isWeekend) {
            sb.append("（周末）");
        }
        sb.append("\n");
        return sb.toString();
    }

    // ==================== 天气 ====================

    /**
     * 获取天气（使用免费 wttr.in API），带缓存
     */
    public String getWeather() {
        return getWeather(DEFAULT_CITY);
    }

    public String getWeather(String city) {
        // 检查缓存
        long now = System.currentTimeMillis();
        if (cachedWeather != null && (now - weatherCacheTime) < CACHE_TTL_MS) {
            return cachedWeather;
        }

        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            URL url = URI.create(WTTR_URL + encodedCity + "?format=j1").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "University-Platform/1.0");

            if (conn.getResponseCode() != 200) {
                log.warn("Weather API returned {}, using fallback", conn.getResponseCode());
                return formatWeatherFallback(city);
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) body.append(line);
            }
            conn.disconnect();

            String result = parseWttrResponse(body.toString(), city);
            cachedWeather = result;
            weatherCacheTime = now;
            return result;

        } catch (Exception e) {
            log.warn("Weather fetch failed: {}, using fallback", e.getMessage());
            return formatWeatherFallback(city);
        }
    }

    private String parseWttrResponse(String json, String city) {
        try {
            JSONObject root = JSON.parseObject(json);
            JSONObject current = root.getJSONObject("current_condition").getJSONArray("current_condition").getJSONObject(0);

            String condition = current.getJSONArray("weatherDesc").getJSONObject(0).getString("value");
            String tempC = current.getString("temp_C");
            String humidity = current.getString("humidity");
            String windSpeed = current.getString("windspeedKmph");
            String windDir = current.getString("winddir16Point");
            String feelsLike = current.getString("FeelsLikeC");
            String visibility = current.getString("visibility");

            return formatWeatherContext(city, condition, tempC, feelsLike, humidity, windSpeed, windDir, visibility);
        } catch (Exception e) {
            log.warn("Failed to parse weather response: {}", e.getMessage());
            return formatWeatherFallback(city);
        }
    }

    private String formatWeatherContext(String city, String condition, String temp,
                                        String feelsLike, String humidity,
                                        String windSpeed, String windDir, String visibility) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前天气信息（真实数据，来自气象API，请直接引用）\n");
        sb.append("- 城市：").append(city).append("\n");
        sb.append("- 天气：").append(condition).append("\n");
        sb.append("- 温度：").append(temp).append("°C");
        if (feelsLike != null && !feelsLike.isEmpty()) {
            sb.append("（体感 ").append(feelsLike).append("°C）");
        }
        sb.append("\n");
        sb.append("- 湿度：").append(humidity).append("%\n");
        sb.append("- 风速：").append(windSpeed).append(" km/h（").append(windDir).append("）\n");
        if (visibility != null && !visibility.isEmpty()) {
            sb.append("- 能见度：").append(visibility).append(" km\n");
        }
        return sb.toString();
    }

    private String formatWeatherFallback(String city) {
        TimeInfo t = getCurrentTime();
        return String.format(
                "## 天气信息（API暂时不可用）\n"
                + "- 城市：%s\n"
                + "- 当前是%d月%d日%s，无法获取实时天气数据。"
                + "请告知用户天气服务暂时不可用，可以建议用户自行查看天气APP。\n",
                city, t.month, t.day, t.weekday
        );
    }

    /**
     * 格式化所有实时上下文字段
     */
    public String buildEnrichedContext() {
        StringBuilder sb = new StringBuilder();
        sb.append(formatTimeContext());
        sb.append("\n");
        sb.append(getWeather());
        sb.append("\n");
        sb.append("注意：以上时间和天气信息是当前的真实数据，当用户询问时间或天气时，"
                + "请基于以上真实数据直接回答，不要说你不知道。\n");
        return sb.toString();
    }

    // ==================== 内部类 ====================

    public static class TimeInfo {
        public String datetime;
        public String date;
        public String time;
        public String weekday;
        public int year;
        public int month;
        public int day;
        public boolean isWeekend;
    }
}
