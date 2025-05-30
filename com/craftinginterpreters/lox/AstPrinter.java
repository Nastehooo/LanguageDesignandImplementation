package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

  String print(Expr expr) {
    return expr.accept(this);
  }

  String print(Stmt stmt) {
    return stmt.accept(this);
  }

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
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    for (int i = 0; i < expr.elements.size(); i++) {
      builder.append(expr.elements.get(i).accept(this));
      if (i < expr.elements.size() - 1) builder.append(", ");
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public String visitSubscriptExpr(Expr.Subscript expr) {
    return parenthesize2("subscript", expr.object, expr.index);
  }


  @Override
  public String visitLoxDictExpr(Expr.LoxDict expr) {
    StringBuilder builder = new StringBuilder();
    builder.append("{ ");
    for (Map.Entry<Expr, Expr> entry : expr.entries.entrySet()) {
      builder.append(entry.getKey().accept(this));
      builder.append(": ");
      builder.append(entry.getValue().accept(this));
      builder.append(", ");
    }
    if (!expr.entries.isEmpty()) {
      builder.setLength(builder.length() - 2); // remove trailing comma and space
    }
    builder.append(" }");
    return builder.toString();
  }


  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return parenthesize(";", stmt.expression);
  }

  @Override
  public String visitAssignSubscriptExpr(Expr.AssignSubscript expr) {
      // Represent assignment to subscript like "(assign-subscript <object> <index> <value>)"
      return parenthesize("assignSubscript",
      expr.target.object,
      expr.target.index,
      expr.value);

  }
  

 

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

  @Override
  public String visitIfStmt(Stmt.If stmt) {
    if (stmt.elseBranch == null) {
      return parenthesize2("if", stmt.condition, stmt.thenBranch);
    }
    return parenthesize2("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
  }

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
  }

  @Override
  public String visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null) return "(return)";
    return parenthesize("return", stmt.value);
  }

  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    if (stmt.initializer == null) {
      return parenthesize2("var", stmt.name);
    }
    return parenthesize2("var", stmt.name, "=", stmt.initializer);
  }

  @Override
  public String visitWhileStmt(Stmt.While stmt) {
    return parenthesize2("while", stmt.condition, stmt.body);
  }

  // Expressions

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize2("=", expr.name.lexeme, expr.value);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitCallExpr(Expr.Call expr) {
    return parenthesize2("call", expr.callee, expr.arguments);
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return parenthesize2(".", expr.object, expr.name.lexeme);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return parenthesize2("=", expr.object, expr.name.lexeme, expr.value);
  }

  @Override
  public String visitSuperExpr(Expr.Super expr) {
    return parenthesize2("super", expr.method);
  }

  @Override
  public String visitThisExpr(Expr.This expr) {
    return "this";
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  // --- New visitor methods for added keywords ---

  @Override
  public String visitContinueStmt(Stmt.Continue stmt) {
    return "continue";
  }

  @Override
  public String visitBreakStmt(Stmt.Break stmt) {
    return "break";
  }

  @Override
  public String visitDoStmt(Stmt.Do stmt) {
    return parenthesize2("do", stmt.body, stmt.condition);
  }

  @Override
  public String visitSetStmt(Stmt.Set stmt) {
    return "set";
  }

  @Override
  public String visitBuildStmt(Stmt.Build stmt) {
    return "build";
  }

  @Override
  public String visitWalkStmt(Stmt.Walk stmt) {
    return "walk";
  }

  @Override
  public String visitCheckStmt(Stmt.Check stmt) {
    return "check";
  }

  @Override
  public String visitOtherwiseStmt(Stmt.Otherwise stmt) {
    return "otherwise";
  }

  @Override
  public String visitThruStmt(Stmt.Thru stmt) {
    return "thru";
  }

  // --- Helper methods ---

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

  private String parenthesize2(String name, Object... parts) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    transform(builder, parts);
    builder.append(")");
    return builder.toString();
  }

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
}
