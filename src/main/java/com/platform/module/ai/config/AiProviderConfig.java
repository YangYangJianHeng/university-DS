package com.platform.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "platform.ai")
public class AiProviderConfig {
    /** 当前激活的模型提供商（聊天用）: ollama / openai */
    private String provider = "ollama";
    /** 嵌入模型提供商（独立配置）: ollama / openai，默认跟随 provider */
    private String embedProvider = "ollama";
}
