package com.java.redis.internal.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.java.redis.internal.constants.ReplyType;

public class RedisReply {
    private final ReplyType type;
    private final String string;             // for SIMPLE_STRING, ERROR, BULK_STRING
    private final long integer;              // for INTEGER
    private final List<RedisReply> children; // for ARRAY

    private RedisReply(ReplyType type, String string, long integer, List<RedisReply> children) {
        this.type = type;
        this.string = string;
        this.integer = integer;
        this.children = children;
    }

    public static RedisReply simpleString(String msg) {
        return new RedisReply(ReplyType.SIMPLE_STRING, msg, 0, null);
    }

    public static RedisReply ok() {
        return simpleString("OK");
    }

    public static RedisReply error(String msg) {
        return new RedisReply(ReplyType.ERROR, msg, 0, null);
    }

    public static RedisReply integer(long val) {
        return new RedisReply(ReplyType.INTEGER, null, val, null);
    }

    public static RedisReply bulkString(String msg) {
        if (msg == null) {
            return new RedisReply(ReplyType.BULK_STRING, null, 0, null);
        }
        return new RedisReply(ReplyType.BULK_STRING, msg, 0, null);
    }

    public static RedisReply nullBulk() {
        return new RedisReply(ReplyType.BULK_STRING, null, 0, null);
    }

    public static RedisReply array(List<RedisReply> elements) {
        return new RedisReply(ReplyType.ARRAY, null, 0, elements);
    }

    /** Encode this reply into RESP bytes */
    public byte[] toBytes() {
        switch (type) {
            case SIMPLE_STRING:
                return ("+" + string + "\r\n").getBytes(StandardCharsets.UTF_8);
            case ERROR:
                return ("-" + string + "\r\n").getBytes(StandardCharsets.UTF_8);
            case INTEGER:
                return (":" + integer + "\r\n").getBytes(StandardCharsets.UTF_8);
            case BULK_STRING:
                if (string == null) {
                    return "$-1\r\n".getBytes(StandardCharsets.UTF_8);
                } else {
                    byte[] data = string.getBytes(StandardCharsets.UTF_8);
                    String header = "$" + data.length + "\r\n";
                    byte[] headerB = header.getBytes(StandardCharsets.UTF_8);
                    byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);
                    byte[] out = new byte[headerB.length + data.length + crlf.length];
                    System.arraycopy(headerB, 0, out, 0, headerB.length);
                    System.arraycopy(data, 0, out, headerB.length, data.length);
                    System.arraycopy(crlf, 0, out, headerB.length + data.length, crlf.length);
                    return out;
                }
            case ARRAY:
                if (children == null) {
                    return "*-1\r\n".getBytes(StandardCharsets.UTF_8);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("*").append(children.size()).append("\r\n");
                byte[][] childBytes = new byte[children.size()][];
                int totalLen = sb.toString().getBytes(StandardCharsets.UTF_8).length;
                for (int i = 0; i < children.size(); i++) {
                    childBytes[i] = children.get(i).toBytes();
                    totalLen += childBytes[i].length;
                }
                byte[] result = new byte[totalLen];
                byte[] prefix = sb.toString().getBytes(StandardCharsets.UTF_8);
                System.arraycopy(prefix, 0, result, 0, prefix.length);
                int pos = prefix.length;
                for (byte[] cb : childBytes) {
                    System.arraycopy(cb, 0, result, pos, cb.length);
                    pos += cb.length;
                }
                return result;
            default:
                throw new IllegalStateException("Unknown reply type: " + type);
        }
    }
}
