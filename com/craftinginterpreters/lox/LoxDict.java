package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * A native dictionary object for Lox, backed by a Java HashMap.
 * Supports put, get, remove, and toString.
 */
public class LoxDict {
    private final Map<Object, Object> map;

    public LoxDict() {
        this.map = new HashMap<>();
    }

    // Associates the specified value with the specified key
    public void put(Object key, Object value) {
        map.put(key, value);
    }

    // Retrieves the value associated with the given key
    public Object get(Object key) {
        return map.get(key);
    }

    // Removes the entry associated with the given key
    public void remove(Object key) {
        map.remove(key);
    }

    // Returns a string representation of the dictionary
    @Override
    public String toString() {
        return map.toString();
    }
}
