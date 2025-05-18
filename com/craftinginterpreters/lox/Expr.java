//> Appendix II expr
package com.craftinginterpreters.lox;

import java.util.List;

/**
 * Abstract base class for all expression nodes in the AST (Abstract Syntax Tree).
 * Follows the Visitor design pattern for type-safe expression evaluation or transformation.
 */
abstract class Expr {

  /**
   * Visitor interface defines a visit method for each concrete Expr subclass.
   * It allows separating the expression evaluation logic from the structure.
   */
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGetExpr(Get expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitSetExpr(Set expr);
    R visitSuperExpr(Super expr);
    R visitThisExpr(This expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
  }

  // Each of the following static classes represents a different kind of expression in the language.
  // They all extend the abstract Expr base class and implement the accept method for the Visitor pattern.

  // Assignment expression: e.g., `x = 5`
  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;   // Variable being assigned to
    final Expr value;   // Value being assigned
  }

  // Binary expression: e.g., `a + b`
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  // Function or method call: e.g., `foo(1, 2)`
  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    final Expr callee;            // Function being called
    final Token paren;            // Closing parenthesis (for error reporting)
    final List<Expr> arguments;   // Arguments passed to the function
  }

  // Accessing a property from an object: e.g., `object.property`
  static class Get extends Expr {
    Get(Expr object, Token name) {
      this.object = object;
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }

    final Expr object;  // Object from which property is accessed
    final Token name;   // Name of the property
  }

  // Grouping expression: e.g., `(a + b)`
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;  // The inner expression
  }

  // Literal expression: e.g., `123`, `"hello"`, `true`
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;  // The actual literal value
  }

  // Logical expression: e.g., `a && b` or `a || b`
  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  // Setting a property on an object: e.g., `object.property = value`
  static class Set extends Expr {
    Set(Expr object, Token name, Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }

    final Expr object;  // Object whose property is being set
    final Token name;   // Property name
    final Expr value;   // Value to assign
  }

  // Refers to the superclass's method: e.g., `super.method()`
  static class Super extends Expr {
    Super(Token keyword, Token method) {
      this.keyword = keyword;
      this.method = method;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSuperExpr(this);
    }

    final Token keyword; // 'super' keyword token
    final Token method;  // The method being accessed in the superclass
  }

  // Refers to the current object: e.g., `this`
  static class This extends Expr {
    This(Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitThisExpr(this);
    }

    final Token keyword; // 'this' keyword token
  }

  // Unary expression: e.g., `-a`, `!b`
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator; // Operator like '-' or '!'
    final Expr right;     // Operand the operator is applied to
    public Expr left;
  }

  // Variable expression: e.g., `x`
  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;  // Variable name token
  }

  // Each expression node implements this method to accept a visitor
  abstract <R> R accept(Visitor<R> visitor);
}
