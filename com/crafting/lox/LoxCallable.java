package com.crafting.lox;
// This interface defines a "callable" object in the Lox Language. 
// In Lox, both functions and classes can be "called", similar to functions in Java or Python

 interpreters.lox;

import java.util.List;

// Define the LoxCallable Interface. 
// Any class that implements this must define how many arguments it takes (arity)
// and what happens when it is called (call).
interface LoxCallable {

    //This method returns the number of arguments (parameters) the callable expects.
    // For example, if a function takes two arguments, this will return 2. 
    int arity();

    // This method is called when the object is invoked (like calling a function).
    // - 'interpreter' is the current interpreter running the program. 
    // - 'arguments' is the list of values passed in when the function or class is called. 
    // It returns an Object, which could be the result of the function execution or a new instance (in the case of a class). 
    Object call (Interpreter interpreter, List<object> arguments);
}