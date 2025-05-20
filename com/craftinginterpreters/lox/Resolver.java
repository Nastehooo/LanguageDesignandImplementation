package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

// This class performs variable resolution and static analysis.
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;

  // A stack of scopes; each scope is a map from variable names to a boolean indicating if it's been defined.
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();

  // Keeps track of the current function type (used to validate returns, etc.).
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  // Enum for tracking the type of function currently being resolved.
  private enum FunctionType {
    NONE,        // Not in any function
    FUNCTION,    // A regular function
    INITIALIZER, // A class initializer (constructor)
    METHOD       // A class method
  }

  // Enum for tracking whether we're in a class and whether it's a subclass.
  private enum ClassType {
    NONE,     // Not inside any class
    CLASS,    // Inside a class
    SUBCLASS  // Inside a subclass
  }

  private ClassType currentClass = ClassType.NONE;

  // Resolve a list of statements (i.e., a program or block body).
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  // Begin a new block scope.
  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  // End the current block scope.
  private void endScope() {
    scopes.pop();
  }

  // Declare a variable in the current scope but don't mark it as defined yet.
  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Already a variable with this name in this scope.");
    }
    scope.put(name.lexeme, false);
  }

  // Mark a variable as defined in the current scope.
  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }

  // Resolve a single statement.
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  // Resolve a single expression.
  private void resolve(Expr expr) {
    expr.accept(this);
  }

  // Resolve a function and its parameters.
  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();

    currentFunction = enclosingFunction;
  }

  // Resolve local variable by determining its depth in the scope stack.
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
    // Not found; assume global.
  }

  // -- Visitor methods for list and dictionary 
  @Override
  public Void visitLoxListExpr(Expr.LoxList expr) {
      for (Expr element : expr.elements) {
          resolve(element);
      }
      return null;
  }

  @Override
  public Void visitLoxDictExpr(Expr.LoxDict expr) {
      for (Map.Entry<Expr, Expr> entry : expr.entries.entrySet()) {
          resolve(entry.getKey());
          resolve(entry.getValue());
      }
      return null;
  }

  // --- Visitor methods for statements ---

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    declare(stmt.name);
    define(stmt.name);

    if (stmt.superclass != null &&
        stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
      Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
    }

    if (stmt.superclass != null) {
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass);
    }

    if (stmt.superclass != null) {
      beginScope();
      scopes.peek().put("super", true);
    }

    beginScope();
    scopes.peek().put("this", true);

    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method, declaration);
    }

    endScope();

    if (stmt.superclass != null) endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }
    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword, "Can't return a value from an initializer.");
      }
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  // -- New visitor methods for your custom statements ---

  @Override
  public Void visitThruStmt(Stmt.Thru stmt) {
    // Resolve fields inside Thru statement.
    // e.g., resolve(stmt.someExpression);
    // or resolve(stmt.bodyStatements);
    // Replace with actual fields in your Stmt.Thru class:
    // Example:
    // if (stmt.condition != null) resolve(stmt.condition);
    // resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitWalkStmt(Stmt.Walk stmt) {
    // Resolve inside Walk statement
    // e.g., resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitBuildStmt(Stmt.Build stmt) {
    // Resolve inside Build statement
    // e.g., resolve(stmt.someExpr);
    return null;
  }

  @Override
  public Void visitCheckStmt(Stmt.Check stmt) {
    // Resolve condition and branches inside Check statement
    // Example:
    // resolve(stmt.condition);
    // resolve(stmt.thenBranch);
    // if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitOtherwiseStmt(Stmt.Otherwise stmt) {
    // Resolve body of Otherwise statement
    // e.g., resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitSetStmt(Stmt.Set stmt) {
    // Resolve value or expression inside Set statement
    // e.g., resolve(stmt.value);
    return null;
  }

  // --- Visitor methods for expressions ---

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSuperExpr(Expr.Super expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
    }
    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
      return null;
    }
    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    // You can add loop context validation logic here if desired.
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    // Same as above — for static analysis or validation.
    return null;
  }

  @Override
  public Void visitDoStmt(Stmt.Do stmt) {
      // Resolve the body of the do-while loop
      resolve(stmt.body);
      // Resolve the condition expression
      resolve(stmt.condition);
      return null;
  }

  

}
