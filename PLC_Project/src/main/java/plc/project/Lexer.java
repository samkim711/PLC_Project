package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) { //has characters left
            while (chars.has(0) && peek(" ")) { //has whitespace
                match(" "); // skip it
                chars.skip();
            }

            if (chars.has(0)) { // has character left
                //System.out.println(chars.index);
                tokens.add(lexToken()); // next token
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")) { // letter of underscore then identifier
            return lexIdentifier();
        } else if (peek("[+-]") || peek("[0-9]")) { // starts with integer or +-
            return lexNumber();
        } else if (peek("'")) { // starts with single quote
            return lexCharacter();
        } else if (peek("\"")) { // starts with a double quote
            return lexString();
        }
        else {
            return lexOperator(); // anything else is an operator
        }
    }

    /**
     * Lexes an identifier, which can start with letters or underscores,
     * and can contain alphanumeric characters, underscores, and hyphens.
     */
    public Token lexIdentifier() {
        if (!peek("[A-Za-z_]")) {
            throw new ParseException("Invalid identifier start", chars.index);
        }
        while (peek("[A-Za-z0-9_-]")) {
            match("[A-Za-z0-9_-]");
        }
        //System.out.println(chars.index + " " + chars.length);
        return chars.emit(Token.Type.IDENTIFIER);
    }

    /**
     * Lexes numbers, allowing both integers and decimals, including
     * negative values.
     */
    public Token lexNumber() {
        if (peek("[+-]"))
            match("[+-]");

        boolean startZero = false;
        if (peek("0")) {
            peek("0");
            startZero = true;
        }

        // Integer part
        if (!peek("[0-9]")) {
            throw new ParseException("Expected digit", chars.index);
        }
        while (peek("[1-9]")) {
            match("[1-9]");
        }

        // Check for decimal point
        if (peek("\\.")) {
            match("\\.");
            if (!peek("[0-9]")) {
                throw new ParseException("Expected digit after decimal point", chars.index);
            }
            while (peek("[0-9]")) {
                match("[0-9]");
            }
            return chars.emit(Token.Type.DECIMAL);
        }
        if (startZero)
            throw new ParseException("Starting Zero not allowed", chars.index);
        return chars.emit(Token.Type.INTEGER);
    }

    /**
     * Lexes character literals, which are enclosed by single quotes
     * and support escape sequences.
     */
    public Token lexCharacter() {
        match("'"); // Opening single quote
        if (peek("\\\\")) { // Check for escape sequence
            lexEscape();
        } else if (peek("[^']")) { // Single character that's not a single quote
            match("[^']");
        } else {
            throw new ParseException("Invalid character literal", chars.index);
        }
        if (!match("'")) { // Closing single quote
            throw new ParseException("Unterminated character literal", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
    }

    /**
     * Handles escape sequences for both character and string literals.
     */
    public void lexEscape() {
        match("\\\\"); // Backslash
        if (!match("[bnrt'\"\\\\]")) { // Valid escape characters
            throw new ParseException("Invalid escape sequence", chars.index);
        }
    }

    /**
     * Lexes string literals, which are enclosed by double quotes
     * and support escape sequences.
     */
    public Token lexString() {
        match("\""); // Opening double quote
        while (!peek("\"")) {
            if (peek("\\\\")) {
                lexEscape(); // Handle escape sequences
            } else if (peek("[^\"]")) {
                match("[^\"]"); // Regular characters
            } else {
                throw new ParseException("Unterminated string", chars.index);
            }
        }
        match("\""); // Closing double quote
        return chars.emit(Token.Type.STRING);
    }

    /**
     * Lexes operators, which can be single characters or two-character
     * comparison or compound operators (e.g., !=, ==, &&, ||).
     */
    public Token lexOperator() {

        if (peek("!", "=") || peek("=", "=") || peek("<", "=") || peek(">", "=") || peek("&", "&") || peek("\\|", "\\|")) {
            match(".", ".");
        }
        else if (peek("[^\\s]")) { // Any other non-whitespace single character
            match("[^\\s]");
        }
        else
            throw new ParseException("Invalid operator", chars.index);

        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters matches the given patterns,
     * which should be a regex.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true.
     */
    public boolean match(String... patterns) {
        if (peek(patterns)) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance(); // Advance the char stream if matches
            }
            return true;
        }
        return false;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}
