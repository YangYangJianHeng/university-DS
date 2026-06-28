package com.platform.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "platform.ai.chat")
public class ChatConfig {
    /** 每个用户最大保留的聊天轮次 */
    private int maxRounds = 20;
    /** 聊天压缩触发阈值 */
    private int compressTrigger = 15;
    /** 压缩后保留的最近轮次 */
    private int compressKeepRecent = 5;
}
