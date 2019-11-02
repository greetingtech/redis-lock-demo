package com.greetingtech.redis.lock;

import java.util.UUID;

/**
 * @author greetingtech
 * @date 2019-11-02
 */
public class DefaultLockIdGenerator implements LockIdGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }

}
