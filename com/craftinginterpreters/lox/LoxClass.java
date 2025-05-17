package com.craftinginterpreters.lox;
//> Classes lox-class


import java.util.List;
import java.util.Map;

// This class represents a user-defined class in the Lox Language.
// It implements the LoxCallable interface meaning it can be called like a function.

class LoxClass implements LoxCallable {

    // The name of the class. e.g., "Person", "Animal", etc.
    final String name;

    // If this class inherits from another, this will reference the superclass. 
    final LoxClass superclass; 

    //A map of method names to their corresponding LoxFunction objects (i.e., the class's methods).
    private final Map<String, LoxFunction> methods; 

    //Constructor to create a new LoxClass. It takes;
    // - the class name,
    // - the optional superclass (null if no inheritance),
    // - and a map of methods that belong to this class.
    LoxClass(String name, LoxClass superclass, 
             Map<String, LoxFuncion> methods) {
                this.name = name; 
                this.superclass = superclass;
                this.methods = methods;
    }

    // This method is used to find a method by name.
    // If the method is defined in this class, it returns it.
    // If not, and there is a superclass, it recursively checks the superclass. 
    LoxFunction findMethod(String name)
    {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null; //Method not found. 
    }

    // Returns the class name when the object is printed or logged. 
    @Override 
    public String toString() {
        return name; 
    }

    // This mehtod is called when the class is "called" (i.e, instantiated).
    // It creates a new instance of the class and calls it initialiser (if one exists).
    @Override
    public Object call (Intepreter interpreter
                        List<Object> arguments){
        LoxInstance instance = new LoxInstance(this);

        // If the class has an "init" method (constructor), call it with the arguments. 
        LoxFunction initialiser = findMethod("init");
        if (initialiser != null){
            initialiser.bind(instance).call(interpreter, arguments);
        }

        return instance; // Return the new instance 

    }

    // Returns the number of parameters required by the "init" method.
    // This defines how many arguments must be passed when the class is called. 
    @Override
    public init arity() {
        LoxFunction initialiser = findMethod("init");
        if (initialiser == null) return 0;
        return initialiser.arity();
    }
}


