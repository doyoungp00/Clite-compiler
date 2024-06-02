public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.

    Token token;          // current token from the input stream
    Lexer lexer;
    String funcId;

    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();           // display abstract syntax tree
    } //main

    private String match(TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }

    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok
                + "; saw: " + token);
        System.exit(1);
    }

    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok
                + "; saw: " + token);
        System.exit(1);
    }

    public Program program() {
        // Program --> { Type Identifier FunctionOrGlobal } MainFunction
        Program prog = new Program();
        prog.globals = new Declarations();
        prog.functions = new Functions();

        Type t;
        Declaration d;
        Function f;

        while (!token.type().equals(TokenType.Eof)) {
            t = type();
            if (token.type().equals(TokenType.Identifier) || token.type().equals(TokenType.Main)) {
                d = new Declaration(new Variable(token.value()), t);
                token = lexer.next();
                TokenType tt = token.type();

                // 전역변수 선언일 경우
                if (tt.equals(TokenType.Comma) || tt.equals(TokenType.Semicolon)) {
                    prog.globals.add(d);
                    while (token.type().equals(TokenType.Comma)) {
                        token = lexer.next();
                        d = new Declaration(new Variable(token.value()), t);
                        prog.globals.add(d);
                        token = lexer.next();
                    }
                    token = lexer.next();
                }
                // 함수 선언일 경우
                else if (tt.equals(TokenType.LeftParen)) {
                    funcId = d.v.toString();
                    f = new Function(funcId, t);
                    token = lexer.next(); // '(' 소모
                    f = functionRest(f); // 함수 몸체 파싱
                    prog.functions.add(f);
                } else error("FunctionOrGlobal");
            } else error("Identifier");
        }
        return prog;
    }

    private Function functionRest(Function f) {
        // functionRest --> ( Parameters ) { Declarations Statements }
        // Parameters --> [ Parameter { , Parameter } ]
        f.params = new Declarations();
        while (isType()) {
            f.params = parameter(f.params);
            if (token.type().equals(TokenType.Comma))
                match(TokenType.Comma);
        }
        match(TokenType.RightParen);
        match(TokenType.LeftBrace);
        f.locals = declarations();
        f.body = new Block();
        while (!token.type().equals(TokenType.RightBrace) && !token.type().equals(TokenType.Eof))
            f.body.members.add(statement());
        match(TokenType.RightBrace);
        return f;
    }

    private Declarations parameter(Declarations params) {
        // Parameter --> Type Identifier
        Variable v = null;
        Type t = new Type(token.value());

        token = lexer.next();
        if (token.type().equals(TokenType.Identifier))
            v = new Variable(match(TokenType.Identifier));
        else error("Identifier");
        params.add(new Declaration(v, t));
        return params;
    }

    private Declarations declarations() {
        // Declarations --> { Declaration }
        // 여러 개의 선언문을 인식해야 함
        // 타입이 나오면 ds에 추가
        Declarations ds = new Declarations();
        while (isType()) declaration(ds);

        return ds;
    }

    private void declaration(Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        Type t = type();
        do {
            // 변수명으로 Variable 생성 후 Declaration도 생성
            Variable v = new Variable(match(TokenType.Identifier));
            ds.add(new Declaration(v, t));
            // 변수 이름이 더 나오지 않으면 break
            if (!token.type().equals(TokenType.Comma))
                break;
            match(TokenType.Comma); // ',' 소모 후 반복
        } while (true);
        // 마지막 세미콜론 소모
        match(TokenType.Semicolon);
    }

    private Type type() {
        // Type  -->  int | bool | float | char | void
        Type t = null;
        // 토큰의 TokenType을 보고 그에 맞는 Type 반환
        switch (token.type()) {
            case Int:
                t = Type.INT;
                break;
            case Bool:
                t = Type.BOOL;
                break;
            case Float:
                t = Type.FLOAT;
                break;
            case Char:
                t = Type.CHAR;
                break;
            case Void:
                t = Type.VOID;
                break;
            default:
                error("int | bool | float | char | void");
        }
        match(token.type()); // 토큰 소모
        return t;
    }

    private Statement statement() {
        // Statement --> ; | Block | Assignment | Return | IfStatement | WhileStatement
        Statement s = new Skip();
        switch (token.type()) {
            case Semicolon:
                // 세미콜론 소모 후 Skip 그대로 반환
                match(TokenType.Semicolon);
                break;
            case LeftBrace:
                s = statements();
                break;
            case Identifier:
                s = assignOrCall();
                break;
            case Return:
                s = returnStatement();
                break;
            case If:
                s = ifStatement();
                break;
            case While:
                s = whileStatement();
                break;
            default:
                error("; | { | Identifier | if | while");
        }
        return s;
    }

    private Block statements() {
        // Block --> '{' Statements '}'
        Block b = new Block();
        match(TokenType.LeftBrace); // '{'
        // 블럭이나 파일이 끝나기 전까지 Statement 추가
        while (!token.type().equals(TokenType.RightBrace) && !token.type().equals(TokenType.Eof))
            b.members.add(statement());
        match(TokenType.RightBrace); // '}'

        return b;
    }

    private Conditional ifStatement() {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        Conditional c = null;
        Expression test;
        Statement thenbranch, elsebranch;

        match(TokenType.If); // 'if'
        match(TokenType.LeftParen); // '('
        test = expression(); // 조건식
        match(TokenType.RightParen); // ')'
        thenbranch = statement(); // thenbranch

        // else문 검사
        if (token.type().equals(TokenType.Else)) { // if-then-else
            match(TokenType.Else); // else 소모
            elsebranch = statement();
            c = new Conditional(test, thenbranch, elsebranch);
        } else { // if-then
            c = new Conditional(test, thenbranch);
        }
        return c;
    }

    private Loop whileStatement() {
        // WhileStatement --> while ( Expression ) Statement
        match(TokenType.While);
        match(TokenType.LeftParen);
        Expression test = expression();
        match(TokenType.RightParen);
        Statement body = statement();
        return new Loop(test, body);
    }

    private Statement assignOrCall() {
        Variable v = new Variable(token.value());

        token = lexer.next(); // 식별자 스킵

        // 대입문
        if (token.type().equals(TokenType.Assign)) {
            token = lexer.next();
            Expression src = expression();
            match(TokenType.Semicolon);
            return new Assignment(v, src);
        }
        // 호출문
        else if (token.type().equals(TokenType.LeftParen)) {
            String n = v.toString();
            token = lexer.next();
            Expressions a = arguments();
            match(TokenType.RightParen);
            match(TokenType.Semicolon);
            return new Call(n, a);
        } else error("assignOrCall");
        return null;
    }

    private Expressions arguments() {
        // Arguments --> [ Expression { , Expression } ]
        Expressions args = new Expressions();
        while (!token.type().equals(TokenType.RightParen)) {
            args.add(expression());
            if (token.type().equals(TokenType.Comma))
                match(TokenType.Comma);
            else if (!token.type().equals(TokenType.RightParen))
                error("Expression");
        }
        if (args.isEmpty()) args = null;
        return args;
    }


    private Return returnStatement() {
        match(TokenType.Return);
        Variable t = new Variable(funcId);
        Expression r = expression();
        match(TokenType.Semicolon);
        return new Return(t, r);
    }

    private Expression expression() {
        // Expression --> Conjunction { || Conjunction }
        Expression result = conjunction();
        while (token.type().equals(TokenType.Or)) {
            Operator op = new Operator(match(TokenType.Or)); // || 소모
            Expression right = conjunction(); // or의 우변 처리
            result = new Binary(op, result, right); // or의 좌변과 우변을 Binary 객체로 변환
        }
        return result;
    }

    private Expression conjunction() {
        // Conjunction --> Equality { && Equality }
        Expression result = equality();
        while (token.type().equals(TokenType.And)) {
            Operator op = new Operator(match(TokenType.And)); // && 소모
            Expression right = equality(); // and의 우변 처리
            result = new Binary(op, result, right); // and의 좌변과 우변을 Binary 객체로 변환
        }
        return result;
    }

    private Expression equality() {
        // Equality --> Relation [ EquOp Relation ]
        Expression result = relation();
        while (isEqualityOp()) {
            Operator op = new Operator(match(token.type())); // ==이나 !=를 소모
            Expression right = relation(); // 우변 처리
            result = new Binary(op, result, right); // 좌변과 우변을 Binary 객체로 변환
        }
        return result;
    }

    private Expression relation() {
        // Relation --> Addition [RelOp Addition]
        Expression result = addition();
        while (isRelationalOp()) {
            Operator op = new Operator(match(token.type())); // 관계 연산자 소모
            Expression right = addition(); // 우변 처리
            result = new Binary(op, result, right); // 좌변과 우변을 Binary 객체로 변환
        }
        return result;
    }

    private Expression addition() {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression term() {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        } else return primary();
    }

    private Expression primary() {
        // Primary --> Identifier | Literal | ( Expression ) | Type ( Expression ) | Call
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            Variable v = new Variable(match(TokenType.Identifier));
            e = v;
            if (token.type().equals(TokenType.LeftParen)) {
                token = lexer.next();
                String n = v.toString();
                Expressions a = arguments();
                match(TokenType.RightParen);
                e = new Call(n, a);
            }
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();
            match(TokenType.RightParen);
        } else if (isType()) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal() {
        Value result = null;
        TokenType type = token.type();
        String s = match(type);
        switch (type) {
            case IntLiteral:
                result = new IntValue(Integer.parseInt(s));
                break;
            // BoolLiteral은 없으므로 True와 False 이용
            case True:
                result = new BoolValue(true);
                break;
            case False:
                result = new BoolValue(false);
                break;
            case CharLiteral:
                result = new CharValue(s.charAt(0));
                break;
            case FloatLiteral:
                result = new FloatValue(Float.parseFloat(s));
                break;
            default:
                error("int | bool | char | float");
                break;
        }
        return result;
    }

    private boolean isAddOp() {
        return token.type().equals(TokenType.Plus)
                || token.type().equals(TokenType.Minus);
    }

    private boolean isMultiplyOp() {
        return token.type().equals(TokenType.Multiply)
                || token.type().equals(TokenType.Divide);
    }

    private boolean isUnaryOp() {
        return token.type().equals(TokenType.Not)
                || token.type().equals(TokenType.Minus);
    }

    private boolean isEqualityOp() {
        return token.type().equals(TokenType.Equals)
                || token.type().equals(TokenType.NotEqual);
    }

    private boolean isRelationalOp() {
        return token.type().equals(TokenType.Less)
                || token.type().equals(TokenType.LessEqual)
                || token.type().equals(TokenType.Greater)
                || token.type().equals(TokenType.GreaterEqual);
    }

    private boolean isType() {
        return token.type().equals(TokenType.Int)
                || token.type().equals(TokenType.Bool)
                || token.type().equals(TokenType.Float)
                || token.type().equals(TokenType.Char);
    }

    private boolean isLiteral() {
        return token.type().equals(TokenType.IntLiteral)
                || isBooleanLiteral()
                || token.type().equals(TokenType.FloatLiteral)
                || token.type().equals(TokenType.CharLiteral);
    }

    private boolean isBooleanLiteral() {
        return token.type().equals(TokenType.True)
                || token.type().equals(TokenType.False);
    }

} // Parser
