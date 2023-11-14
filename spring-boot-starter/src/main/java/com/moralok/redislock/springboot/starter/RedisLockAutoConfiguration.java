package com.moralok.redislock.springboot.starter;

import com.moralok.redislock.RedisLockManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Configurable
@EnableConfigurationProperties(RedisLockProperties.class)
public class RedisLockAutoConfiguration {

    @Autowired
    private RedisLockProperties redisLockProperties;

    @Bean
    @ConditionalOnMissingBean(RedisClient.class)
    public RedisClient redisClient() {
        RedisURI redisURI = new RedisURI();
        redisURI.setHost(redisLockProperties.getHost());
        redisURI.setPort(redisLockProperties.getPort());
        redisURI.setDatabase(redisLockProperties.getDatabase());
        if (redisLockProperties.getUsername() != null) {
            redisURI.setUsername(redisLockProperties.getUsername());
        }
        if (redisLockProperties.getPassword() != null) {
            redisURI.setUsername(redisLockProperties.getPassword());
        }
        return RedisClient.create(redisURI);
    }

    @Bean
    @ConditionalOnMissingBean(RedisLockManager.class)
    public RedisLockManager redisLockManager(RedisClient redisClient) {
        return new RedisLockManager(redisClient);
    }
}
