package com.java.redis.internal.datastore;

import com.java.redis.internal.datastore.value.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class DataStore {
    private final ConcurrentHashMap<String, ValueEntry> store = new ConcurrentHashMap<>();

    /** Check expiration lazily: if expired, remove and return true; else false. */
    private boolean removeIfExpired(String key, ValueEntry entry) {
        Long exp = entry.getExpirationTime();
        if(exp != null && System.currentTimeMillis() >= exp) {
            store.remove(key, entry); // remove only if it matches the current entry
            return true; // expired
        }
        return false; // not expired
    }

    private ValueEntry peekEntry(String key) {
        ValueEntry entry = store.get(key);
        if (entry == null || removeIfExpired(key, entry)) {
            return null; // entry is missing or expired
        }
        return entry; // valid entry
    }

    // ----- String Commands -----

    /** GET key: returns the string or null if missing */
    public String getString(String key) {
        ValueEntry entry = peekEntry(key);
        if (entry == null) {
            return null;
        }
        RedisValue v = entry.getValue();
        if (!(v instanceof StringValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((StringValue) v).getValue();
    }

    /** SET key value: always returns OK */
    public void setString(String key, String value, Long exSeconds) {
        Long expirationTime = null; // no expiration by default
        if(exSeconds != null) {
            expirationTime = System.currentTimeMillis() + exSeconds * 1000; // convert to milliseconds
        }
        store.put(key, new ValueEntry(new StringValue(value), expirationTime));
    }

    /** INCR key: atomically parse, increment, and store the new value */
    public long incr(String key) {
        while (true) {
            ValueEntry oldEntry = peekEntry(key);
            if (oldEntry == null) {
                // absent or expired: create new entry with value "1", no expiration
                ValueEntry newEntry = new ValueEntry(new StringValue("1"), 0);
                if (store.putIfAbsent(key, newEntry) == null) {
                    return 1L;
                }
                // else race: someone else inserted; retry
            } else {
                RedisValue oldVal = oldEntry.getValue();
                if (!(oldVal instanceof StringValue)) {
                    throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                String s = ((StringValue) oldVal).getValue();
                long curr;
                try {
                    curr = Long.parseLong(s);
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("ERR value is not an integer or out of range");
                }
                long next = curr + 1;
                ValueEntry newEntry = new ValueEntry(new StringValue(Long.toString(next)), oldEntry.getExpirationTime());
                // Use replace to ensure atomic update
                boolean replaced = store.replace(key, oldEntry, newEntry);
                if (replaced) {
                    return next;
                }
                // else retry
            }
        }
    }

    // ----- Hash Commands -----

    /**
     * HSET key field val: returns 1 if new field, 0 if updated existing
     */
    public int hset(String key, String field, String val) {
        RedisValue v = store.computeIfAbsent(key, k -> new HashValue());
        if (!(v instanceof HashValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((HashValue) v).hset(field, val);
    }

    /** HGET key field: returns the value or null if missing */
    public String hget(String key, String field) {
        RedisValue v = store.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof HashValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((HashValue) v).hget(field);
    }

    // ----- List Commands -----

    /**
     * LPUSH key values...
     * @return the new length of the list
     */
    public int lpush(String key, List<String> values) {
        RedisValue v = store.computeIfAbsent(key, k -> new ListValue());
        if (!(v instanceof ListValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((ListValue) v).lpush(values);
    }

    // ----- Set Commands -----

    /**
     * SADD key member [member...]: returns number of elements added.
     */
    public int sadd(String key, List<String> members) {
        RedisValue v = store.computeIfAbsent(key, k -> new SetValue());
        if (!(v instanceof SetValue)) {
            throw new IllegalStateException(
                    "WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((SetValue) v).sadd(members);
    }

    /**
     * SREM key member [member...]: returns number of elements removed.
     */
    public int srem(String key, List<String> members) {
        RedisValue v = store.get(key);
        if (v == null) return 0;
        if (!(v instanceof SetValue)) {
            throw new IllegalStateException(
                    "WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((SetValue) v).srem(members);
    }

    /**
     * SMEMBERS key: returns Set<String> or empty set.
     */
    public Set<String> smembers(String key) {
        RedisValue v = store.get(key);
        if (v == null) return Set.of();
        if (!(v instanceof SetValue)) {
            throw new IllegalStateException(
                    "WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((SetValue) v).smembers();
    }

// ----- Sorted Set Commands -----

    /**
     * ZADD key score member: returns 1 if new, 0 if updated.
     */
    public int zadd(String key, double score, String member) {
        RedisValue v = store.computeIfAbsent(key, k -> new ZSetValue());
        if (!(v instanceof ZSetValue)) {
            throw new IllegalStateException(
                    "WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((ZSetValue) v).zadd(score, member);
    }

    /**
     * ZRANGE key start stop: returns list of members in range.
     */
    public List<String> zrange(String key, int start, int stop) {
        RedisValue v = store.get(key);
        if (v == null) return List.of();
        if (!(v instanceof ZSetValue)) {
            throw new IllegalStateException(
                    "WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((ZSetValue) v).zrange(start, stop);

    }
}