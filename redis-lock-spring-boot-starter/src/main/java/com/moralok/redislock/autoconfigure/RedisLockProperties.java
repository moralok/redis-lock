package com.moralok.redislock.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = RedisLockProperties.PREFIX)
public class RedisLockProperties {

    public static final String PREFIX = "redis-lock";

    /**
     * host
     */
    private String host = "localhost";

    /**
     * port
     */
    private int port = 6379;

    /**
     * database
     */
    private int database = 0;

    /**
     * username
     */
    private String username;

    /**
     * password
     */
    private String password;

    /**
     * wait time in milliseconds
     */
    private long waitTimeMillis;

    /**
     * leaseTime in milliseconds
     */
    private long leaseTimeMillis;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getWaitTimeMillis() {
        return waitTimeMillis;
    }

    public void setWaitTimeMillis(long waitTimeMillis) {
        this.waitTimeMillis = waitTimeMillis;
    }

    public long getLeaseTimeMillis() {
        return leaseTimeMillis;
    }

    public void setLeaseTimeMillis(long leaseTimeMillis) {
        this.leaseTimeMillis = leaseTimeMillis;
    }
}
