package com.aisaas.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    @Value("${spring.data.redis.mode:single}")
    private String redisMode;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:60000}")
    private int timeout;

    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.cluster.nodes:}")
    private String clusterNodes;

    /**
     * Redisson 客户端配置
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        log.info("Initializing Redisson client with mode: {}", redisMode);

        Config config = new Config();

        switch (redisMode.toLowerCase()) {
            case "single":
                config.useSingleServer()
                        .setAddress("redis://" + redisHost + ":" + redisPort)
                        .setDatabase(redisDatabase)
                        .setConnectTimeout(timeout)
                        .setTimeout(timeout);
                if (redisPassword != null && !redisPassword.isEmpty()) {
                    config.useSingleServer().setPassword(redisPassword);
                }
                break;

            case "sentinel":
                String[] sentinelNodes = redisHost.split(",");
                config.useSentinelServers()
                        .setMasterName(sentinelMaster)
                        .addSentinelAddress(sentinelNodes)
                        .setDatabase(redisDatabase)
                        .setReadMode(ReadMode.SLAVE)
                        .setSubscriptionMode(SubscriptionMode.SLAVE)
                        .setConnectTimeout(timeout)
                        .setTimeout(timeout);
                if (redisPassword != null && !redisPassword.isEmpty()) {
                    config.useSentinelServers().setPassword(redisPassword);
                }
                break;

            case "cluster":
                String[] nodes = clusterNodes.split(",");
                config.useClusterServers()
                        .addNodeAddress(nodes)
                        .setReadMode(ReadMode.SLAVE)
                        .setSubscriptionMode(SubscriptionMode.SLAVE)
                        .setConnectTimeout(timeout)
                        .setTimeout(timeout);
                if (redisPassword != null && !redisPassword.isEmpty()) {
                    config.useClusterServers().setPassword(redisPassword);
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported Redis mode: " + redisMode);
        }

        // 序列化配置
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        return Redisson.create(config);
    }

    /**
     * 配置 RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化器 - 使用 JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
