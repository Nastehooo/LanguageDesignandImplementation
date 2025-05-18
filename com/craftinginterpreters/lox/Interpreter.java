package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Interpreter class implements the visitor interfaces for both expressions and statements.
// It is responsible for evaluating expressions and executing statements.
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

  // The global environment holds globally defined variables and functions.
  final Environment globals = new Environment();

  // The current environment starts as the global environment.
  // It can be changed to handle nested scopes (like blocks or function calls).
  private Environment environment = globals;

  // Map to keep track of the scope depth for variable resolution.
  // It maps expressions to the number of scopes between current scope and variable definition.
  private final Map<Expr, Integer> locals = new HashMap<>();

  // Constructor: Define native functions here.
  Interpreter() {
    // Define a native function "clock" that returns the current time in seconds.
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; } // clock takes no arguments

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        // Return system time in seconds as a double
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
  }

  // Interpret a list of statements (the program)
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement); // execute each statement
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error); // Handle runtime errors gracefully
    }
  }

  // Evaluate an expression and return its result
  private Object evaluate(Expr expr) {
    return expr.accept(this); // Use visitor pattern to evaluate
  }

  // Execute a statement (statement visitor pattern)
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  // Keep track of variable resolution by associating an expression with its depth
  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  // Execute a block of statements within a new environment (scope)
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment; // save current environment
    try {
      this.environment = environment; // switch to new environment

      for (Stmt statement : statements) {
        execute(statement); // execute each statement in block
      }
    } finally {
      this.environment = previous; // restore previous environment
    }
  }

  // Visit a block statement and execute it in a new environment scope
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  // Visit a class declaration statement and define the class in the environment
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    // Evaluate superclass if there is inheritance
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
      }
    }

    // Define the class name in the environment before its methods
    environment.define(stmt.name.lexeme, null);

    // If subclassing, create a new environment for 'super'
    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    // Collect methods into a map from method name to function
    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      // Create LoxFunction, mark as initializer if named 'init'
      LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }

    // Create the LoxClass object, passing superclass and methods
    LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);

    // Restore previous environment if superclass environment was created
    if (superclass != null) {
      environment = environment.enclosing;
    }

    // Assign the class object to the class name in the environment
    environment.assign(stmt.name, klass);
    return null;
  }

  // Evaluate an expression statement
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  // Visit a function declaration statement and define the function in the environment
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // Create LoxFunction for the declared function (not an initializer)
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  // Visit an if statement and execute branches conditionally
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  // Visit a print statement: evaluate and print the result
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  // Visit a return statement: evaluate return value and throw to unwind the call stack
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    // Throw Return exception to signal function return with value
    throw new Return(value);
  }

  // Visit a variable declaration statement
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    // Define the variable with its initial value in the current environment
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  // Visit a while loop statement and execute the body repeatedly while condition is true
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  // Visit an assignment expression: evaluate right side and assign to variable
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    // Look up the variable's scope depth and assign accordingly
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  // Visit a binary expression and perform the appropriate operation
  @Override
public Object visitBinaryExpr(Expr.Binary expr) {
  Object left = evaluate(expr.left);
  Object right = evaluate(expr.right);

  switch (expr.operator.type) {
    case BANG_EQUAL: return !isEqual(left, right);
    case EQUAL_EQUAL: return isEqual(left, right);

    case GREATER:
      checkNumberOperands(expr.operator, left, right);
      return (double)left > (double)right;
    case GREATER_EQUAL:
      checkNumberOperands(expr.operator, left, right);
      return (double)left >= (double)right;
    case LESS:
      checkNumberOperands(expr.operator, left, right);
      return (double)left < (double)right;
    case LESS_EQUAL:
      checkNumberOperands(expr.operator, left, right);
      return (double)left <= (double)right;

    case MINUS:
      checkNumberOperands(expr.operator, left, right);
      return (double)left - (double)right;

    case PLUS:
      if (left instanceof Double && right instanceof Double) {
        return (double)left + (double)right;
      }
      if (left instanceof String && right instanceof String) {
        return (String)left + (String)right;
      }
      throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

    case SLASH:
      checkNumberOperands(expr.operator, left, right);
      return (double)left / (double)right;

    case STAR:
      checkNumberOperands(expr.operator, left, right);
      return (double)left * (double)right;

    case AND:
    case OR:
      throw new RuntimeError(expr.operator, "Logical operators should be handled in visitLogicalExpr().");

    default:
      throw new RuntimeError(expr.operator, "Unknown binary operator: " + expr.operator.type);
    }
  }

  // Visit a function or class call expression and execute it
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    // Evaluate all arguments
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    // Check that the callee is callable
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable)callee;

    // Check arity (argument count)
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    // Call the function or class constructor with arguments
    return function.call(this, arguments);
  }

  // Visit a get expression (property access on objects)
  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance)object).get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  // Visit a grouping expression (parentheses)
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  // Visit a literal expression (numbers, strings, booleans, nil)
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  // Visit a logical expression (and/or)
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left; // Short-circuit OR
    } else {
      if (!isTruthy(left)) return left; // Short-circuit AND
    }

    return evaluate(expr.right);
  }

  // Visit a set expression (assigning to an instance field)
  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    Object value = evaluate(expr.value);
    ((LoxInstance)object).set(expr.name, value);
    return value;
  }

  // Visit a super expression (calling a superclass method)
  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass)environment.getAt(distance, "super");

    // 'this' is always one environment nearer than 'super'
    LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");

    // Find method in superclass
    LoxFunction method = superclass.findMethod(expr.method.lexeme);

    if (method == null) {
      throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
    }

    // Bind method to 'this' instance and return
    return method.bind(object);
  }

  // Visit a this expression (reference to current instance)
  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  // Visit a unary expression (! or -)
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
      default:
        throw new RuntimeError(expr.operator, "Unknown unary operator: " + expr.operator.type);
    }
  }

  // Visit a variable expression (variable lookup)
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  // Helper method to look up a variable's value considering its scope depth
  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      // Variable is in a local scope
      return environment.getAt(distance, name.lexeme);
    } else {
      // Variable is global
      return globals.get(name);
    }
  }

  // Check if an operand is a number; throw runtime error if not
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  // Check if both operands are numbers; throw runtime error if not
  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  // Helper method to check if a value is truthy (for control flow)
  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  // Helper method to check equality of two objects
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }

  // Helper method to convert values to strings for printing
  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      // Remove trailing .0 for integers
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}

