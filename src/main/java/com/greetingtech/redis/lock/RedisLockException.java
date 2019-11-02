package com.greetingtech.redis.lock;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public class RedisLockException extends RuntimeException {

    public RedisLockException() {
    }

    public RedisLockException(String message) {
        super(message);
    }
}
