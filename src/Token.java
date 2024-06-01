public class Token {

    public static final Token eofTok = new Token(TokenType.Eof, "<<EOF>>");
    public static final Token boolTok = new Token(TokenType.Bool, "bool");
    public static final Token charTok = new Token(TokenType.Char, "char");
    public static final Token elseTok = new Token(TokenType.Else, "else");
    public static final Token falseTok = new Token(TokenType.False, "false");
    public static final Token floatTok = new Token(TokenType.Float, "float");
    public static final Token ifTok = new Token(TokenType.If, "if");
    public static final Token intTok = new Token(TokenType.Int, "int");
    public static final Token mainTok = new Token(TokenType.Main, "main");
    public static final Token trueTok = new Token(TokenType.True, "true");
    public static final Token voidTok = new Token(TokenType.Void, "void");
    public static final Token returnTok = new Token(TokenType.Return, "return");
    public static final Token whileTok = new Token(TokenType.While, "while");
    public static final Token leftBraceTok = new Token(TokenType.LeftBrace, "{");
    public static final Token rightBraceTok = new Token(TokenType.RightBrace, "}");
    public static final Token leftBracketTok = new Token(TokenType.LeftBracket, "[");
    public static final Token rightBracketTok = new Token(TokenType.RightBracket, "]");
    public static final Token leftParenTok = new Token(TokenType.LeftParen, "(");
    public static final Token rightParenTok = new Token(TokenType.RightParen, ")");
    public static final Token semicolonTok = new Token(TokenType.Semicolon, ";");
    public static final Token commaTok = new Token(TokenType.Comma, ",");
    public static final Token assignTok = new Token(TokenType.Assign, "=");
    public static final Token eqeqTok = new Token(TokenType.Equals, "==");
    public static final Token ltTok = new Token(TokenType.Less, "<");
    public static final Token lteqTok = new Token(TokenType.LessEqual, "<=");
    public static final Token gtTok = new Token(TokenType.Greater, ">");
    public static final Token gteqTok = new Token(TokenType.GreaterEqual, ">=");
    public static final Token notTok = new Token(TokenType.Not, "!");
    public static final Token noteqTok = new Token(TokenType.NotEqual, "!=");
    public static final Token plusTok = new Token(TokenType.Plus, "+");
    public static final Token minusTok = new Token(TokenType.Minus, "-");
    public static final Token multiplyTok = new Token(TokenType.Multiply, "*");
    public static final Token divideTok = new Token(TokenType.Divide, "/");
    public static final Token andTok = new Token(TokenType.And, "&&");
    public static final Token orTok = new Token(TokenType.Or, "||");

    private static final int KEYWORDS = TokenType.Eof.ordinal();
    private static final String[] reserved = new String[KEYWORDS];
    private static final Token[] token = new Token[KEYWORDS];

    static {
        reserved[TokenType.Bool.ordinal()] = "bool";
        reserved[TokenType.Char.ordinal()] = "char";
        reserved[TokenType.Else.ordinal()] = "else";
        reserved[TokenType.False.ordinal()] = "false";
        reserved[TokenType.Float.ordinal()] = "float";
        reserved[TokenType.If.ordinal()] = "if";
        reserved[TokenType.Int.ordinal()] = "int";
        reserved[TokenType.Main.ordinal()] = "main";
        reserved[TokenType.True.ordinal()] = "true";
        reserved[TokenType.Void.ordinal()] = "void";
        reserved[TokenType.Return.ordinal()] = "return";
        reserved[TokenType.While.ordinal()] = "while";

        token[TokenType.Bool.ordinal()] = boolTok;
        token[TokenType.Char.ordinal()] = charTok;
        token[TokenType.Else.ordinal()] = elseTok;
        token[TokenType.False.ordinal()] = falseTok;
        token[TokenType.Float.ordinal()] = floatTok;
        token[TokenType.If.ordinal()] = ifTok;
        token[TokenType.Int.ordinal()] = intTok;
        token[TokenType.Main.ordinal()] = mainTok;
        token[TokenType.True.ordinal()] = trueTok;
        token[TokenType.Void.ordinal()] = voidTok;
        token[TokenType.Return.ordinal()] = returnTok;
        token[TokenType.While.ordinal()] = whileTok;
    }

    private TokenType type;
    private String value = "";

    private Token(TokenType t, String v) {
        type = t;
        value = v;
    }

    public static Token keyword(String name) {
        char ch = name.charAt(0);
        if (ch >= 'A' && ch <= 'Z') return mkIdentTok(name);
        for (int i = 0; i < KEYWORDS; i++)
            if (name.equals(reserved[i])) return token[i];
        return mkIdentTok(name);
    } // keyword

    public static Token mkIdentTok(String name) {
        return new Token(TokenType.Identifier, name);
    }

    public static Token mkIntLiteral(String name) {
        return new Token(TokenType.IntLiteral, name);
    }

    public static Token mkFloatLiteral(String name) {
        return new Token(TokenType.FloatLiteral, name);
    }

    public static Token mkCharLiteral(String name) {
        return new Token(TokenType.CharLiteral, name);
    }

    public static void main(String[] args) {
        System.out.println(eofTok);
        System.out.println(whileTok);
    }

    public TokenType type() {
        return type;
    }

    public String value() {
        return value;
    }

    public String toString() {
        if (type.compareTo(TokenType.Identifier) < 0) return value;
        return type + "\t" + value;
    }
}
