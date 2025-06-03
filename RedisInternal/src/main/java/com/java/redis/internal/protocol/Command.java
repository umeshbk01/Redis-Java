package com.java.redis.internal.protocol;

import java.util.List;

//["SET","mykey","value"].
public class Command {
    private final String name;
    private final List<String> args;

    public Command(String name, List<String> args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public List<String> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return name + " " + args;
    }

}
