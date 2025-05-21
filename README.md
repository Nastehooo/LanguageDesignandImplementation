# Lox Interpreter — Custom Implementation

This project is a Java implementation of the Lox programming language, inspired by Crafting Interpreters. It supports core Lox features, extended with dictionary (hash map) and list subscripting, arithmetic, variables, strings, and error handling.
Features

Variable declarations and assignments
Arithmetic with integers and floats
Strings and string concatenation
Lists and dictionaries (hash maps) with subscript syntax
Runtime and syntax error reporting
Interactive REPL and script execution modes
Usage

Compile and run scripts using the command line.

# How to run stages
This project is organised into six stages, each building on the previous one: 

1. Stage 1: Basic variable declarations and printing.
2. Stage 2: Arithmetic operations and expressions.
3. Stage 3: Type handling and type-safe operations.
4. Stage 4: String concatenation and type conversions.
5. Stage 5: Control flow structures (if, while, etc.).
6. Stage 6: List support and manipulation.

# Running the Interpreter
To run the interpreter on a Lox script, use the following command in your terminal:

java -cp bin com.craftinginterpreters.lox.Lox test/your_script.lox


# How to Build the Lox Interpreter:
Requirements:
 * Java JDK installed (version 8 or higher)
 * Terminal or command prompt

# Compile the Project: 

javac -d bin com/craftinginterpreters/lox/*.java 
## This compiles all Java files and outputs the class files into the bin directory. 

