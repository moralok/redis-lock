package com.moralok.redislock;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class RedisReentrantLockTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisReentrantLockTest.class);

    private RedisClient redisClient;
    private RedisLockManager redisLockManager;
    private static final String lockKey = "user:1";

    @BeforeEach
    void before() {
        redisClient = RedisClient.create("redis://192.168.46.135:6379/0");
        redisLockManager = new RedisLockManager(redisClient);
    }

    @AfterEach
    void afterEach() {
        redisLockManager.shutdown();
    }

    @Test
    void lockWithRetry() {
        Assertions.assertDoesNotThrow(this::_lockWithRetry);
    }

    @Test
    void lockWithTimeout() {
        Assertions.assertDoesNotThrow(this::_lockWithTimeout);
    }

    @Test()
    void reentrantLock() {
        Assertions.assertDoesNotThrow(() -> _reentrantLock(3));
    }

    @Test
    void failToLockAfterRetry() throws InterruptedException {
        final Throwable[] throwable = new Throwable[1];
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> throwable[0] = e;
        Thread t1 = new Thread(this::_lockFor200msWithRetry, "t1");
        Thread t2 = new Thread(this::_lockFor200msWithRetry, "t2");
        t1.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t2.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assertions.assertThrows(RedisLockException.class, () -> {
            throw throwable[0];
        }, "Lock contention occurs, a RedisLockException should be thrown.");
    }

    @Test
    void succeedToLockAfterRetry() throws InterruptedException {
        final Throwable[] throwable = new Throwable[1];
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> throwable[0] = e;
        Thread t1 = new Thread(this::_lockWithRetry, "t1");
        Thread t2 = new Thread(this::_lockWithRetry, "t2");
        t1.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t2.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assertions.assertNull(throwable[0]);
    }

    @Test
    void failToLockAfterTimeout() throws InterruptedException {
        final Throwable[] throwable = new Throwable[1];
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> throwable[0] = e;
        Thread t1 = new Thread(this::_lockFor200msWithTimeout, "t1");
        Thread t2 = new Thread(this::_lockFor200msWithTimeout, "t2");
        t1.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t2.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assertions.assertThrows(RedisLockException.class, () -> {
            throw throwable[0];
        }, "Lock contention occurs, a RedisLockException should be thrown.");
    }

    @Test
    void succeedToLockAfterTimeout() throws InterruptedException {
        final Throwable[] throwable = new Throwable[1];
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> throwable[0] = e;
        Thread t1 = new Thread(this::_lockWithTimeout, "t1");
        Thread t2 = new Thread(this::_lockWithTimeout, "t2");
        t1.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t2.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assertions.assertNull(throwable[0]);
    }

    @Test
    void renewalLock() {
        Thread t1 = new Thread(this::_lockFor40sWithRetry, "t1");
        t1.start();
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertThrows(RedisLockException.class, this::_lockWithRetry);
        try {
            t1.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void _lockWithRetry() {
        RedisReentrantLock lock = redisLockManager.getLock(lockKey);
        try {
            lock.lock(3);
            logger.info("processing start...");
            logger.info("processing end");
        } finally {
            lock.unlock();
        }
    }

    private void _lockWithTimeout() {
        RedisReentrantLock lock = redisLockManager.getLock(lockKey);
        try {
            lock.lock(200, TimeUnit.MILLISECONDS);
            logger.info("processing start...");
            logger.info("processing end");
        } finally {
            lock.unlock();
        }
    }

    private void _reentrantLock(int times) {
        if (times <= 0) {
            return;
        }
        RedisReentrantLock lock = redisLockManager.getLock(lockKey);
        try {
            lock.lock(3);
            logger.info("processing start...times[{}]", times);
            _reentrantLock(times - 1);
            logger.info("processing end...times[{}]", times);
        } finally {
            lock.unlock();
        }
    }

    private void _lockFor200msWithRetry() {
        RedisReentrantLock lock = redisLockManager.getLock(lockKey);
        try {
            lock.lock(3);
            logger.info("processing start...");
            TimeUnit.MILLISECONDS.sleep(200);
            logger.info("processing end");
        } catch (InterruptedException e) {
            logger.info("InterruptedException: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void _lockFor200msWithTimeout() {
        RedisReentrantLock lock = redisLockManager.getLock(lockKey);
        try {
            lock.lock(150, TimeUnit.MILLISECONDS);
            logger.info("processing start...");
            TimeUnit.MILLISECONDS.sleep(200);
            logger.info("processing end");
        } catch (InterruptedException e) {
            logger.info("InterruptedException: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void _lockFor40sWithRetry() {
        RedisReentrantLock lock = redisLockManager.getLock(lockKey);
        try {
            lock.lock(3);
            logger.info("processing start...");
            TimeUnit.SECONDS.sleep(40);
            logger.info("processing end");
        } catch (InterruptedException e) {
            logger.info("InterruptedException: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}