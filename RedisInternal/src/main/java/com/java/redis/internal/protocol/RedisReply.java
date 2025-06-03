package com.java.redis.internal.protocol;

import java.nio.charset.StandardCharsets;

import com.java.redis.internal.constants.ReplyType;

public class RedisReply {
    private final String payload;
    private final ReplyType type;
    
    private RedisReply(ReplyType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public static RedisReply ok() {
        return new RedisReply(ReplyType.SIMPLE_STRING, "OK");
    }

    public static RedisReply error(String msg) {
        return new RedisReply(ReplyType.ERROR, "ERR " + msg);
    }
    public static RedisReply bulkString(String msg) {
        return new RedisReply(ReplyType.BULK_STRING, msg);
    }

    public byte[] toByte(){
        switch(type){
            case SIMPLE_STRING:
                return ("+" + payload + "\r\n").getBytes(StandardCharsets.UTF_8);
            case ERROR:
                return ("+" + payload + "\r\n").getBytes(StandardCharsets.UTF_8);
            case BULK_STRING:
                byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                byte[] headerBytes = ("$" + body.length + "\r\n").getBytes(StandardCharsets.UTF_8);
                byte[] clrf = "\r\n".getBytes(StandardCharsets.UTF_8);
                byte[] result = new Byte[headerBytes.length + body.length + crlf.length];
                System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
                System.arraycopy(body, 0, result, headerBytes.length, body.length);
                System.arraycopy(crlf, 0, result, headerBytes.length + body.length, crlf.length);
                return result;
            default:
                return ("-ERR Unknown reply type\r\n")
                        .getBytes(StandardCharsets.UTF_8);
        }
    }
}
