package com.moralok.redislock;

public class RedisLockException extends RuntimeException {

    public RedisLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisLockException(String message) {
        super(message);
    }
}
