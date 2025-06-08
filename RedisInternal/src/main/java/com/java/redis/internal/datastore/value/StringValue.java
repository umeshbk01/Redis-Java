package com.java.redis.internal.datastore.value;

import com.java.redis.internal.constants.DataType;
import com.java.redis.internal.datastore.RedisValue;

public class StringValue implements RedisValue {
    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    /** Retrieve the stored string. */
    public String getValue() {
        return value;
    }
}
