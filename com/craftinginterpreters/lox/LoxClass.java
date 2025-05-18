package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

// This class represents a user-defined class in the Lox Language.
// It implements the LoxCallable interface, meaning instances of this class
// can be "called" like functions (usually to create new instances).
class LoxClass implements LoxCallable {

    // The name of the class (e.g., "Person", "Animal", etc.)
    final String name;

    // The superclass, if this class inherits from another class. Null if none.
    final LoxClass superclass;

    // A map from method names to LoxFunction objects representing the class's methods.
    private final Map<String, LoxFunction> methods;

    // Constructor to create a new LoxClass.
    // Takes the class name, optional superclass, and the map of methods.
    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    // Finds a method by name in this class or any superclass.
    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        if (superclass != null) {
            return superclass.findMethod(name);
        }
        return null; // Method not found
    }

    // Returns the class name when printing or logging the class object.
    @Override
    public String toString() {
        return name;
    }

    // This method is called when the class is "called" like a function (i.e., instantiated).
    // It creates a new instance of this class and calls its initializer ("init") if present.
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Create a new instance of this class
        LoxInstance instance = new LoxInstance(this);

        // Look for the initializer method ("init")
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            // Bind the initializer to the new instance and call it with arguments
            initializer.bind(instance).call(interpreter, arguments);
        }

        // Return the new instance
        return instance;
    }

    // Returns the number of arguments expected by the initializer ("init") method.
    // If there is no initializer, returns 0.
    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}
