package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * A native list object for Lox, backed by a Java ArrayList.
 * Supports push, get by index, remove by index, and size.
 */
public class LoxList {
    private final List<Object> elements;

    public LoxList() {
        this.elements = new ArrayList<>();
    }

    // Appends a value to the end of the list
    public void add(Object value) {
        elements.add(value);
    }

    // Gets the value at a specific index
    public Object get(int index) {
        return elements.get(index);
    }

    // Removes the value at a specific index and returns it
    public Object remove(int index) {
        return elements.remove(index);
    }

    // Returns the number of elements in the list
    public int size() {
        return elements.size();
    }

    // Returns a string representation of the list
    @Override
    public String toString() {
        return elements.toString();
    }
}

