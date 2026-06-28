package com.platform.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "platform.ai.openai")
public class OpenAIConfig {
    /** OpenAI 兼容 API 地址 */
    private String baseUrl = "https://api.openai.com/v1";
    /** API Key */
    private String apiKey = "";
    /** 对话模型 */
    private String chatModel = "gpt-4o-mini";
    /** 嵌入模型 */
    private String embeddingModel = "text-embedding-3-small";
}
