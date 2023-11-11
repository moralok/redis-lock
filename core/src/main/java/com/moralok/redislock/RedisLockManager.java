package com.moralok.redislock;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisLockManager {

    private RedisClient redisClient;

    private StatefulRedisConnection<String, String> connection;

    private Map<String, RedisReentrantLock> lockCache = new ConcurrentHashMap<>();

    public RedisLockManager(RedisClient redisClient) {
        this.redisClient = redisClient;
        this.connection = redisClient.connect();
    }

    public RedisReentrantLock getLock(String lockKey) {
        return lockCache.computeIfAbsent(lockKey, key -> new RedisReentrantLock(connection, lockKey));
    }

    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }
}
