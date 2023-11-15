package com.moralok.redislock.core;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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

    public static final String LOCK_PREFIX = "distributed_lock:";

    public static final int LOCK_EXPIRE = 30_000;

    public static final int RENEWAL_INTERVAL = 10_000;

    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final String RENEWAL_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "  return true " +
            "end " +
            "return false ";

    /**
     * redisClient
     */
    private RedisClient redisClient;

    /**
     * connection
     */
    private StatefulRedisConnection<String, String> connection;

    /**
     * commands
     */
    private RedisCommands<String, String> commands;

    /**
     * lockCache
     */
    private Map<String, RedisReentrantLock> lockCache = new ConcurrentHashMap<>();

    /**
     * scheduler
     */
    private ScheduledExecutorService scheduler;

    /**
     * Cache the SHA of unlock script
     */
    private String unlockScriptSha;

    /**
     * Cache the SHA of renewal script
     */
    private String renewalScriptSha;

    private volatile boolean shutdown = false;

    public RedisLockManager(RedisClient redisClient) {
        this.redisClient = redisClient;
        this.connection = redisClient.connect();
        this.commands = connection.sync();
        this.unlockScriptSha = commands.scriptLoad(UNLOCK_SCRIPT);
        this.renewalScriptSha = commands.scriptLoad(RENEWAL_SCRIPT);
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
        return lockCache.computeIfAbsent(lockKey, key -> new RedisReentrantLock(this, lockKey));
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

    public RedisCommands<String, String> getCommands() {
        return commands;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public String getUnlockScriptSha() {
        return unlockScriptSha;
    }

    public String getRenewalScriptSha() {
        return renewalScriptSha;
    }
}
