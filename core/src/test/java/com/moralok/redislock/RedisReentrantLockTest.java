package com.moralok.redislock;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class RedisReentrantLockTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisReentrantLockTest.class);

    private RedisLockManager redisLockManager;

    @BeforeEach
    void before() {
        RedisClient redisClient = RedisClient.create("redis://192.168.46.135:6379/0");
        redisLockManager = new RedisLockManager(redisClient);
    }

    @AfterEach
    void afterEach() {
        redisLockManager.shutdown();
    }

    @Test
    void lock() {
        RedisReentrantLock lock = redisLockManager.getLock("user:1");
        try {
            lock.lock(3);
            logger.debug("processing start");
            TimeUnit.SECONDS.sleep(2);
            logger.debug("processing end");
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Test
    void lock1() {
        RedisReentrantLock lock = redisLockManager.getLock("user:1");
        long start = System.currentTimeMillis();
        int count = 10000;
        for (int i = 0; i < count; i++) {
            try {
                lock.lock(3);
                logger.debug("processing start");
                logger.debug("processing end");
            } catch (Exception e) {
                logger.debug("Exception: {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        }
        long end = System.currentTimeMillis();
        logger.debug("cost: {}ms, avg: {}ms", end - start, (end - start) / count);
    }

    @Test
    void lock2() throws InterruptedException {
        RedisReentrantLock lock = redisLockManager.getLock("user:1");
        Thread t1 = new Thread(() -> {
            try {
                lock.lock(3);
                logger.debug("processing start");
                TimeUnit.SECONDS.sleep(1);
                logger.debug("processing end");
            } catch (Exception e) {
                logger.debug("Exception: {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            try {
                lock.lock(3);
                logger.debug("processing start");
                TimeUnit.SECONDS.sleep(1);
                logger.debug("processing end");
            } catch (Exception e) {
                logger.debug("Exception: {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        }, "t2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}