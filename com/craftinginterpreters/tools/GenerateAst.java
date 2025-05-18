package com.craftinginterpreters.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    // Check for exactly one argument: the output directory for generated files
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    // Define the AST for expressions with their respective fields
    defineAst(outputDir, "Expr", Arrays.asList(
      "Assign   : Token name, Expr value",              // Assignment expression: variable and value
      "Binary   : Expr left, Token operator, Expr right", // Binary operations (e.g., +, -, *, /)
      "Call     : Expr callee, Token paren, List<Expr> arguments", // Function/method call
      "Get      : Expr object, Token name",             // Get property of an object
      "Grouping : Expr expression",                      // Parenthesized expression
      "Literal  : Object value",                         // Literal values like numbers or strings
      "Logical  : Expr left, Token operator, Expr right", // Logical operators (and, or)
      "Set      : Expr object, Token name, Expr value", // Setting a property of an object
      "Super    : Token keyword, Token method",         // 'super' keyword in inheritance
      "This     : Token keyword",                        // 'this' keyword in classes
      "Unary    : Token operator, Expr right",          // Unary operators like negation (-)
      "Variable : Token name"                            // Variable expressions
    ));

    // Define the AST for statements with their respective fields
    defineAst(outputDir, "Stmt", Arrays.asList(
      "Block      : List<Stmt> statements",               // Block of statements enclosed in {}
      "Class      : Token name, Expr.Variable superclass, List<Stmt.Function> methods", // Class declaration
      "Expression : Expr expression",                      // Expression as a statement
      "Function   : Token name, List<Token> params, List<Stmt> body", // Function declaration
      "If         : Expr condition, Stmt thenBranch, Stmt elseBranch", // If-else statement
      "Print      : Expr expression",                      // Print statement
      "Return     : Token keyword, Expr value",            // Return statement
      "Var        : Token name, Expr initializer",         // Variable declaration
      "While      : Expr condition, Stmt body"             // While loop
    ));
  }

  /**
   * Generates the Java source file for the AST base class and nested classes.
   * @param outputDir the directory to write the generated file
   * @param baseName the base name of the AST (e.g., "Expr" or "Stmt")
   * @param types list of type definitions in the format "ClassName : fieldType1 fieldName1, fieldType2 fieldName2"
   */
  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {

    // Construct the output file path for the base AST class
    String path = outputDir + "/" + baseName + ".java";

    // Open a PrintWriter to write to the file with UTF-8 encoding
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    // Write package and imports
    writer.println("//> Appendix II " + baseName.toLowerCase()); // marker for docs
    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();

    // Declare the abstract base class for the AST (e.g., Expr or Stmt)
    writer.println("abstract class " + baseName + " {");

    // Generate the Visitor interface for this AST type
    defineVisitor(writer, baseName, types);

    writer.println();
    writer.println("  // Nested " + baseName + " classes here...");

    // Generate each nested AST node class
    for (String type : types) {
      String className = type.split(":")[0].trim();  // Class name like "Binary"
      String fields = type.split(":")[1].trim();     // Field list like "Expr left, Token operator, Expr right"
      defineType(writer, baseName, className, fields);
    }

    // Define the base accept() method to be implemented by subclasses
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    writer.println("}");

    // Close the writer, finishing the file
    writer.println("//< Appendix II " + baseName.toLowerCase());
    writer.close();
  }

  /**
   * Generates the Visitor interface inside the base AST class.
   * @param writer PrintWriter to write to the file
   * @param baseName Base class name ("Expr" or "Stmt")
   * @param types List of AST node types
   */
  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types) {

    // Start the visitor interface declaration
    writer.println("  interface Visitor<R> {");

    // For each AST node type, declare a visit method that returns R
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(" +
          typeName + " " + baseName.toLowerCase() + ");");
    }

    // End visitor interface
    writer.println("  }");
  }

  /**
   * Generates a nested class for one AST node type inside the base class.
   * @param writer PrintWriter to write to the file
   * @param baseName Base class name ("Expr" or "Stmt")
   * @param className Name of the AST node class (e.g., "Binary")
   * @param fieldList List of fields with types (e.g., "Expr left, Token operator, Expr right")
   */
  private static void defineType(
      PrintWriter writer, String baseName,
      String className, String fieldList) {

    // Mark the start of the class (used for documentation)
    writer.println("//> " + baseName.toLowerCase() + "-" + className.toLowerCase());

    // Declare the nested class extending the base class
    writer.println("  static class " + className + " extends " +
        baseName + " {");

    // Format the constructor arguments nicely for long field lists
    if (fieldList.length() > 64) {
      fieldList = fieldList.replace(", ", ",\n          ");
    }

    // Constructor declaration
    writer.println("    " + className + "(" + fieldList + ") {");

    // Store the parameters in fields (fix line breaks to split correctly)
    fieldList = fieldList.replace(",\n          ", ", ");
    String[] fields = fieldList.split(", ");

    // Assign each parameter to its field inside the constructor
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    writer.println("    }");

    // Visitor pattern: implement the accept method to call visitor's method for this node
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" +
        className + baseName + "(this);");
    writer.println("    }");

    writer.println();

    // Declare the fields as final to hold the node data
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }

    // Close the nested class
    writer.println("  }");

    // Mark the end of the class (used for documentation)
    writer.println("//< " + baseName.toLowerCase() + "-" + className.toLowerCase());
  }
}
