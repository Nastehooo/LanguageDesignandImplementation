package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxDict {
    private final Map<Object, Object> map;

    public LoxDict() {
        this.map = new HashMap<>();
    }

    public LoxDict(Map<Object, Object> map) {
        this.map = map;
    }

    public void put(Object key, Object value) {
        map.put(key, value);
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public void remove(Object key) {
        map.remove(key);
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
