package com.java.redis.internal.datastore.value;

import com.java.redis.internal.constants.DataType;
import com.java.redis.internal.datastore.RedisValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ZSetValue implements RedisValue {
    // Map member → score for O(1) lookups
    private final ConcurrentHashMap<String, Double> scoreMap = new ConcurrentHashMap<>();
    // Sorted map score → set of members with that score, for range queries
    private final ConcurrentSkipListMap<Double, Set<String>> sorted = new ConcurrentSkipListMap<>();

    /**
     * ZADD: add or update member with score; returns 1 if new, 0 if updated existing.
     */
    public int zadd(double score, String member) {
        Double oldScore = scoreMap.put(member, score);
        if (oldScore != null) {
            // remove member from old score bucket
            sorted.computeIfPresent(oldScore, (s, members) -> {
                members.remove(member);
                return members.isEmpty() ? null : members;
            });
        }
        // add to new score bucket
        sorted.compute(score, (s, members) -> {
            if (members == null) members = ConcurrentHashMap.newKeySet();
            members.add(member);
            return members;
        });
        return (oldScore == null) ? 1 : 0;
    }

    /**
     * ZRANGE start..stop (inclusive, 0-based). Negative indices count from end.
     * Returns list of members in order.
     */
    public List<String> zrange(int start, int stop) {
        // flatten sorted map into a single list
        List<String> all = new ArrayList<>();
        for (Map.Entry<Double, Set<String>> e : sorted.entrySet()) {
            all.addAll(e.getValue());
        }
        int size = all.size();
        // handle negative indices
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;
        start = Math.max(0, start);
        stop = Math.min(size - 1, stop);
        if (start > stop || start >= size) return Collections.emptyList();
        return all.subList(start, stop + 1);
    }
}
