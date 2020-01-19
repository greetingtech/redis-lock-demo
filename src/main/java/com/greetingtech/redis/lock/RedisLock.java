package com.greetingtech.redis.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public class RedisLock {

    private static final String UNLOCK_SCRIPT;

    private static final Long UNLOCK_SUCCESS = 1L;

    static {
        StringBuilder builder = new StringBuilder();
        builder.append("if redis.call('get', KEYS[1]) == ARGV[1] ");
        builder.append("then return redis.call('del', KEYS[1]) ");
        builder.append("else return 0 ");
        builder.append("end");
        UNLOCK_SCRIPT = builder.toString();
    }

    private final JedisPool pool;

    private final String key;

    private final LockIdGenerator lockIdGenerator;

    private final ThreadLocal<String> lockId = new ThreadLocal<>();

    RedisLock(JedisPool pool, LockIdGenerator lockIdGenerator, String key) {
        this.pool = pool;
        this.lockIdGenerator = lockIdGenerator;
        this.key = key;
    }

    public boolean tryLock(int secondsToExpire, long timeout) throws InterruptedException {
        if (lockId.get() != null) {
            throw new RedisLockException("not support re lock");
        }
        String newLockId = lockIdGenerator.nextId();
        try (Jedis resource = pool.getResource()) {
            SetParams setParams = new SetParams();
            setParams.nx().ex(secondsToExpire);
            long begin = System.currentTimeMillis();
            while (true) {
                String set = null;
                set = resource.set(key, newLockId, setParams);
                if (set != null) {
                    lockId.set(newLockId);
                    return true;
                }
                if (System.currentTimeMillis() - begin >= timeout) {
                    return false;
                }
                int sleepTime = ThreadLocalRandom.current().nextInt(100);
                Thread.sleep(sleepTime);
            }
        }
    }

    public void unlock() {
        String id = lockId.get();
        if (id == null) {
            throw new RedisLockException("not the lock owner");
        }
        try (Jedis resource = pool.getResource()) {
            Object evalResult = resource.eval(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(key),
                    Collections.singletonList(id)
            );
            if (evalResult != null && UNLOCK_SUCCESS.equals(evalResult)) {
                return;
            }
            throw new RedisLockException("not the lock owner");
        } finally {
            lockId.remove();
        }
    }

}
