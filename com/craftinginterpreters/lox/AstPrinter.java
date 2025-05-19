package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/**
 * AstPrinter is a utility class used to convert the Lox abstract syntax tree (AST)
 * into a readable, parenthesized string representation similar to Lisp.
 * It implements both Expr.Visitor and Stmt.Visitor interfaces to traverse
 * expression and statement nodes.
 */
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

  // Entry point for printing an expression node
  String print(Expr expr) {
    return expr.accept(this);
  }

  // Entry point for printing a statement node
  String print(Stmt stmt) {
    return stmt.accept(this);
  }

  // Block statement: (block <stmt1> <stmt2> ...)
  @Override
  public String visitBlockStmt(Stmt.Block stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(block ");
    for (Stmt statement : stmt.statements) {
      builder.append(statement.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  // Class statement: (class ClassName <superclass>? <methods>...)
  @Override
  public String visitClassStmt(Stmt.Class stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(class " + stmt.name.lexeme);
    if (stmt.superclass != null) {
      builder.append(" < " + print(stmt.superclass));
    }
    for (Stmt.Function method : stmt.methods) {
      builder.append(" " + print(method));
    }
    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitLoxListExpr(Expr.LoxList expr) {
      // Convert the LoxList expression into a string representation.
      // Format: [elem1, elem2, elem3, ...]
      StringBuilder builder = new StringBuilder();
      builder.append("[");
      for (int i = 0; i < expr.elements.size(); i++) {
          builder.append(expr.elements.get(i).accept(this)); // Recursively convert each element.
          if (i < expr.elements.size() - 1) {
              builder.append(", ");  // Add comma separator between elements.
          }
      }
      builder.append("]");
      return builder.toString();
  }

  @Override
  public String visitLoxDictExpr(Expr.LoxDict expr) {
      // Convert the LoxDict expression into a string representation.
      // Format: {key1: value1, key2: value2, ...}
      StringBuilder builder = new StringBuilder();
      builder.append("{");
      int count = 0;
      int size = expr.entries.size();
      for (Map.Entry<Expr, Expr> entry : expr.entries.entrySet()) {
          // Convert the key expression to string
          builder.append(entry.getKey().accept(this));
          builder.append(": ");
          // Convert the value expression to string
          builder.append(entry.getValue().accept(this));
          if (count < size - 1) {
              builder.append(", "); // Add comma separator between entries.
          }
          count++;
      }
      builder.append("}");
      return builder.toString();
  }


 


  // Expression statement: (; <expression>)
  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return parenthesize(";", stmt.expression);
  }

  // Function declaration: (fun name(param1 param2 ...) <body>...)
  @Override
  public String visitFunctionStmt(Stmt.Function stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(fun " + stmt.name.lexeme + "(");
    for (Token param : stmt.params) {
      if (param != stmt.params.get(0)) builder.append(" ");
      builder.append(param.lexeme);
    }
    builder.append(") ");
    for (Stmt body : stmt.body) {
      builder.append(body.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  // If statement: (if condition thenBranch) or (if-else condition then else)
  @Override
  public String visitIfStmt(Stmt.If stmt) {
    if (stmt.elseBranch == null) {
      return parenthesize2("if", stmt.condition, stmt.thenBranch);
    }
    return parenthesize2("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
  }

  // Print statement: (print <expression>)
  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
  }

  // Return statement: (return) or (return <value>)
  @Override
  public String visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null) return "(return)";
    return parenthesize("return", stmt.value);
  }

  // Variable declaration: (var name) or (var name = value)
  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    if (stmt.initializer == null) {
      return parenthesize2("var", stmt.name);
    }
    return parenthesize2("var", stmt.name, "=", stmt.initializer);
  }

  // While loop: (while condition body)
  @Override
  public String visitWhileStmt(Stmt.While stmt) {
    return parenthesize2("while", stmt.condition, stmt.body);
  }

  // Assignment expression: (= name value)
  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize2("=", expr.name.lexeme, expr.value);
  }

  // Binary expression: (operator left right)
  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  // Function call: (call callee args...)
  @Override
  public String visitCallExpr(Expr.Call expr) {
    return parenthesize2("call", expr.callee, expr.arguments);
  }

  // Property access: (. object name)
  @Override
  public String visitGetExpr(Expr.Get expr) {
    return parenthesize2(".", expr.object, expr.name.lexeme);
  }

  // Grouped expression: (group expression)
  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  // Literal values: just return their string representation
  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  // Logical expressions (and/or): (operator left right)
  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  // Property setting: (= object name value)
  @Override
  public String visitSetExpr(Expr.Set expr) {
    return parenthesize2("=", expr.object, expr.name.lexeme, expr.value);
  }

  // Super method call: (super methodName)
  @Override
  public String visitSuperExpr(Expr.Super expr) {
    return parenthesize2("super", expr.method);
  }

  // 'this' keyword
  @Override
  public String visitThisExpr(Expr.This expr) {
    return "this";
  }

  // Unary expression: (operator right)
  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  // Variable expression: just return the variable name
  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  /**
   * Helper method for printing expressions in the form:
   * (name expr1 expr2 ...)
   */
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  /**
   * More flexible helper that supports mixing Expr, Stmt, Token, and other parts.
   */
  private String parenthesize2(String name, Object... parts) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    transform(builder, parts);
    builder.append(")");
    return builder.toString();
  }

  /**
   * Transforms each part (Expr, Stmt, Token, List, or literal) into a string.
   */
  private void transform(StringBuilder builder, Object... parts) {
    for (Object part : parts) {
      builder.append(" ");
      if (part instanceof Expr) {
        builder.append(((Expr) part).accept(this));
      } else if (part instanceof Stmt) {
        builder.append(((Stmt) part).accept(this));
      } else if (part instanceof Token) {
        builder.append(((Token) part).lexeme);
      } else if (part instanceof List) {
        transform(builder, ((List<?>) part).toArray());
      } else {
        builder.append(part);
      }
    }
  }

  /**
   * Uncomment the following main method for a sample usage demonstration.
   */
//  public static void main(String[] args) {
//    Expr expression = new Expr.Binary(
//        new Expr.Unary(s
//            new Token(TokenType.MINUS, "-", null, 1),
//            new Expr.Literal(123)),
//        new Token(TokenType.STAR, "*", null, 1),
//        new Expr.Grouping(
//            new Expr.Literal(45.67)));
//
//    System.out.println(new AstPrinter().print(expression));
//  }
}
