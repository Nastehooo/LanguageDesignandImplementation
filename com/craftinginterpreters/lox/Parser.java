package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    // Exception used internally for parse errors
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    private final boolean debug = true; // Toggle this to enable/disable debug logs

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Entry point: Parses a list of statements from tokens.
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /**
     * Parses a declaration (e.g., variable declaration).
     * If an error occurs, synchronize to prevent cascading failures.
     */
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            log("[declaration] Parse error occurred, synchronizing...");
            synchronize();
            return null;
        }
    }

    /**
     * Parses a variable declaration: `var name = value;`
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * Parses a general statement (e.g., print, if, while, block, or expression).
     */
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return block();
        return expressionStatement();
    }

    /**
     * Parses an `if` statement: `if (condition) { ... } else { ... }`
     */
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

    /**
     * Parses a `while` loop: `while (condition) { body }`
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        Stmt body = statement(); // Body can be a block or single statement
        return new Stmt.While(condition, body);
    }

    /**
     * Parses a block of statements wrapped in `{ ... }`
     */
    private Stmt block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return new Stmt.Block(statements);
    }

    /**
     * Parses a print statement: `print expression;`
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * Parses a plain expression followed by a semicolon.
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * Parses an expression, starting from the highest precedence: assignment.
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * Parses an assignment expression like `a = b`.
     */
    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            log("[assignment] Left expression: " + expr.getClass().getSimpleName());

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * Parses equality expressions: `==`, `!=`
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses comparison expressions: `<`, `<=`, `>`, `>=`
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses addition and subtraction: `+`, `-`
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses multiplication and division: `*`, `/`
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses unary expressions: `-x`, `!x`
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    /**
     * Parses literals, variable names, or grouped expressions.
     */
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

        throw error(peek(), "Expect expression.");
    }

    /**
     * If the current token matches any of the given types, consume it and return true.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                log("[match] Matched token: " + type);
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes a token if it matches the expected type; otherwise throws a parse error.
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            Token token = advance();
            log("[consume] Consumed token: " + token.lexeme);
            return token;
        }

        throw error(peek(), message);
    }

    /**
     * Returns true if the current token is of the given type.
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Advances the parser to the next token and returns the previous one.
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        Token previousToken = previous();
        log("[advance] Advanced to token: " + peek().lexeme);
        return previousToken;
    }

    /**
     * Returns true if we’ve consumed all tokens.
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * Returns the current token.
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the token just before the current one.
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Reports an error at a specific token.
     */
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        log("[error] Error at token '" + token.lexeme + "': " + message);
        return new ParseError();
    }

    /**
     * Skips tokens until we find a statement boundary.
     */
    private void synchronize() {
        log("[synchronize] Synchronizing at token: " + peek().lexeme);
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                log("[synchronize] Found semicolon, resuming normal parsing.");
                return;
            }

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    log("[synchronize] Found statement boundary: " + peek().lexeme);
                    return;
                default:
                    break;
            }

            log("[synchronize] Skipping token: " + peek().lexeme);
            advance();
        }

        log("[synchronize] Reached end of tokens while synchronizing.");
    }

    /**
     * Outputs a debug log message if debug is enabled.
     */
    private void log(String message) {
        if (debug) System.out.println(message);
    }
}
