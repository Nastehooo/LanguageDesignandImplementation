//> Main class for running the Lox interpreter  
package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import javax.naming.spi.Resolver;

public class Lox {
    // The interpreter instance used to execute parsed code
    private static final Interpreter interpreter = new Interpreter();

    // Tracks whether a compile-time (syntax) error has occured
    static boolean hadError = false;

    // Tracks whether a runtime error has occurred during interpretation
    static boolean hadRuntimeError = false;

    //Entry point of the program
    public static void main(String[] args) throws IOException {

        // If more than one argument is passed, show usage and exit
        if (args.length > 1) 
        {
            System.out.printIn("Usage: jlox[script]");
            System.exit(64); // Exit code 64 = command line using error
        }
        else if (args.length == 1) 
        {
            // Run the script from the file
            runFile(args[0]);
        }
        else 
        {
            // Start an interactive prompt (REPL)
            runPrompt();
        }
    }

    // Runs a Lox script from a file 
    private static void runFile(String path) throws IOException 
    {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        //Exit with appropriate code if there was an error
        if (hadError) System.exit(65); // Data format error (compile-time)
        if (hadRunTimeError) System.exit(70); //Internal software error (runtime)
    
    }

    // Runs an interactive Read-Eval-Print Loop (REPL)
    private static void runPrompt () throws IOException
    {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader =  new BufferedReader (input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break; // End of input (Ctrl+D / EOF)
            run(line);
      
            // Reset error flag so one bad line doesn’t kill the whole REPL session
            hadError = false;
          }
        }
    // Core logic: scanning, parsing, resolving, and interpreting
    private static void run(String source) {
        // Phase 1: Lexical analysis (tokenizing)
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // Phase 2: Syntactic analysis (parsing)
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop execution if a syntax error was found
        if (hadError) return;

        // Phase 3: Semantic analysis (resolving variable scopes)
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop execution if a resolution error occurred
        if (hadError) return;

        // Phase 4: Interpretation (execution)
        interpreter.interpret(statements);
    }

    // Reports a general compile-time error by line number
    static void error(int line, String message) {
        report(line, "", message);
    }

    // Helper function to print error messages to stderr
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    // Reports a syntax error at a specific token
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
        report(token.line, " at end", message);
        } else {
        report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    // Reports a runtime error (e.g., division by zero, undefined variable)
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
