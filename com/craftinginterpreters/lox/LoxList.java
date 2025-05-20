package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * A native list object for Lox, backed by a Java ArrayList.
 * Supports push, get, remove, and size methods callable from Lox.
 */
public class LoxList {
    private final List<Object> elements;

    public LoxList() {
        this.elements = new ArrayList<>();
    }

    public void add(Object value) {
        elements.add(value);
    }

    /**
     * Direct index access method to support subscript like list[0]
     */
    public Object get(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new RuntimeError(null, "Index out of bounds: " + index);
        }
        return elements.get(index);
    }

    public void set(Token indexToken, Object index, Object value) {
        // Example assuming index is an integer:
        if (!(index instanceof Integer)) {
            throw new RuntimeError(indexToken, "List index must be an integer.");
        }
        int i = (Integer) index;
        if (i < 0 || i >= elements.size()) {
            throw new RuntimeError(indexToken, "List index out of bounds.");
        }
        elements.set(i, value);
    }
    

    /**
     * Property access for callable list methods like "push", "get", "remove", and "size".
     */
    public Object get(Token name) {
        switch (name.lexeme) {
            case "push":
                return new LoxCallable() {
                    @Override
                    public int arity() { return 1; }

                    @Override
                    public Object call(Interpreter interpreter, List<Object> arguments) {
                        elements.add(arguments.get(0));
                        return null;
                    }

                    @Override
                    public String toString() { return "<native fn push>"; }
                };
            case "get":
                return new LoxCallable() {
                    @Override
                    public int arity() { return 1; }

                    @Override
                    public Object call(Interpreter interpreter, List<Object> arguments) {
                        int index = ((Double) arguments.get(0)).intValue();
                        return elements.get(index);
                    }

                    @Override
                    public String toString() { return "<native fn get>"; }
                };
            case "remove":
                return new LoxCallable() {
                    @Override
                    public int arity() { return 1; }

                    @Override
                    public Object call(Interpreter interpreter, List<Object> arguments) {
                        int index = ((Double) arguments.get(0)).intValue();
                        return elements.remove(index);
                    }

                    @Override
                    public String toString() { return "<native fn remove>"; }
                };
            case "size":
                return new LoxCallable() {
                    @Override
                    public int arity() { return 0; }

                    @Override
                    public Object call(Interpreter interpreter, List<Object> arguments) {
                        return (double) elements.size();
                    }

                    @Override
                    public String toString() { return "<native fn size>"; }
                };
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    // Setting properties on the list is unsupported
    public void set(Token name, Object value) {
        throw new RuntimeError(name, "Cannot set property '" + name.lexeme + "' on list.");
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
