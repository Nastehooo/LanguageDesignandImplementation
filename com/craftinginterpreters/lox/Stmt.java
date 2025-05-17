//> Appendix II stmt
package com.craftinginterpreters.lox;

import java.util.List;

// Abstract base class representing all statement types in the Lox language.
abstract class Stmt {

  // Visitor interface defines a generic method for each specific subclass of Stmt.
  // This is used to implement the Visitor pattern for handling different kinds of statements.
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitClassStmt(Class stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
  }

  // Block represents a sequence of statements enclosed in curly braces `{ ... }`
  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements; // The list of statements inside the block
  }

  // Class represents a class declaration (e.g., `class Foo { ... }`)
  static class Class extends Stmt {
    Class(Token name,
          Expr.Variable superclass,
          List<Stmt.Function> methods) {
      this.name = name;
      this.superclass = superclass;
      this.methods = methods;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }

    final Token name;                       // The name of the class
    final Expr.Variable superclass;         // Optional superclass (for inheritance)
    final List<Stmt.Function> methods;      // Methods defined in the class
  }

  // Expression represents a statement that evaluates an expression (e.g., `a + b;`)
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression; // The expression to be evaluated
  }

  // Function represents a function declaration (e.g., `fun sayHi() { print "Hi"; }`)
  static class Function extends Stmt {
    Function(Token name, List<Token> params, List<Stmt> body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token name;              // Function name
    final List<Token> params;      // Parameters (arguments) for the function
    final List<Stmt> body;         // Body of the function (statements)
  }

  // If represents an if statement (e.g., `if (condition) { ... } else { ... }`)
  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;      // The condition to be evaluated
    final Stmt thenBranch;     // Statement to execute if condition is true
    final Stmt elseBranch;     // Optional statement to execute if condition is false
  }

  // Print represents a print statement (e.g., `print "Hello";`)
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression; // The expression to be printed
  }

  // Return represents a return statement inside a function (e.g., `return 42;`)
  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword; // The 'return' keyword token
    final Expr value;    // The value being returned (can be null for `return;`)
  }

  // Var represents a variable declaration (e.g., `var x = 10;`)
  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;          // Name of the variable
    final Expr initializer;    // Optional initializer expression
  }

  // While represents a while loop (e.g., `while (condition) { ... }`)
  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;  // The loop condition expression
    final Stmt body;       // The body to execute while the condition is true
  }

  // Abstract method to be implemented by each subclass, allowing a Visitor to visit it.
  abstract <R> R accept(Visitor<R> visitor);
}
//< Appendix II stmt
