package plc.project;

import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar arcPLChitecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (peek("LET")) {
            fields.add(parseField());
        }

        while (peek("DEF")) {
            methods.add(parseMethod());
        }

        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        match("LET"); // match LET
        boolean constPresent = match("CONST"); // match potential CONST
        String identifier;
        // if and identifier is missing throw an exception
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", tokens.index);
        }
        identifier = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> expression = Optional.empty();
        if (match("=")) expression = Optional.of(parseExpression());

        // match ;
        if (!match(";")) {
            throw new ParseException("Expected \";\"", tokens.index);
        }

        return new Ast.Field(identifier, constPresent, expression);


    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        match("DEF");

        // if and identifier is missing throw an exception
        String identifier;
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", tokens.index);
        }
        identifier = tokens.get(0).getLiteral();
        tokens.advance();

        // match (
        if (!match("(")) throw new ParseException("Expected '('", tokens.index);

        List<String> parameters = new ArrayList<>();
        if (!peek(")")){
            if (!peek(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier", tokens.index);
            parameters.add(tokens.get(0).getLiteral());
            tokens.advance();
            while(match(",")){
                if (!peek(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier", tokens.index);
                parameters.add(tokens.get(0).getLiteral());
                tokens.advance();
            }
        }

        // match )
        if (!match(")")) throw new ParseException("Expected ')'", tokens.index);
        // match DO
        if (!match("DO")) throw new ParseException("Expected 'DO'", tokens.index);

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        // match END
        if (!match("END")) throw new ParseException("Expected 'END'", tokens.index);

        return new Ast.Method(identifier, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")){
            return parseDeclarationStatement();
        }
        else if (peek("IF")){
            return parseIfStatement();
        }
        else if (peek("FOR")){
            return parseForStatement();
        }
        else if (peek("WHILE")){
            return parseWhileStatement();
        }
        else if (peek("RETURN")){
            return parseReturnStatement();
        }
        else {
            Ast.Expression expression = parseExpression();
            if (!match("=")){
                if (!match(";")) throw new ParseException("Expected ';'", tokens.index);
                return new Ast.Statement.Expression(expression);
            }
            Ast.Expression value = parseExpression();
            if (!match(";")) throw new ParseException("Expected ';'", tokens.index);
            return new Ast.Statement.Assignment(expression, value);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        if (!peek(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier", tokens.index);
        String identifier = tokens.get(0).getLiteral();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")){
            value = Optional.of(parseExpression());
        }
        if (!match(";")) throw new ParseException("Expected ';'", tokens.index);

        return new Ast.Statement.Declaration(identifier, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expression expression = parseExpression();
        if (!match("DO")) throw new ParseException("Expected 'DO'", tokens.index);

        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }

        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                elseStatements.add(parseStatement());
            }
        }

        if (!match("END")) throw new ParseException("Expected 'END'", tokens.index);

        return new Ast.Statement.If(expression, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        match("FOR");
        if (!match("(")) throw new ParseException("Expected '('", tokens.index);

        Ast.Statement.Declaration statmentDeclaration = null;
        if (!peek(";")){
            if (!peek(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier", tokens.index);
            String identifier = tokens.get(0).getLiteral();
            if (!match("=")) throw new ParseException("Expected '='", tokens.index);
            Ast.Expression value = parseExpression();
            statmentDeclaration = new Ast.Statement.Declaration(identifier, Optional.of(value));
        }

        if (!match(";")) throw new ParseException("Expected ';'", tokens.index);

        Ast.Expression conditionExpression = parseExpression();

        if (!match(";")) throw new ParseException("Expected ';'", tokens.index);

        Ast.Statement.Assignment incrementStatement = null;
        if (!peek(")")){
            Ast.Expression identifier = parseExpression();
            if (!match("=")) throw new ParseException("Expected '='", tokens.index);
            Ast.Expression value = parseExpression();
            incrementStatement = new Ast.Statement.Assignment(identifier, value);

        }

        if (!match(")")) throw new ParseException("Expected ')'", tokens.index);

        List<Ast.Statement> finalStatements = new ArrayList<>();
        while (!peek("END")) {
            finalStatements.add(parseStatement());
        }
        if (!match("END")) throw new ParseException("Expected 'END'", tokens.index);

        return new Ast.Statement.For(statmentDeclaration, conditionExpression, incrementStatement, finalStatements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression conditionExpression = parseExpression();
        if (!match("DO")) throw new ParseException("Expected 'DO'", tokens.index);
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        if (!match("END")) throw new ParseException("Expected 'END'", tokens.index);
        return new Ast.Statement.While(conditionExpression, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expression expression = parseExpression();
        if (!match(";")) throw new ParseException("Expected ';'", tokens.index);
        return new Ast.Statement.Return(expression);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression logicalExpression = parseEqualityExpression();
        while (peek("&&") || peek("||")){
            String logicalOperator = tokens.get(0).getLiteral();
            match(logicalOperator);
            Ast.Expression tempLogicalExpression = parseEqualityExpression();
            logicalExpression = new Ast.Expression.Binary(logicalOperator, logicalExpression, tempLogicalExpression);
        }

        return logicalExpression;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression additiveExpression = parseAdditiveExpression();
        while (peek("<") || peek("<=") || peek(">") || peek(">=") || peek("==") || peek("!=")){
            String additiveOperator = tokens.get(0).getLiteral();
            match(additiveOperator);
            Ast.Expression tempAdditiveExpression = parseAdditiveExpression();
            additiveExpression = new Ast.Expression.Binary(additiveOperator, additiveExpression, tempAdditiveExpression);
        }

        return additiveExpression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression multiExpression = parseMultiplicativeExpression();
        while (peek("+") || peek("-")){
            String addOperator = tokens.get(0).getLiteral();
            match(addOperator);
            Ast.Expression tempMuliExpression = parseMultiplicativeExpression();
            multiExpression = new Ast.Expression.Binary(addOperator, multiExpression, tempMuliExpression);
        }

        return multiExpression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression secondExpression = parseSecondaryExpression();
        while (peek("*") || peek("/")){
            String secondOperator = tokens.get(0).getLiteral();
            match(secondOperator);
            Ast.Expression tempSecondExpression = parseSecondaryExpression();
            secondExpression = new Ast.Expression.Binary(secondOperator, secondExpression, tempSecondExpression);
        }

        return secondExpression;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression primaryExpression = parsePrimaryExpression();
        while (peek(".")){
            match(".");
            if (!peek(Token.Type.IDENTIFIER)) throw new ParseException("Expected identifier", tokens.index);
            String identifier = tokens.get(0).getLiteral();
            if (peek("(")){
                match('(');
                List<Ast.Expression> parameters = new ArrayList<>();
                if (!peek(")")) {
                    parameters.add(parseExpression());
                    while (match(",")) {
                        parameters.add(parseExpression());
                    }
                }
                if (!match(")")) throw new ParseException("Expected ')'", tokens.index);
                primaryExpression = new Ast.Expression.Function(Optional.of(primaryExpression), identifier, parameters);
            }
            else primaryExpression = new Ast.Expression.Access(Optional.of(primaryExpression), identifier);
        }

        return primaryExpression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")){
            return new Ast.Expression.Literal(null);
        }
        else if (match("TRUE")){
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if (match("FALSE")){
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if (peek(Token.Type.INTEGER)){
            Token token = tokens.get(0);
            tokens.advance();
            return new Ast.Expression.Literal(new BigInteger(token.getLiteral()));        }
        else if (peek(Token.Type.DECIMAL)){
            Token token = tokens.get(0);
            tokens.advance();
            return new Ast.Expression.Literal(new BigDecimal(token.getLiteral()));        }
        else if (peek(Token.Type.CHARACTER)) {
            Token token = tokens.get(0);
            tokens.advance();
            String literal = token.getLiteral();
            literal = literal
                    .replace("\\b", "\b")  // Backspace
                    .replace("\\n", "\n")  // Newline
                    .replace("\\r", "\r")  // Carriage return
                    .replace("\\t", "\t")  // Tab
                    .replace("\\\"", "\"")  // Escaped double-quote
                    .replace("\\'", "'")    // Escaped single-quote
                    .replace("\\\\", "\\"); // Escaped backslash
            return new Ast.Expression.Literal(token.getLiteral().charAt(1));
        }
        else if (peek(Token.Type.STRING)){
            Token token = tokens.get(0);
            tokens.advance();
            String literal = token.getLiteral();
            //System.out.println(literal.substring(1, literal.length() - 1));
            literal = literal
                    .replace("\\b", "\b")  // Backspace
                    .replace("\\n", "\n")  // Newline
                    .replace("\\r", "\r")  // Carriage return
                    .replace("\\t", "\t")  // Tab
                    .replace("\\\"", "\"")  // Escaped double-quote
                    .replace("\\'", "'")    // Escaped single-quote
                    .replace("\\\\", "\\"); // Escaped backslash
            return new Ast.Expression.Literal(literal.substring(1, literal.length() - 1));
        }
        else if (match("(")){
            Ast.Expression expression = parseExpression();

            if (!match(")")) throw new ParseException("Expected ')'", tokens.index);
            return new Ast.Expression.Group(expression);
        }
        else if (peek(Token.Type.IDENTIFIER)) {
            String identifier = tokens.get(0).getLiteral();
            tokens.advance();
            if (peek("(")){
                match("(");
                List<Ast.Expression> parameters = new ArrayList<>();
                if (!peek(")")) {
                    parameters.add(parseExpression());
                    while (peek(",")) {
                        match(",");
                        parameters.add(parseExpression());
                    }
                }

                if (!match(")")) {
                    System.out.println(tokens.index);
                    throw new ParseException("Expected ')'", tokens.index);
                }
                return new Ast.Expression.Function(Optional.empty(), identifier, parameters);
            }
            else {
                return new Ast.Expression.Access(Optional.empty(), identifier);
            }
        }
        else {
            //asdf
            throw new ParseException("Invalid primary expression", tokens.index);
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)){
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            } else if (patterns[i] instanceof String){
                if (!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek){
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

        public boolean isEmpty() {
            return !has(1);
        }
    }

}
