package com.java.redis.internal.datastore;

import java.io.Serializable;

public class ValueEntry implements Serializable{
    private static final long serialVersionUID = 1L;

    private final RedisValue value;
    private final long expirationTime;
    
    public ValueEntry(RedisValue value, long expirationTime) {
        this.value = value;
        this.expirationTime = expirationTime;
    }
    public RedisValue getValue() {
        return value;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
    
}
