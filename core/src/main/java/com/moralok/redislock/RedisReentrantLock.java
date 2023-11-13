package com.moralok.redislock;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RedisReentrantLock
 *
 * @author moralok
 */
public class RedisReentrantLock {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisReentrantLock.class);

    private static final String LOCK_PREFIX = "distributed_lock:";

    private static final int LOCK_EXPIRE = 10_000;

    public static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                                               "  return redis.call('del', KEYS[1]) " +
                                               "else " +
                                               "  return 0 " +
                                               "end";

    private ThreadLocal<Integer> lockCount = ThreadLocal.withInitial(() -> 0);

    private ThreadLocal<String> lockValue = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private AtomicReference<String> UNLOCK_SCRIPT_SHA = new AtomicReference<>();

    private StatefulRedisConnection<String, String> connection;

    private RedisCommands<String, String> commands;

    private String lockKey;

    public RedisReentrantLock(StatefulRedisConnection<String, String> connection, String lockKey) {
        this.connection = connection;
        this.commands = connection.sync();
        this.lockKey = LOCK_PREFIX + lockKey;
        this.UNLOCK_SCRIPT_SHA.compareAndSet(null, commands.scriptLoad(UNLOCK_SCRIPT));
    }

    public void lock(int retryTimes) {
        for (int i = 0; i < retryTimes; i++) {
            if (tryLock()) {
                return;
            }
            try {
                logger.debug("Failed to lock [{}] for [{}] times, total times [{}]", lockKey, i, retryTimes);
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisLockException("Interrupted while waiting for the lock", e);
            }
        }
        throw new RedisLockException("Can not acquire lock: " + lockKey);
    }

    public void lock(long timeout, TimeUnit unit) {
        long deadline = (timeout > 0) ? System.currentTimeMillis() + unit.toMillis(timeout) + 1 : Long.MAX_VALUE;
        while (true) {
            long now = System.currentTimeMillis();
            if (now >= deadline) {
                throw new RedisLockException("Failed to acquire lock within the specified timeout");
            }
            if (tryLock()) {
                return;
            }
            try {
                logger.debug("Failed to lock [{}] at {}, deadline [{}]", lockKey, now, deadline);
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisLockException("Interrupted while waiting for the lock", e);
            }
        }
    }

    public boolean tryLock() {
        if (lockCount.get() > 0) {
            lockCount.set(lockCount.get() + 1);
            logger.debug("Get lock again [{}], lockValue[{}], lockCount[{}]", lockKey, lockValue.get(), lockCount.get());
            return true;
        }
        String result = commands.set(lockKey, lockValue.get(), SetArgs.Builder.nx().px(LOCK_EXPIRE));
        if ("OK".equals(result)) {
            lockCount.set(lockCount.get() + 1);
            logger.debug("Get lock[{}], lockValue[{}], lockCount[{}]", lockKey, lockValue.get(), lockCount.get());
            return true;
        }
        return false;
    }

    public void unlock() {
        logger.debug("Unlock lockKey[{}], lockValue[{}] lockCount[{}]", lockKey, lockValue.get(), lockCount.get());
        if (lockCount.get() > 0) {
            lockCount.set(lockCount.get() - 1);
        }
        if (lockCount.get().equals(0)) {
            String currentValue = commands.get(lockKey);
            if (lockValue.get().equals(currentValue)) {
                logger.debug("Delete lockKey[{}], value[{}]", lockKey, currentValue);
                commands.evalsha(UNLOCK_SCRIPT_SHA.get(), ScriptOutputType.INTEGER, new String[]{lockKey}, currentValue);
            }
            logger.debug("Try to unlock other's lock, lockValue[{}], current[{}]", lockValue.get(), currentValue);
        }
    }
}
