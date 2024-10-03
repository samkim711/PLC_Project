private static Stream<Arguments> testExamples() {
    return Stream.of(
            Arguments.of("Example 1", "LET x = -5;", Arrays.asList(
                    new Token(Token.Type.IDENTIFIER, "LET", 0),
                    new Token(Token.Type.IDENTIFIER, "x", 4),
                    new Token(Token.Type.OPERATOR, "=", 6),
                    new Token(Token.Type.INTEGER, "-5", 8),
                    new Token(Token.Type.OPERATOR, ";", 10)
            )),
            Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                    new Token(Token.Type.IDENTIFIER, "print", 0),
                    new Token(Token.Type.OPERATOR, "(", 5),
                    new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                    new Token(Token.Type.OPERATOR, ")", 21),
                    new Token(Token.Type.OPERATOR, ";", 22)
            )),
            Arguments.of("Example 3", "i = i + inc;",
                    Arrays.asList(
                            new Token(Token.Type.IDENTIFIER, "i", 0),
                            new Token(Token.Type.OPERATOR, "=", 2),
                            new Token(Token.Type.IDENTIFIER, "i", 4),
                            new Token(Token.Type.OPERATOR, "+", 6),
                            new Token(Token.Type.IDENTIFIER, "inc", 8),
                            new Token(Token.Type.OPERATOR, ";", 11)
                    )),
            Arguments.of("Example 4", "LET i = -1 : Integer;\nLET inc = 2 : Integer;\nDEF foo() DO\n    WHILE i != 1 DO\n        IF i > 0 DO\n            print(\"bar\");\n",
                    Arrays.asList(
                            //LET i = -1 : Integer;
                            new Token(Token.Type.IDENTIFIER, "LET", 0),
                            new Token(Token.Type.IDENTIFIER, "i", 4),
                            new Token(Token.Type.OPERATOR, "=", 6),
                            new Token(Token.Type.INTEGER, "-1", 8),
                            new Token(Token.Type.OPERATOR, ":", 11),
                            new Token(Token.Type.IDENTIFIER, "Integer", 13),
                            new Token(Token.Type.OPERATOR, ";", 20),

                            //LET inc = 2 : Integer;
                            new Token(Token.Type.IDENTIFIER, "LET", 22),
                            new Token(Token.Type.IDENTIFIER, "inc", 26),
                            new Token(Token.Type.OPERATOR, "=", 30),
                            new Token(Token.Type.INTEGER, "2", 32),
                            new Token(Token.Type.OPERATOR, ":", 34),
                            new Token(Token.Type.IDENTIFIER, "Integer", 36),
                            new Token(Token.Type.OPERATOR, ";", 43),

                            //DEF foo() DO
                            new Token(Token.Type.IDENTIFIER, "DEF", 45),
                            new Token(Token.Type.IDENTIFIER, "foo", 49),
                            new Token(Token.Type.OPERATOR, "(", 52),
                            new Token(Token.Type.OPERATOR, ")", 53),
                            new Token(Token.Type.IDENTIFIER, "DO", 55),

                            // WHILE i != 1 DO
                            new Token(Token.Type.IDENTIFIER, "WHILE", 62),
                            new Token(Token.Type.IDENTIFIER, "i", 68),
                            new Token(Token.Type.OPERATOR, "!=", 70),
                            new Token(Token.Type.INTEGER, "1", 73),
                            new Token(Token.Type.IDENTIFIER, "DO", 75),

                            // IF i > 0 DO
                            new Token(Token.Type.IDENTIFIER, "IF", 86),
                            new Token(Token.Type.IDENTIFIER, "i", 89),
                            new Token(Token.Type.OPERATOR, ">", 91),
                            new Token(Token.Type.INTEGER, "0", 93),
                            new Token(Token.Type.IDENTIFIER, "DO", 95),

                            // print(\"bar\");
                            new Token(Token.Type.IDENTIFIER, "print", 110),
                            new Token(Token.Type.OPERATOR, "(", 115),
                            new Token(Token.Type.STRING, "\"bar\"", 116),
                            new Token(Token.Type.OPERATOR, ")", 121),
                            new Token(Token.Type.OPERATOR, ";", 122)
                    )),
            Arguments.of("Example 5", "\b\n\r\t", Arrays.asList())
    );
}