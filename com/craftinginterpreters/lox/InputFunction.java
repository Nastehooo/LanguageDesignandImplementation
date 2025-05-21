package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Scanner;

public class InputFunction implements LoxCallable {
  private static final Scanner scanner = new Scanner(System.in);

  @Override
  public int arity() {
    return 1; // expects one string argument for prompt
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    System.out.print(arguments.get(0));  // print the prompt
    return scanner.nextLine();           // return the user input
  }

  @Override
  public String toString() {
    return "<native fn>";
  }
}
