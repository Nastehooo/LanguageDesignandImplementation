package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Entry point to parse tokens into a list of statements
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // Parses a declaration; handles error recovery with synchronize()
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            System.out.println("[declaration] Parse error occurred, synchronizing...");
            synchronize(); // recover from error
            return null;
        }
    }

    // Parses a variable declaration statement
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    // Parses general statements like print or expression statements
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(IF)) return ifStatement();     
        if (match(WHILE)) return whileStatement();
        return expressionStatement();
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
    
        // Ensure it recognizes block statements
        Stmt body;
        if (match(LEFT_BRACE)) {
            body = block(); // Parse statements inside {}
        } else {
            body = statement(); // Single statement case
        }
    
        return new Stmt.While(condition, body);
    }

    private Stmt block() {
        List<Stmt> statements = new ArrayList<>();
    
        while (!isAtEnd() && !match(RIGHT_BRACE)) {
            statements.add(declaration()); // Parse statements inside the block
        }
    
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return new Stmt.Block(statements); // Wrap in Block statement
    }
    
    
    

    // Parses a print statement
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    // Parses an expression as a statement
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // Parses an expression (starting point for expressions)
    private Expr expression() {
        return assignment();
    }

    // Parses assignment expressions
    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            // Debug: print left expression type and token
            System.out.println("[assignment] Left expression: " + expr.getClass().getSimpleName());

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    // Parses equality expressions (==, !=)
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Parses comparison expressions (<, >, <=, >=)
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Parses addition and subtraction expressions
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Parses multiplication and division expressions
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Parses unary expressions (!, -)
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // Parses primary expressions: literals, variables, parentheses
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

    // Checks if current token matches any of the given types and advances if so
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                System.out.println("[match] Matched token: " + type);
                return true;
            }
        }
        return false;
    }

    // Consumes a token of the expected type or throws an error
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            Token token = advance();
            System.out.println("[consume] Consumed token: " + token.lexeme);
            return token;
        }

        throw error(peek(), message);
    }

    // Checks if current token is of the given type without consuming it
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Advances to the next token and returns the previous one
    private Token advance() {
        if (!isAtEnd()) current++;
        Token previousToken = previous();
        System.out.println("[advance] Advanced to token: " + peek().lexeme);
        return previousToken;
    }

    // Returns true if all tokens have been consumed
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    // Returns the current token without consuming it
    private Token peek() {
        return tokens.get(current);
    }

    // Returns the previous token
    private Token previous() {
        return tokens.get(current - 1);
    }

    // Creates a ParseError and reports the error message
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        System.out.println("[error] Error at token '" + token.lexeme + "': " + message);
        return new ParseError();
    }

    // Synchronizes parser after an error to avoid cascading failures
    private void synchronize() {
        System.out.println("[synchronize] Synchronizing at token: " + peek().lexeme);
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                System.out.println("[synchronize] Found semicolon, resuming normal parsing.");
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
                    System.out.println("[synchronize] Found statement boundary: " + peek().lexeme);
                    return;
                    
                 default:
                 // Continue skipping tokens until we find a boundary
                 break;
            }
                


            System.out.println("[synchronize] Skipping token: " + peek().lexeme);
            advance();
        }

        System.out.println("[synchronize] Reached end of tokens while synchronizing.");
    }
}
