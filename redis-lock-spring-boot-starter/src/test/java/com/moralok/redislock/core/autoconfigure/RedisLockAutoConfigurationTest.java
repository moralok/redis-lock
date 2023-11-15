package com.moralok.redislock.core.autoconfigure;

import com.moralok.redislock.core.RedisLockManager;
import com.moralok.redislock.core.RedisReentrantLock;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
@SpringBootTest(
        classes = RedisLockApplication.class,

        // Tried injecting config file in different ways but all failed finally.
        properties = {
                "redis-lock.host=192.168.46.135",
                "redis-lock.port=6379",
                "redis-lock.database=0"
        }
)
class RedisLockAutoConfigurationTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockAutoConfigurationTest.class);

    @Autowired
    private RedisLockManager redisLockManager;

    @Test
    public void testLock() {
        assertDoesNotThrow(() -> {
            RedisReentrantLock lock = redisLockManager.getLock("spring:test:1");
            try {
                lock.lock(3);
                logger.info("processing start...");
                logger.info("processing end");
            } finally {
                lock.unlock();
            }
        });
    }
}