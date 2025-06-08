package com.java.redis.internal.datastore.value;

import com.java.redis.internal.constants.DataType;
import com.java.redis.internal.datastore.RedisValue;

import java.util.concurrent.ConcurrentHashMap;

public class HashValue implements RedisValue {
    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    /**
     * @return 1 if field is new, 0 if replacing an existing field
     */
    public int hset(String field, String val) {
        return map.put(field, val) == null ? 1 : 0;
    }

    /** @return the value for the field, or null if absent */
    public String hget(String field) {
        return map.get(field);
    }
}
