package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new LoxCallable() {
        @Override public int arity() { return 0; }
        @Override public Object call(Interpreter interpreter, List<Object> arguments) {
            return (double) System.currentTimeMillis() / 1000.0;
        }
        @Override public String toString() { return "<native fn>"; }
    });

    globals.define("input", new InputFunction());

    // Add the native global "myList" here
    globals.define("myList", new LoxList());

     // Native function to remove a key from dictionary
     globals.define("dictRemove", new LoxCallable() {
      @Override public int arity() { return 2; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
          Object dictObj = arguments.get(0);
          Object key = arguments.get(1);

          if (!(dictObj instanceof LoxDict)) {
              throw new RuntimeError(null, "First argument to dictRemove must be a dictionary.");
          }

          ((LoxDict) dictObj).remove(key);
          return null;
      }

      @Override public String toString() { return "<native fn dictRemove>"; }
  });

  // Native function to check if dictionary contains a key
  globals.define("dictContainsKey", new LoxCallable() {
      @Override public int arity() { return 2; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
          Object dictObj = arguments.get(0);
          Object key = arguments.get(1);

          if (!(dictObj instanceof LoxDict)) {
              throw new RuntimeError(null, "First argument to dictContainsKey must be a dictionary.");
          }

          return ((LoxDict) dictObj).containsKey(key);
      }

      @Override public String toString() { return "<native fn dictContainsKey>"; }
  });
}

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Object visitSubscriptExpr(Expr.Subscript expr) {
    Object object = evaluate(expr.object);
    Object index = evaluate(expr.index);

    if (object instanceof LoxList) {
        LoxList list = (LoxList) object;
        if (!(index instanceof Double)) {
            throw new RuntimeError(expr.indexToken, "List index must be a number.");
        }
        int i = ((Double) index).intValue();
        try {
            return list.get(i);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeError(expr.indexToken, "List index out of bounds.");
        }
    }

    if (object instanceof LoxDict) {
        LoxDict dict = (LoxDict) object;
        if (!dict.containsKey(index)) {
            throw new RuntimeError(expr.indexToken, "Key not found in dictionary.");
        }
        return dict.get(index);
    }

    throw new RuntimeError(expr.indexToken, "Only lists and dictionaries can be indexed.");
  }

  @Override
  public Object visitAssignSubscriptExpr(Expr.AssignSubscript expr) {
    Object object = evaluate(expr.target.object);
    Object index = evaluate(expr.target.index);
    Object value = evaluate(expr.value);

    if (object instanceof LoxList) {
        ((LoxList) object).set(expr.target.indexToken, index, value);
        return value;
    } else if (object instanceof LoxDict) {
        ((LoxDict) object).put(index, value);
        return value;
    } else {
        throw new RuntimeError(expr.target.indexToken, "Can only assign to list or dict elements.");
    }
}







  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
      }
    }
    environment.define(stmt.name.lexeme, null);

    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }

    LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass) superclass, methods);

    if (superclass != null) {
      environment = environment.enclosing;
    }

    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Object visitLoxDictExpr(Expr.LoxDict expr) {
      Map<Object, Object> map = new HashMap<>();
      for (Map.Entry<Expr, Expr> entry : expr.entries.entrySet()) {
        Object key = evaluate(entry.getKey());
        Object value = evaluate(entry.getValue());
        map.put(key, value);
      }
    
      return new LoxDict(map);
  }
  
  @Override
  public Object visitLoxListExpr(Expr.LoxList expr) {   
  LoxList loxList = new LoxList();
  for (Expr element : expr.elements) {
    loxList.add(evaluate(element));
  }
      return loxList;
  }


  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
      evaluate(stmt.expression);
      return null;
  }


  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }


  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
      Object value = null;
      if (stmt.value != null) value = evaluate(stmt.value);
      throw new Return(value);
  }


  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  // Control flow exceptions
  static class BreakException extends RuntimeException {}
  static class ContinueException extends RuntimeException {}

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new BreakException();
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    throw new ContinueException();
  }

  @Override
  public Void visitThruStmt(Stmt.Thru stmt) {
    return null;
  }

  @Override
  public Void visitBuildStmt(Stmt.Build stmt) {
    return null;
  }

  @Override
  public Void visitWalkStmt(Stmt.Walk stmt) {
    return null;
  }

  @Override
  public Void visitSetStmt(Stmt.Set stmt) {
    return null;
  }

  @Override
  public Void visitCheckStmt(Stmt.Check stmt) {

    return null;
  }

  @Override
  public Void visitOtherwiseStmt(Stmt.Otherwise stmt) {
    return null;
  }

  @Override
  public Void visitDoStmt(Stmt.Do stmt) {
    do {
      try {
        execute(stmt.body);
      } catch (BreakException e) {
        break;
      } catch (ContinueException e) {
        // continue to next iteration
      }
    } while (isTruthy(evaluate(stmt.condition)));
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
  while (isTruthy(evaluate(stmt.condition))) {
    try {
      execute(stmt.body);
    } catch (BreakException e) {
      break;
    } catch (ContinueException e) {
      // continue next iteration
    }
  }
  return null;
}


  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
        case PLUS:
            if (left instanceof String && right instanceof String) {
                return (String) left + (String) right;
            }
            if (left instanceof Number && right instanceof Number) {
                return ((Number) left).doubleValue() + ((Number) right).doubleValue();
            }
            throw new RuntimeError(expr.operator, "Operands must be both numbers or both strings.");
    
     case PERCENT: {
          checkNumberOperands(expr.operator, left, right);
          double leftNum = ((Number) left).doubleValue();
          double rightNum = ((Number) right).doubleValue();
          return leftNum % rightNum;
      }
               
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      default:
        throw new RuntimeError(expr.operator, "Unknown operator.");
    }
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
      Object callee = evaluate(expr.callee);

      List<Object> arguments = new ArrayList<>();
      for (Expr argument : expr.arguments) {
          arguments.add(evaluate(argument));
      }

      if (!(callee instanceof LoxCallable)) {
          throw new RuntimeError(expr.paren, "Can only call functions and classes.");
      }

      LoxCallable function = (LoxCallable) callee;

      if (arguments.size() != function.arity()) {
          throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
      }

      return function.call(this, arguments);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

 
  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);

    if (object instanceof LoxInstance) {
        return ((LoxInstance) object).get(expr.name);
    }

    if (object instanceof LoxList) {
        return ((LoxList) object).get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
}




  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    Object value = evaluate(expr.value);
    ((LoxInstance) object).set(expr.name, value);
    return value;
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass) environment.getAt(distance, "super");

    LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");

    LoxFunction method = superclass.findMethod(expr.method.lexeme);

    if (method == null) {
      throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
    }

    return method.bind(object);
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      default:
        return null;
    }
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private String stringify(Object object) {
    if (object == null) return "nil";
  
    if (object instanceof Double) {
      String text = object.toString();
      // Remove ".0" for integer-looking doubles
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
  
    return object.toString();
  }  
}
