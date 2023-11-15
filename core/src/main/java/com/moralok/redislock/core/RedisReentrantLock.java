package com.moralok.redislock.core;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * RedisReentrantLock
 *
 * @author moralok
 */
public class RedisReentrantLock {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisReentrantLock.class);

    private RedisLockManager redisLockManager;

    private ThreadLocal<Integer> lockCount = new ThreadLocal<>();

    private ThreadLocal<String> lockValue = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private ThreadLocal<ScheduledFuture<?>> renewalTask = new ThreadLocal<>();

    private RedisCommands<String, String> commands;

    private String lockKey;

    public RedisReentrantLock(RedisLockManager redisLockManager, String lockKey) {
        this.redisLockManager = redisLockManager;
        this.commands = redisLockManager.getCommands();
        this.lockKey = RedisLockManager.LOCK_PREFIX + lockKey;
    }

    public void lock(int retryTimes) {
        for (int i = 0; i < retryTimes; i++) {
            if (tryLock()) {
                return;
            }
            try {
                logger.debug("Failed to lock [{}] for [{}] times, total times [{}]", lockKey, i + 1, retryTimes);
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
        int retryTimes = 0;
        while (true) {
            retryTimes++;
            long now = System.currentTimeMillis();
            if (now >= deadline) {
                throw new RedisLockException("Failed to acquire lock within the specified timeout");
            }
            if (tryLock()) {
                return;
            }
            try {
                logger.debug("Failed to lock [{}] at [{}] for [{}] times, deadline [{}]", lockKey, now, retryTimes, deadline);
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisLockException("Interrupted while waiting for the lock", e);
            }
        }
    }

    public boolean tryLock() {
        Integer count = lockCount.get();
        if (count != null && count > 0) {
            lockCount.set(count + 1);
            logger.debug("Get lock again [{}], lockValue[{}], lockCount[{}]", lockKey, lockValue.get(), lockCount.get());
            return true;
        }
        String result = commands.set(lockKey, lockValue.get(), SetArgs.Builder.nx().px(RedisLockManager.LOCK_EXPIRE));
        if ("OK".equals(result)) {
            lockCount.set(1);
            logger.debug("Get lock[{}], lockValue[{}], lockCount[{}]", lockKey, lockValue.get(), lockCount.get());
            scheduleRenewal();
            return true;
        }
        return false;
    }

    public void unlock() {
        Integer count = lockCount.get();
        if (count == null) {
            return;
        }
        logger.debug("Unlock lockKey[{}], lockValue[{}] lockCount[{}]", lockKey, lockValue.get(), count);
        if (count > 0) {
            count--;
        }
        if (count.equals(0)) {
            // Regardless of whether you hold the lock, try to cancel the task.
            cancelRenewal();
            String currentValue = commands.get(lockKey);
            if (lockValue.get().equals(currentValue)) {
                logger.debug("Delete lockKey[{}], value[{}]", lockKey, currentValue);
                commands.evalsha(redisLockManager.getUnlockScriptSha(), ScriptOutputType.INTEGER, new String[]{lockKey}, currentValue);
            } else {
                logger.warn("Try to unlock other's lock, lockValue[{}], current[{}]", lockValue.get(), currentValue);
            }
            lockCount.remove();
        } else {
            lockCount.set(count);
        }
    }

    private void scheduleRenewal() {
        String value = lockValue.get();
        logger.debug("Schedule renewal task for lockKey[{}], lockValue[{}] lockCount[{}]", lockKey, value, lockCount.get());
        // Do not use lockValue.get() in task, otherwise the wrong value will be obtained.
        ScheduledFuture<?> scheduledFuture = redisLockManager.getScheduler().scheduleAtFixedRate(() -> this.renewal(value), RedisLockManager.RENEWAL_INTERVAL, RedisLockManager.RENEWAL_INTERVAL, TimeUnit.MILLISECONDS);
        renewalTask.set(scheduledFuture);
    }

    private void renewal(String value) {
        logger.debug("Renewal lockKey[{}], lockValue[{}] lockCount[{}]", lockKey, value, lockCount.get());
        commands.evalsha(redisLockManager.getRenewalScriptSha(), ScriptOutputType.BOOLEAN, new String[]{lockKey}, value, String.valueOf(RedisLockManager.LOCK_EXPIRE));
    }

    private void cancelRenewal() {
        ScheduledFuture<?> scheduledFuture = renewalTask.get();
        if (scheduledFuture != null) {
            logger.debug("Cancel renewal task for lockKey[{}], lockValue[{}]", lockKey, lockValue.get());
            scheduledFuture.cancel(false);
            renewalTask.remove();
        }
    }
}
