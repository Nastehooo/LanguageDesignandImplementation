// File: InputFunction.java
package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Scanner;

class InputFunction implements LoxCallable {
    @Override
    public int arity() {
        return 1;  // Takes 1 argument: the prompt string
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.print(arguments.get(0));  // Print prompt
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    @Override
    public String toString() {
        return "<native fn input>";
    }
}
