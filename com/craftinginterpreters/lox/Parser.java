package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Parser class: parses tokens into AST statements and expressions.
 */
class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt decl = declaration();
            if (decl != null) {
                statements.add(decl);
            }
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(FUN)) {
                System.out.println("Matched FUN token");
                return function("function");
            }
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    

    

    /**
     * Parses a function declaration.
     * 
     * Grammar:
     * function -> "fun" functionName "(" parameters? ")" block ;
     *
     * @param kind the kind of function (e.g., "function") for error messages
     * @return a Stmt.Function node representing the parsed function declaration
     */

     private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
          do {
            if (parameters.size() >= 255) {
              error(peek(), "Can't have more than 255 parameters.");
            }
            parameters.add(consume(IDENTIFIER, "Expect parameter name."));
          } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
    
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        // Parse a block and extract its list of statements.
        Stmt.Block body = block();
        return new Stmt.Function(name, parameters, body.statements);
      }
    
   
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BREAK)) return breakStatement();
        if (match(CONTINUE)) return continueStatement();
        if (match(DO)) return doWhileStatement();
        if (match(SET)) return setStatement();
        if (match(BUILD)) return buildStatement();
        if (match(WALK)) return walkStatement();
        if (match(CHECK)) return checkStatement();
        if (match(OTHERWISE)) return otherwiseStatement();
        if (match(THRU)) return thruStatement();
        if (match(RETURN)) return returnStatement(); 
        if (match(LEFT_BRACE)) return block();
        return expressionStatement();
    }
    

    private Stmt breakStatement() {
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after 'break'.");
        return new Stmt.Break(keyword);
    }

    private Stmt returnStatement() {
        Token keyword = previous();  // 'return' token
    
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
    
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }
    

    private Stmt continueStatement() {
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after 'continue'.");
        return new Stmt.Continue(keyword);
    }

    private Stmt doWhileStatement() {
        Token doKeyword = previous();
        Stmt body = statement();
        consume(WHILE, "Expect 'while' after 'do' block.");
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        consume(SEMICOLON, "Expect ';' after do-while.");
        return new Stmt.Do(doKeyword, body, condition);
    }

    private Stmt setStatement() {
        Token name = consume(IDENTIFIER, "Expect variable name after 'set'.");
        consume(EQUAL, "Expect '=' after variable name.");
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after 'set' statement.");
        return new Stmt.Set(name, value);
    }

    private Stmt buildStatement() {
        Expr target = expression();
        List<Expr> args = new ArrayList<>();
        if (match(LEFT_PAREN)) {
            if (!check(RIGHT_PAREN)) {
                do {
                    args.add(expression());
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expect ')' after arguments.");
        }
        consume(SEMICOLON, "Expect ';' after 'build' statement.");
        return new Stmt.Build(target, args);
    }

    private Stmt walkStatement() {
        Expr direction = expression();
        consume(SEMICOLON, "Expect ';' after 'walk' statement.");
        return new Stmt.Walk(direction);
    }

    private Stmt checkStatement() {
        Expr condition = expression();
        consume(SEMICOLON, "Expect ';' after 'check' statement.");
        return new Stmt.Check(condition);
    }

    private Stmt otherwiseStatement() {
        consume(SEMICOLON, "Expect ';' after 'otherwise' statement.");
        return new Stmt.Otherwise();
    }

    private Stmt thruStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after 'thru' statement.");
        return new Stmt.Thru(expr);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt.Block block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return new Stmt.Block(statements);
    }
    
    
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = equality();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();  // Use the new call() method instead of primary()
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }
    

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        if (match(LEFT_BRACKET)) return list();
        if (match(LEFT_BRACE)) return dict();
        throw error(peek(), "Expect expression.");
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }
    

    private Expr list() {
        List<Expr> elements = new ArrayList<>();
        if (!check(RIGHT_BRACKET)) {
            do {
                elements.add(expression());
            } while (match(COMMA));
        }
        consume(RIGHT_BRACKET, "Expect ']' after list elements.");
        return new Expr.LoxList(elements);
    }

    private Expr dict() {
        Map<Expr, Expr> entries = new HashMap<>();
        if (!check(RIGHT_BRACE)) {
            do {
                Expr key = expression();
                consume(COLON, "Expect ':' after dictionary key.");
                Expr value = expression();
                entries.put(key, value);
            } while (match(COMMA));
        }
        consume(RIGHT_BRACE, "Expect '}' after dictionary entries.");
        return new Expr.LoxDict(entries);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;

                default:
                    break;
            }
            advance();
        }
    }
}
