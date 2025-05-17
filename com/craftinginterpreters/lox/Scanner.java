package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*; // Import token types statically

class Scanner {
    // A map to store all reserved keysowrds in the Lox language and their corresponding token types
    private static final Map<String, TokenType> keywords; 


    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    // The source code string to scan 
    private final String source; 

    // The list of tokens produced from the Source code
    private final List<Token> tokens = new ArrayList<>();

    // Indices and line tracker 
    private int start = 0; // Start index of the curret lexeme 
    private int current = 0; // Current character index 
    private int line = 1; // Line number for error reporting 

    Scanner(String source) {
        this.source = source; 
    }

    // Main method to scan the entire source and return a list of tokens 
    List<Token> scanTokens(){
        while (!isAtEnd()) {
            start = current; // Start of a new lexeme 
            scanToken();
        }
        // Add end-of-file token at the end
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    // Scans a single token and adds it to the token list
    private void scanToken() {
        char c = advance(); // Read the next character

        switch (c) {
        case '(': addToken(LEFT_PAREN); break;
        case ')': addToken(RIGHT_PAREN); break;
        case '{': addToken(LEFT_BRACE); break;
        case '}': addToken(RIGHT_BRACE); break;
        case ',': addToken(COMMA); break;
        case '.': addToken(DOT); break;
        case '-': addToken(MINUS); break;
        case '+': addToken(PLUS); break;
        case ';': addToken(SEMICOLON); break;
        case '*': addToken(STAR); break;

        // Handle one or two character operators
        case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
        case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
        case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
        case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

        // Handle single-line comments or division
        case '/':
            if (match('/')) {
            // Skip until end of line for comments
            while (peek() != '\n' && !isAtEnd()) advance();
            } else {
            addToken(SLASH); // It's a division operator
            }
            break;

        // Ignore whitespace characters
        case ' ':
        case '\r':
        case '\t':
            break;

        case '\n':
            line++; // Newline: increment line count
            break;

        // Handle string literals
        case '"': string(); break;

        default:
            // Check if character starts a number or identifier
            if (isDigit(c)) {
            number();
            } else if (isAlpha(c)) {
            identifier();
            } else {
            // If it's an unknown character, report an error
            Lox.error(line, "Unexpected character.");
            }
            break;
        }
    }

    // Handles identifiers and reserved keywords
    private void identifier() {
        while (isAlphaNumeric(peek())) advance(); // Consume rest of the identifier

        String text = source.substring(start, current);
        TokenType type = keywords.get(text); // Check if it's a keyword
        if (type == null) type = IDENTIFIER; // If not, it's a normal identifier
        addToken(type);
    }

    // Handles numeric literals (integers and decimals)
    private void number() {
        while (isDigit(peek())) advance();

        // Look for a decimal part
        if (peek() == '.' && isDigit(peekNext())) {
        advance(); // Consume the dot
        while (isDigit(peek())) advance();
        }

        // Parse the number and add the token
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // Handles string literals
    private void string() {
        // Consume characters until closing quote or end of file
        while (peek() != '"' && !isAtEnd()) {
        if (peek() == '\n') line++; // Handle multiline strings
        advance();
        }

        if (isAtEnd()) {
        Lox.error(line, "Unterminated string.");
        return;
        }

        advance(); // Consume the closing quote

        // Extract the string value without quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // Check if next character matches the expected one
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // Peek at the current character without consuming it
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Peek ahead to the next character without consuming
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // Check if character is a letter or underscore
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    // Check if character is a letter, digit, or underscore
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // Check if character is a digit
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // Check if we have reached the end of the source
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // Read the next character and advance the current index
    private char advance() {
        return source.charAt(current++);
    }

    // Add a token with no literal value
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // Add a token with a literal value
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current); // Get token text
        tokens.add(new Token(type, text, literal, line)); // Create and add token
    }
}

