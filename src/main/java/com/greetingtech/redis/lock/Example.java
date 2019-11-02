package com.greetingtech.redis.lock;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public class Example {

    private static int count = 0;

    public static void main(String[] args) throws Exception {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(64);
        config.setMaxIdle(64);
        config.setMinIdle(8);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        config.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        config.setNumTestsPerEvictionRun(3);
        config.setBlockWhenExhausted(true);

        try (JedisPool pool = new JedisPool(config, "localhost")) {
            RedisLockFactory factory = new RedisLockFactory(pool, "locks-");
            RedisLock lock = factory.createLock("lock-test");
            final int threadCount = 16;
            CountDownLatch latch = new CountDownLatch(threadCount);
            for (int i = 0; i < threadCount; ++i) {
                test(lock, latch);
            }
            latch.await();
        }


    }

    private static void test(RedisLock lock, CountDownLatch latch) throws Exception {
        Thread thread = new Thread(() -> {
            try {
                boolean b = lock.tryLock(60, 10000);
                if (b) {
                    try {
                        Thread.sleep(100);
                        ++count;
                        System.out.println("success " + count);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        thread.start();
    }

}
