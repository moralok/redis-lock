package com.moralok.redislock;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * RedisLockManager
 *
 * @author moralok
 */
public class RedisLockManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockManager.class);

    /**
     * redisClient
     */
    private RedisClient redisClient;

    /**
     * connection
     */
    private StatefulRedisConnection<String, String> connection;

    /**
     * lockCache
     */
    private Map<String, RedisReentrantLock> lockCache = new ConcurrentHashMap<>();

    /**
     * scheduler
     */
    private ScheduledExecutorService scheduler;

    private volatile boolean shutdown = false;

    public RedisLockManager(RedisClient redisClient) {
        this.redisClient = redisClient;
        this.connection = redisClient.connect();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Get lock with the specified lockKey from the cache, create one if not exists and put it into the cache, return it.
     *
     * @param lockKey lockKey
     * @return the lock corresponding to the lockKey.
     */
    public RedisReentrantLock getLock(String lockKey) {
        if (shutdown) {
            throw new RedisLockException("This RedisLockManager had been shutdown already");
        }
        return lockCache.computeIfAbsent(lockKey, key -> new RedisReentrantLock(connection, lockKey, scheduler));
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        if (shutdown) {
            logger.info("This RedisLockManager had been shutdown already");
            return;
        }
        logger.info("RedisLockManager shutdown start...");
        shutdown = true;
        scheduler.shutdown();
        connection.close();
        redisClient.shutdown();
        logger.info("RedisLockManager shutdown end");
    }
}
