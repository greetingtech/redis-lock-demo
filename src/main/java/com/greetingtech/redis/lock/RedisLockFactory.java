package com.greetingtech.redis.lock;

import redis.clients.jedis.JedisPool;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public class RedisLockFactory {

    private JedisPool pool;

    private String prefixKey;

    private LockIdGenerator lockIdGenerator;

    public RedisLockFactory(JedisPool pool, String prefixKey) {
        this(pool, prefixKey, new DefaultLockIdGenerator());
    }

    public RedisLockFactory(JedisPool pool, String prefixKey, LockIdGenerator lockIdGenerator) {
        assert pool != null;
        this.pool = pool;
        this.prefixKey = prefixKey == null ? "" : prefixKey;
        this.lockIdGenerator = lockIdGenerator;
    }


    public RedisLock createLock(String key) {
        if (key == null || "".equals(key)) {
            throw new IllegalArgumentException("key can not be empty");
        }
        String realKey = prefixKey + key;
        RedisLock lock = new RedisLock(pool, lockIdGenerator.nextId(), realKey);
        return lock;
    }

}
