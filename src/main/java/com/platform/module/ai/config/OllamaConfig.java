package com.platform.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "platform.ai.ollama")
public class OllamaConfig {
    /** Ollama 服务基础地址 */
    private String baseUrl = "http://localhost:11434";
    /** 聊天模型名称 */
    private String chatModel = "qwen2.5:7b";
    /** 嵌入模型名称 */
    private String embeddingModel = "nomic-embed-text";
    /** 请求超时秒数 */
    private int timeoutSeconds = 120;
}
