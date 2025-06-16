package com.java.redis.internal.datastore.value;

import com.java.redis.internal.constants.DataType;
import com.java.redis.internal.datastore.RedisValue;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SetValue implements RedisValue, Serializable {
    /** Using ConcurrentHashMap’s keySet for thread-safe set
     *
     */
    private final Set<String> set = ConcurrentHashMap.newKeySet();

    /**
     * SADD: add each member; returns count of new elements added.
     */
    public int sadd(Iterable<String> members) {
        int added = 0;
        for (String m : members) {
            if (set.add(m)) {
                added++;
            }
        }
        return added;
    }

    /**
     * SREM: remove each member; returns count of removed elements.
     */
    public int srem(Iterable<String> members) {
        int removed = 0;
        for (String m : members) {
            if (set.remove(m)) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * SMEMBERS: returns a snapshot of all members.
     */
    public Set<String> smembers() {
        // Return an immutable copy to avoid concurrent modification surprises
        return Collections.unmodifiableSet(Set.copyOf(set));
    }

}
