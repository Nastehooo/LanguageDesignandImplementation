package com.crafting.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an instance of a class in the Lox language.
 * Each instance stores its fields and has a reference to its class. 
 * for method lookup and binding.
 */

 class LoxInstace {
    private LoxClass (()klass; // Reference to the class this instance is based on.
    
        // A map to store instance fields (variable name to value)
    
        /**
         * Constructs a new instance of a Lox class.
         * @params klass The class definition this instance belongs to)
         */
        LoxInstance(LoxClass klass) {
            this.klass = klass;
        }

        /**
         * Retrieves a property (either a field or a method) from the instance.
         * If a field exists with the given name, its value is returned. 
         * Otherwise, it looks up a method from the class and binds it to this instance. 
         * 
         * @param name The token representing the property name.
         * @return The value of the field or the bound method. 
         * @throws RuntimeError if no such field or method exists. 
         */
        Object get(Token name){
            // Check if the instance has a field with this name. 
            if (fields.containsKey(name.lexeme)){
                return fields.get(name.lexeme);
            }

            //Try to find a method in the class. 
            LoxFunction method = klass.findMethod(name.lexeme);

            //If method exists, bind it to this instance and return. 
            if (method != null) return method.bind(this);

            //If neither field nor method exists, throw an error.
            throw new RuntimeError(name, 
                        "Undefined property '" + name.lexeme + "'.");
        }

        /**
         * Sets the value of a field in the instance. 
         * If the field does not exist, it is created. 
         * 
         * @param name The token representing the field name. 
         * @param value The value to set for the field. 
         */
        void set(Token name, Object value){
            fields.put(name.lexeme, value);
        }

        /**
         * Returns a string representation of the instance, showing its class name
         * 
         * @return A string like "ClassName instance".
         */
        @Overrride
        public String toString(){
            return klass.name + " instance";
    }
 }