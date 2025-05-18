package com.craftinginterpreters.lox;
// This class represents a function in the Lox Language. 
// It implements LoxCallable, so it can be "called" like a function in code. 

import java.util.List;

class LoxFunction implements LoxCallable {
    
    // Stores the function's syntax tree (parameters and body).
    private final Stmt.Function declaration;

    // The closure is the enivronment where the function was declared. 
    // This is used to preserve variables from the surrounding scope. 
    private final Environment closure;

    // Used to determine if this function is actually an initialiser (i.e., a constructor)
    private final boolean isInitializer;

    // Constructor for LoxFunction. It receives; 
    // - declaration: the syntax tree of the function
    // - closure: the environment where it was created
    // - isInitialiser: whether it is used to initialise a class instance
    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    // This method is used when a function is bound to a class instnace. 
    // It creates a new environment where "this" refers to the given instance. 
    LoxFunction bind (LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);

        // Return a new LoxFunction bound to the instance (with the same declaration).
        return new LoxFunction(declaration, environment, isInitializer);

    }

    // Returns a string representation of the function, like "<fn sayHello>"
    @Override
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";

    }

    // Returns the number of parameters this function expects. 
    @Override
    public int arity() {
        return declaration.params.size();
    }

    // Called when the function is executed. 
    // This is where parameters are assigned, and the function body is run. 
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Create a new environment that inherits from the function's closure. 
        Environment environment = new Environment (closure);

        // Assign each argument to the corresponding parameter name in the new environment. 
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        // Execute the function body in the new environment. 
        try { 
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            // If a return value is caught, return it-unless this is an initialiser, 
            // in which case we always return "this".
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }
        
        // If no return was thrown, return "this" if this is an initialiser,
        // otherwise return null (no value).
        if (isInitializer) return closure.getAt(0, "this");
        return null;

    }
}