package com.platform.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    // ===== 缓存区名称常量 =====
    /** 平台公开统计：30分钟 */
    public static final String CACHE_PUBLIC_STATS = "publicStats";
    /** 竞赛列表/推荐/日历：10分钟 */
    public static final String CACHE_COMPETITION = "competition";
    /** 招募列表/详情：5分钟 */
    public static final String CACHE_RECRUIT = "recruit";
    /** 成果分享列表/统计：10分钟 */
    public static final String CACHE_ACHIEVEMENT = "achievement";
    /** 管理后台仪表盘/公告：5-10分钟 */
    public static final String CACHE_ADMIN = "admin";
    /** 徽章列表：30分钟 */
    public static final String CACHE_BADGE = "badge";
    /** 用户仪表盘：5分钟 */
    public static final String CACHE_USER_DASHBOARD = "userDashboard";
    /** 教师列表：10分钟 */
    public static final String CACHE_TEACHER = "teacher";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        valueSerializer.setObjectMapper(om);

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * 多缓存区 TTL 配置：
     * 高频变动的数据（招募）→ 5min，
     * 列表类（竞赛/教师/成果）→ 10min，
     * 静态数据（徽章/统计）→ 30min，
     * 用户个人仪表盘 → 5min
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        valueSerializer.setObjectMapper(om);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_PUBLIC_STATS,     ttlConfig(valueSerializer, Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_COMPETITION,      ttlConfig(valueSerializer, Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_RECRUIT,          ttlConfig(valueSerializer, Duration.ofMinutes(5)));
        cacheConfigs.put(CACHE_ACHIEVEMENT,      ttlConfig(valueSerializer, Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_ADMIN,            ttlConfig(valueSerializer, Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_BADGE,            ttlConfig(valueSerializer, Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_USER_DASHBOARD,   ttlConfig(valueSerializer, Duration.ofMinutes(5)));
        cacheConfigs.put(CACHE_TEACHER,          ttlConfig(valueSerializer, Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private RedisCacheConfiguration ttlConfig(Jackson2JsonRedisSerializer<Object> valueSerializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();
    }
}
