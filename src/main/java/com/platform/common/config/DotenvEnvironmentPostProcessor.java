package com.platform.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 加载项目根目录 .env 文件到 Spring Environment
 * 
 * 优先级 HIGHEST_PRECEDENCE 确保在 Spring 解析 application.yml 
 * 的 ${ENV_VAR} 占位符之前完成注入
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ENV_FILE_NAME = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = findEnvFile();
        if (envFile == null || !envFile.exists()) {
            System.out.println("[dotenv] .env file not found, using application.yml defaults");
            return;
        }

        try {
            Map<String, Object> map = parseEnvFile(envFile);
            if (!map.isEmpty()) {
                // 1. 注入到 System Properties（property placeholder 解析可用）
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    System.setProperty(entry.getKey(), (String) entry.getValue());
                }
                // 2. 注入到 Spring Environment 最前端
                environment.getPropertySources()
                        .addFirst(new MapPropertySource("dotenv", map));
                System.out.println("[dotenv] Loaded " + map.size() + " variables from " + envFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[dotenv] Failed to load .env: " + e.getMessage());
        }
    }

    /**
     * 手动解析 .env 文件，避免 Properties.load() 的 ISO-8859-1 编码问题
     */
    private Map<String, Object> parseEnvFile(File envFile) throws Exception {
        Map<String, Object> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) continue;
                // 解析 KEY=VALUE
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) continue;
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                // 去除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private File findEnvFile() {
        // 1. user.dir 优先（IDE 运行场景）
        File cwdFile = new File(System.getProperty("user.dir"), ENV_FILE_NAME);
        if (cwdFile.exists()) return cwdFile;

        // 2. 向上遍历项目根目录
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File env = new File(current, ENV_FILE_NAME);
            if (env.exists()) return env;
            current = current.getParentFile();
        }

        // 3. classpath 目录遍历
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String entry : classpath.split(File.pathSeparator)) {
                File dir = new File(entry);
                if (!dir.isDirectory()) dir = dir.getParentFile();
                while (dir != null) {
                    File env = new File(dir, ENV_FILE_NAME);
                    if (env.exists()) return env;
                    dir = dir.getParentFile();
                }
            }
        }

        return cwdFile;
    }
}
