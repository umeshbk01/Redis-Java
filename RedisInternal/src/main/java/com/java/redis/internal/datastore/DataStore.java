package com.java.redis.internal.datastore;

import com.java.redis.internal.datastore.value.*;

import java.util.*;
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
                ValueEntry newEntry = new ValueEntry(new StringValue("1"), null);
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
        while (true) {
            ValueEntry entry = peekEntry(key);
            if(entry == null){
                HashValue map = new HashValue();
                map.hset(field, val);
                ValueEntry newEntry = new ValueEntry(map, null);// no expiration
                if(store.putIfAbsent(key, newEntry)==null){
                    return 1;
                }
            }else{
                RedisValue v =entry.getValue();
                if(!(v instanceof HashValue)) {
                    throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                HashValue map = (HashValue) v;
                int added = map.hset(field, val);
                return added;
            }
        }
    }

    /** HGET key field: returns the value or null if missing */
    public String hget(String key, String field) {
        ValueEntry entry = peekEntry(key);
        if (entry == null) {
            return null; // key is missing or expired
        }
        RedisValue v = entry.getValue();
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
        while(true){
            ValueEntry entry = peekEntry(key);
            if(entry == null){
                // key does not exist or expired, create a new list
                ListValue list = new ListValue();
                list.lpush(values);
                ValueEntry newEntry = new ValueEntry(list, null); // no expiration
                if(store.putIfAbsent(key, newEntry) == null) {
                    return  list.lpush(Collections.emptyList()) + values.size(); // successfully created
                }
            } else {
                RedisValue v = entry.getValue();
                if (!(v instanceof ListValue)) {
                    throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                ListValue list = (ListValue) v;
                return list.lpush(values); // push to existing list
            }
        }
    }

    // ----- Set Commands -----

    /**
     * SADD key member [member...]: returns number of elements added.
     */
    public int sadd(String key, List<String> members) {
        while(true){
            ValueEntry entry = peekEntry(key);
            if(entry == null){
                // key does not exist or expired, create a new set
                SetValue set = new SetValue();
                int added = set.sadd(members);
                ValueEntry newEntry = new ValueEntry(set, null); // no expiration
                if(store.putIfAbsent(key, newEntry) == null) {
                    return added; // successfully created
                }
            } else {
                RedisValue v = entry.getValue();
                if (!(v instanceof SetValue)) {
                    throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                SetValue set = (SetValue) v;
                return set.sadd(members); // add to existing set
            }
        }
    }

    /**
     * SREM key member [member...]: returns number of elements removed.
     */
    public int srem(String key, List<String> members) {
        ValueEntry entry = peekEntry(key);
        if (entry == null) {
            return 0; // key does not exist or expired
        }
        if(!(entry.getValue() instanceof SetValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        SetValue set = (SetValue) entry.getValue();
        return set.srem(members); // remove from existing set
    }

    /**
     * SMEMBERS key: returns Set<String> or empty set.
     */
    public Set<String> smembers(String key) {
        ValueEntry entry = peekEntry(key);
        if (entry == null) {
            return Collections.emptySet(); // key does not exist or expired
        }
        RedisValue v = entry.getValue();
        if(!(v instanceof SetValue)) {
            throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return ((SetValue) v).smembers(); // return members of the set
    }

// ----- Sorted Set Commands -----

    /**
     * ZADD key score member: returns 1 if new, 0 if updated.
     */
    public int zadd(String key, double score, String member) {
        while (true){
            ValueEntry entry = peekEntry(key);
            if (entry == null) {
                // key does not exist or expired, create a new sorted set
                ZSetValue zset = new ZSetValue();
                int added = zset.zadd(score, member);
                ValueEntry newEntry = new ValueEntry(zset, null); // no expiration
                if (store.putIfAbsent(key, newEntry) == null) {
                    return added; // successfully created
                }
            } else {
                RedisValue v = entry.getValue();
                if (!(v instanceof ZSetValue)) {
                    throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }
                ZSetValue zset = (ZSetValue) v;
                return zset.zadd(score, member); // add to existing sorted set
            }
        }
    }

    /**
     * ZRANGE key start stop: returns list of members in range.
     */
    public List<String> zrange(String key, int start, int stop) {
        ValueEntry entry = peekEntry(key);
            if (entry == null) {
                return Collections.emptyList(); // key does not exist or expired
            }
            RedisValue v = entry.getValue();
            if (!(v instanceof ZSetValue)) {
                throw new IllegalStateException("WRONGTYPE Operation against a key holding the wrong kind of value");
            }
            return ((ZSetValue) v).zrange(start, stop); // return range of members
    }


    //TTL Commands
    /**
     * EXPIRE key seconds: set expiration. Returns:
     * - 1 if timeout set,
     * - 0 if key does not exist.
     */
    public int expire(String key, long seconds) {
        ValueEntry entry = peekEntry(key);
        if (entry == null) {
            return 0; // key does not exist or expired
        }
        Long expireAt = System.currentTimeMillis() + seconds * 1000; // convert to milliseconds

        ValueEntry newEntry = new ValueEntry(entry.getValue(), expireAt);
        store.replace(key, entry, newEntry);
        return 1;
    }

    /**
     * TTL key: return remaining seconds:
     * - if key does not exist: -2
     * - if key exists but no expire: -1
     * - else remaining seconds (rounded down)
     */
    public  long ttl(String key) {
        ValueEntry entry = peekEntry(key);
        if (entry == null) {
            return -2; // key does not exist or expired
        }
        Long exp = entry.getExpirationTime();
        if (exp == null) {
            return -1; // no expiration set
        }
        long remaining = (exp - System.currentTimeMillis()) / 1000; // convert to seconds
        return Math.max(0, remaining); // ensure non-negative
    }

    //For Persistence
    /** Expose the internal map for serialization. */
    public Map<String, ValueEntry> getStore() {
        // Return a shallow copy to avoid concurrency issues during serialization
        return new HashMap<>(store);
    }

    /** Load state from a given map (during startup). */
    public void loadSnapshot(Map<String, ValueEntry> snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot cannot be null");
        }
        store.clear(); // clear existing state
        store.putAll(snapshot); // load new state
    }

}