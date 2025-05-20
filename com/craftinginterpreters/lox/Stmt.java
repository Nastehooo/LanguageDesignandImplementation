package com.craftinginterpreters.lox;

import java.util.List;

// Abstract base class representing all statement types in the Lox language.
abstract class Stmt {

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
    R visitOtherwiseStmt(Otherwise stmt);
    R visitSetStmt(Set stmt);
    R visitBuildStmt(Build stmt);
    R visitWalkStmt(Walk stmt);
    R visitCheckStmt(Check stmt);
    R visitThruStmt(Thru stmt);
    R visitBreakStmt(Break stmt);
    R visitContinueStmt(Continue stmt);
    R visitDoStmt(Do stmt);
  }

  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    // Public field so the parser can extract its contents.
    final List<Stmt> statements;
  }

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

    final Token name;
    final Expr.Variable superclass;
    final List<Stmt.Function> methods;
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }

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

    final Token name;
    final List<Token> params;
    final List<Stmt> body;
  }

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

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value;
  }

  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer;
  }

  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }

  static class Otherwise extends Stmt {
    Otherwise() {}

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitOtherwiseStmt(this);
    }
  }

  static class Set extends Stmt {
    Set(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetStmt(this);
    }

    final Token name;
    final Expr value;
  }

  static class Build extends Stmt {
    Build(Expr target, List<Expr> arguments) {
      this.target = target;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBuildStmt(this);
    }

    final Expr target;
    final List<Expr> arguments;
  }

  static class Walk extends Stmt {
    Walk(Expr direction) {
      this.direction = direction;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWalkStmt(this);
    }

    final Expr direction;
  }

  static class Check extends Stmt {
    Check(Expr condition) {
      this.condition = condition;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCheckStmt(this);
    }

    final Expr condition;
  }

  static class Thru extends Stmt {
    Thru(Expr expr) {
      this.expr = expr;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitThruStmt(this);
    }

    final Expr expr;
  }

  static class Break extends Stmt {
    Break(Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }

    final Token keyword;
  }

  static class Continue extends Stmt {
    Continue(Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }

    final Token keyword;
  }

  static class Do extends Stmt {
    Do(Token doKeyword, Stmt body, Expr condition) {
      this.doKeyword = doKeyword;
      this.body = body;
      this.condition = condition;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitDoStmt(this);
    }

    final Token doKeyword;
    final Stmt body;
    final Expr condition;
  }

  // Abstract method for visitor pattern.
  abstract <R> R accept(Visitor<R> visitor);
}
