//> Statements and State environment-class 

package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a variable environment (scope) in the Lox interpreter. 
 * Supports variable definition, lookup, and assignment, with support for nested scopes.
 */
class Environment {
    // The enclosing (parent) environment, used for nested scopes.
    final Environment enclosing; 

    // Holds variable names (as strings) mapped to their values;
    private final Map<String, Object> values = new HashMap<>();

    // Constructor for the global (top-level) environment. 
    Environment(){
        enclosing = null; 
    }

    // Constructor for a nested environment with a reference to its enclosing scope. 
    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    /**
     * Retrieves the value of a variable. 
     * Searches the current environment and, if not found, recursively checks enclosing environments.
     * @param name Token representing the variable name. 
     * @return The variable's value. 
     * @throws RuntimeError if the variable is undefined. 
     */
    Object get(Token name){
        if (values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

        // If not in the current scope, check the enclosing (outer) environment. 
        if (enclosing != null) return enclosing.get(name);

        // Variable not found in any scope - throw an error.
        throw new RuntimeError(name, 
            "Undefined vairable '" + name.lexeme + "'.");
    }

    /**
     * Assigns to new value to an existing variable. 
     * If the variable exists in the current or enclosing spaces, updates its value. 
     * @param name Token representing the variable name. 
     * @return The variable's value. 
     * @throws RuntimeError if the variable is undefined. 
     */
    void assign(Token name, Object value){
        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme, value);
            return;
        }

        // If not in current scope, try assigning in enclosing environment.
        if (enclosing != null){
            enclosing.assign(name, value);
            return;
        }

        // Variable not found - throw an error.
        throw new RuntimeError(name, 
            "Undefined vairable '" + name.lexeme + "'.");
    }
    /**
     * Defines a new variable in the current scope. 
     * @param name Variable name. 
     * @param value Initial value. 
     */
    void define(String name, Object value){
        values.put(name, value);
    }

    /**
     * Returns the ancestor environment that is 'distance' levels up.
     * Useful for optimised variable resolution (e.g., after static analysis).
     * @param distance Number of scopes to go up.
     * @return The ancestor Environment
     */
    Environment ancestor(int distance){
        Environment environment = this;
        for (int i = 0; i < distance; i++){
            environment = environment.enclosing; // Traverse up the chain. 
        }
        return environment; 
    }

    /**
     * Retrieves a variable from an ancestor environment at a certain distance.
     * @param distance How many scopes up to look.
     * @param name Variable name 
     * @return The variable's value. 
     */
    Object getAt(int distance, String name){
        return ancestor(distance).values.get(name);
    }

    /**
     * Assigns a value to variable in an ancestor environment. 
     * @param distance How many scopes up to assign.
     * @param name Token representing the variable name.
     * @param value New value to assign
     */
    void assignAt(int distance, Token name, Object value){
        ancestor(distance).values.put(name.lexeme,value);
    }
    /**
   * Returns a string representation of the current environment.
   * Includes its values and recursively prints enclosing environments.
   * @return String of current and enclosing scopes.
   */
  @Override
  public String toString() {
    String result = values.toString();
    if (enclosing != null) {
      result += " -> " + enclosing.toString();
    }

    return result;
  }
}

