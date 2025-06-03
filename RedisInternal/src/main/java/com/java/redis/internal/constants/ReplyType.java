package com.java.redis.internal.constants;

public enum ReplyType {
    SIMPLE_STRING,    // +OK\r\n
    BULK_STRING,      // $<len>\r\n<payload>\r\n
    ERROR             // -ERR <message>\r\n
    // (int, arrays, etc. can be added later)
}
