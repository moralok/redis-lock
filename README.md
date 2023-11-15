# redis-lock

## 特性

一款基于 Redis 实现的分布式锁，拥有以下特性：

- 操作原子性，加锁、解锁和续期等操作为原子操作。
- 防止锁悬挂，避免服务宕机未解锁造成死锁。
- 防止锁误删，避免错误释放其他线程持有的锁。
- 支持锁等待，允许使用次数或超时时间进行重试。
- 支持锁续期，避免业务处理超时造成锁过期自动释放。
- 支持可重入，允许同一线程多次加锁。

## 使用方式

### 手动管理

1. 引入依赖

    ```xml
    <dependency>
        <groupId>com.moralok.redis-lock</groupId>
        <artifactId>core</artifactId>
        <version>${version}</version>
    </dependency>
    ```

2. 使用

   ```java
   public class Test {
       public static void main(String[] args) {
           RedisClient redisClient = RedisClient.create("redis://192.168.46.135:6379/0");
           RedisLockManager redisLockManager = new RedisLockManager(redisClient);
           RedisReentrantLock lock = redisLockManager.getLock("lockKey");
           try {
               lock.lock(3);
               logger.info("processing start...");
               logger.info("processing end");
           } finally {
               lock.unlock();
           }
       }
   }
   ```

### 使用 redis-lock-spring-boot-starter

1. 引入依赖

    ```xml
    <dependency>
        <groupId>com.moralok.redis-lock</groupId>
        <artifactId>redis-lock-spring-boot-starter</artifactId>
        <version>${version}</version>
    </dependency>
    ```

2. 在代码中使用

    ```java
    @RestController
    public class TestController {
    
        @Autowired
        private RedisLockManager redisLockManager;
    
        @GetMapping("/test")
        public String test() {
            RedisReentrantLock lock = redisLockManager.getLock("test:lock");
            try {
                lock.lock(3);
            } finally {
                lock.unlock();
            }
            return "success";
        }
    }
    ```

