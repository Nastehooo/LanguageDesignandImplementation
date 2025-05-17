package com.craftinginterpreters.lox;

//Import neccessary Java utility classes
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;


/**
 * The Interpreter class implements both the Expr.Visitor and Stmt.Visitor interfaces.
 * This allows it to evaluate expressions and execute statements in the Lox Language.
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
    
    // The global environment (top-level scope), used to store global variables and functions. 
    final Environment globals = new Environment();

    // The current environment, used for block scoping (can change during execution).
    private Environment environment = globals; 

    // Stores the resolution depth for each expression, used for lexical scoping.
    private final Map<Expr, Integer> locals = new HashMap<>();

    /**
     * Constructor initialises the interpreter with native functons (e.g., "clock").
     */

    Interpreter(){
        globals.define("clock", new LoxCallable() {
            @Override
            public init arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString(){
                return "<native fn>";
            }

        });
    }
    /**
     * Entry point to interpret a list of statements.
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // Evaluates an expression by visiting it
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // Executes a statement by visiting it
    private void execute (Stmt stmt) {
        stmt.accept(this);
    }

    // Used by the Resolver to record variable scope depth
    void resolve(Expr expr, int depth){
        locals.put(expr, depth);
    }

    /**
     * Executes a block of statements within a new environment. 
     * Used for '{}' blocks and function bodies. 
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute (statement);
            }
        } finally {
            this.environment = previous; 
        }
    }

    // Block statement execution 
    @Override
    public Void visitClassStmt(Stmt.Class stmt){
        Object superclass = null; 


        //Evaluate the superclass, if any
        if (stmt.superclass != null){
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
            
            }
        }


        // Define the class name in the environment before evaluating methods
        environment.define(stmt.name.lexeme, null);

        // If there's a superclass, create a new scope for 'super'
        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);

        }

        // Collect all methods in the class
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods){
            boolean isInitialiser = method.name.lexeme.equals("init");
            LoxFunction function = new LoxFunction(method, environment, isInitialiser);
            methods.put(method.name.lexeme, function);
        }

        // Create the class object
        LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);

        // Restore the environment if a superclass was set
        if (superclass != null) {
            environment = environment.enclosing;
        }

        // Assign the class object to its name in the environment 
        environment.assign(stmt.name, klass);
        return null;
    }  

    // Expression statements (just evaluate the expression)
    @Override 
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null; 
    }   

    // If-else branching logic
    @Override
    public Void visitIfStmt(Stmt.If stmt){
        if (isTruthy(evaluate(stmt.condition))){
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null){
            execute(stmt.elseBranch);
        }
        return null;
    }

    // Print statement 
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.printIn(stringify(value));
        return null; 
    }

    // Return statement in functions 
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null; 
        if (stmt.value != null) value = evaluate (stmt.value);
        throw new Return(value); // Use the Return exception to exit function 
    }

    // Variable Function 
    @Override 
    public Void visitVarstmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initialiser != null){
            value = evaluate(stmt.initialiser);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    // While loop statement 
    @Override
    public Void visitWhileStmt(Stmt.While stmt){
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null; 
    }

    // Variable assignment with scope resolution
    @Override 
    public Object visitAssignExpr(Expr.Assign expr){
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null){
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value; 
    }

    // Binary expressions: +, -, *, /, comparison, etc. 
    @Override 
    public Object visitBinaryExpr(Expr.Binary expr){
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return !isEqual(left, right);
            
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
                if (left instanceof String && right instanceof String){
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, 
                     "Operands must be two numbers or two strings. ");
                case SLASH: 
                    checkNumberOperands(expr.operator, left, right);
                    return (double)left / (double)right;
                case STAR:
                    checkNumberOperands(expr.operator, left, right);
                    return (double)left * (double)right;            
            
        }
        return null; // Should not reach here

    }

    // Function or class call expressions 
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        
        if (!(callee instanceof LoxCallable)){
            throw new RuntimeError(expr.paren, 
            "Can only call functions and classes.");
        }


        LoxCallable function = (LoxCallable)callee; 
        if (arguments.size() != function.arrity()) {
            throw new RuntimeError(expr.paren, 
            "Expected " + function.arity() + "arguments but got " + 
            arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    // Helper: check if value is truthy 
    private boolean isTruthy(Object object) {
        if (object == null) return false; 
        if (object instanceof Boolean) return (boolean)object; 
        return true;
    }

    // Helper: check for equality 
    private boolean isEqualy(Object a, Object b){
        if(a == null && b == null ) return true;
        if (a == null )return false;
        return a.equals(b);
    }

    // Helper: convert any object to string ( used in print and REPL)
    private String stringify(Object object){
        if (object == null) return "nil";
        if (object instanceof Double){
            String text = object.toString();
            if (text.endsWith(".0")){
                text = text.substring(0, text.lenght() - 2);
            }
            return text; 
        }
        return object.toString();
    }

    // Helper: check that operands are numbers 
    private void checkNumberOperands(Token operator, Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
}
