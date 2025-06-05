package com.java.redis.internal.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
    final BufferedReader reader;
    public RespParser(InputStream in){
        this.reader = new BufferedReader(new InputStreamReader(in));
    }

    public Command parse() throws IOException{
        String line = reader.readLine();
        if(line == null){
            throw new IOException("Client closed connection");
        }
        if (!line.startsWith("*")) {
            throw new IOException("Expected RESP array, got "+ line);
        }
        int numArgs;
        try {
            numArgs = Integer.parseInt(line.substring(1));
        } catch (NumberFormatException e) {
            // TODO: handle exception
            throw new IOException("invalid Array length: "+ line + "\n"+e);
        }
        List<String> parts = new ArrayList<>(numArgs);

        for(int i=0; i<numArgs; i++){
            String dollar = reader.readLine();
            if(dollar == null || !dollar.startsWith("$")){
                throw new IOException("Expected bulk string length, got: " + dollar);
            }
            int len;
            try {
                len = Integer.parseInt(dollar.substring(1));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid bulk string length: " + dollar);
            }

            char[] buffer = new char[len];
            int read = reader.read(buffer, 0, len);
            if (read != len) {
                throw new IOException("Incomplete bulk string: expected " + len + ", got " + read);
            }
            reader.readLine();
            parts.add(new String(buffer));
        }
        if(parts.isEmpty()){
            throw new IOException("Empty command");
        }
        
        String name = parts.get(0).toUpperCase();
        List<String> args = parts.subList(1, parts.size());
        return new Command(name, args);
    }
}
