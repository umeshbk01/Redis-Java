package com.java.redis.internal.datastore.value;

import com.java.redis.internal.constants.DataType;
import com.java.redis.internal.datastore.RedisValue;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;

/**
 * ListValue represents a Redis list data structure.
 * This implementation uses a LinkedList to allow efficient push operations.
 */

public class ListValue implements RedisValue, Serializable {
    private final LinkedList<String> list = new LinkedList<>();

    /**
     * LPUSH semantics: pushes all values to the head in order.
     * @return new length of the list
     */
    public int lpush(List<String> values) {
        // values are in the order they appear in the command,
        // but LPUSH pushes them one by one so the first in that list
        // becomes the headmost.
        for (String v : values) {
            list.addFirst(v);
        }
        return list.size();
    }

}
