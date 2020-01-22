package com.greetingtech.redis.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public class RedisLock {

    private static final String UNLOCK_SCRIPT;

    private static final Long UNLOCK_SUCCESS = 1L;

    private static final ReentrantLock localLock = new ReentrantLock();

    static {
        String tempStr = "if redis.call('get', KEYS[1]) == ARGV[1] ";
        tempStr += "then return redis.call('del', KEYS[1]) ";
        tempStr += "else return 0 ";
        tempStr += "end";
        UNLOCK_SCRIPT = tempStr;
    }

    private final JedisPool pool;

    private final String key;

    private final String lockId;

    RedisLock(JedisPool pool, String lockId, String key) {
        this.pool = pool;
        this.key = key;
        this.lockId = lockId;
    }

    public boolean tryLock(int secondsToExpire, long timeout) {
        localLock.lock();
        try {
            try (Jedis resource = pool.getResource()) {
                SetParams setParams = new SetParams();
                setParams.nx().ex(secondsToExpire);
                long begin = System.currentTimeMillis();
                while (true) {
                    String set = resource.set(key, lockId, setParams);
                    if (set != null) {
                        return true;
                    }
                    if (System.currentTimeMillis() - begin >= timeout) {
                        localLock.unlock();
                        return false;
                    }
                    int sleepTime = ThreadLocalRandom.current().nextInt(100);
                    Thread.sleep(sleepTime);
                }
            }
        } catch (Throwable t) {
            localLock.unlock();
            return false;
        }
    }

    public void unlock() {
        if (!localLock.isHeldByCurrentThread()) {
            throw new RedisLockException("not the lock owner");
        }
        try (Jedis resource = pool.getResource()) {
            Object evalResult = resource.eval(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(key),
                    Collections.singletonList(lockId)
            );
            if (evalResult != null && UNLOCK_SUCCESS.equals(evalResult)) {
                return;
            }
            throw new RedisLockException("not the lock owner");
        } finally {
            localLock.unlock();
        }
    }

}
