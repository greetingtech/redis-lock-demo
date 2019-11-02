package com.greetingtech.redis.lock;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public interface LockIdGenerator {

    String nextId();

}
