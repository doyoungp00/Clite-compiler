// Following is the semantics class:
// The meaning M of a Statement is a State
// The meaning M of a Expression is a Value

public class Semantics {

    State sigmag;
    Functions fs;

    public static void main(String[] args) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();
        System.out.println("\n\nBegin type checking...");
        System.out.println("\n\nType map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals, prog.functions);
        map.display();
        StaticTypeCheck.V(prog);
        Program out = TypeTransformer.T(prog);
        System.out.println("\n\nOutput AST");
        out.display();
        Semantics semantics = new Semantics();
        State state = semantics.M(out);
        System.out.println("\n\nFinal State");
        state.display();
    }

    // 프로그램을 가지고 함수와 글로벌 상태 의미 분석
    State M(Program p) {
        sigmag = new State();
        sigmag = sigmag.allocate(p.globals);
        sigmag.dlink = sigmag.slink = sigmag.a;
        fs = p.functions;
        return M(fs, sigmag);
    }

    // 메인 함수 의미 분석
    // 지역변수를 상태에 추가한 후 몸체 의미분석
    State M(Functions fs, State sigmag) {
        Function main = fs.findFunction("main");
        State sigma = new State(sigmag);
        sigma.dlink = sigmag.a;
        sigma = sigma.allocate(main.locals);
        sigma = M(main.body, sigma);
        sigma = sigma.deallocate(main.locals);
        sigmag = sigmag.onion(sigma);
        return sigmag;
    }

    // 함수 호출 위한 글로벌 상태 복사 및 인수와 지역변수 처리
    State addFrame(State current, Call c, Function f) {
        State s = new State(current);
        s = s.minus(current.a - current.dlink);
        s = s.onion(sigmag);
        s = s.allocate(f.params);
        for (int i = 0; i < f.params.size(); i++) {
            Expression e = c.args.get(i);
            Declaration d = f.params.get(i);
            Variable v = d.v;
            s = s.onion(new State(v, M(e, current)));
        }
        s = s.allocate(f.locals);
        Declarations ds = new Declarations();
        ds.add(new Declaration(f.id, f.t));
        s = s.allocate(ds);
        s.dlink = current.a;
        return s;
    }

    // 함수 종료 후 프레임 제거 및 상태 병합
    State removeFrame(State current, Call c, State former) {
        Function f = fs.findFunction(c.name);
        Declarations ds = new Declarations();
        ds.add(new Declaration(f.id, f.t));
        State s = current.deallocate(ds);
        s = s.deallocate(f.locals);
        s = s.deallocate(f.params);
        s = s.onion(sigmag);
        s = s.plus(former);
        s.dlink = former.dlink;
        return s;
    }

    State M(Statement s, State sigma) {
        if (s instanceof Skip) return M((Skip) s, sigma);
        if (s instanceof Assignment) return M((Assignment) s, sigma);
        if (s instanceof Conditional) return M((Conditional) s, sigma);
        if (s instanceof Loop) return M((Loop) s, sigma);
        if (s instanceof Block) return M((Block) s, sigma);
        if (s instanceof Call) return M((Call) s, sigma);
        if (s instanceof Return) return M((Return) s, sigma);
        throw new IllegalArgumentException("should never reach here");
    }

    State M(Skip s, State state) {
        return state;
    }

    State M(Assignment a, State sigma) {
        return sigma.onion(a.target, M(a.source, sigma));
    }

    State M(Block b, State sigma) {
        int n = b.members.size();
        Statement s;
        for (int i = 0; i < n; i++) {
            s = b.members.get(i);
            sigma = M(s, sigma);
            if (s instanceof Return)
                return sigma;
        }
        return sigma;
    }

    State M(Conditional c, State sigma) {
        if (M(c.test, sigma).boolValue())
            return M(c.thenbranch, sigma);
        else
            return M(c.elsebranch, sigma);
    }

    State M(Loop l, State sigma) {
        if (M(l.test, sigma).boolValue())
            return M(l, M(l.body, sigma));
        else return sigma;
    }

    // Call Statement
    State M(Call c, State sigma) {
        Function f = fs.findFunction(c.name);
        State sigmaPrime = new State(sigma);
        sigmaPrime = addFrame(sigmaPrime, c, f);
        sigmaPrime = M(f.body, sigmaPrime);
        sigmaPrime = removeFrame(sigmaPrime, c, sigma);
        return sigmaPrime;
    }

    // Call Expression
    Value M(Call c, State sigma, String name) {
        Function f = fs.findFunction(name);
        State sigmaPrime = new State(sigma);
        sigmaPrime = addFrame(sigmaPrime, c, f);
        sigmaPrime = M(f.body, sigmaPrime);
        Value v = sigmaPrime.get(new Variable(name));
        sigmaPrime = removeFrame(sigmaPrime, c, sigma);
        return v;
    }

    State M(Return r, State sigma) {
        return sigma.onion(new State(r.target, M(r.result, sigma)));
    }

    Value M(Expression e, State sigma) {
        if (e instanceof Value)
            return (Value) e;
        if (e instanceof Variable)
            return sigma.get((Variable) e);
        if (e instanceof Binary b) {
            return applyBinary(b.op, M(b.term1, sigma), M(b.term2, sigma));
        }
        if (e instanceof Unary u) {
            return applyUnary(u.op, M(u.term, sigma));
        }
        if (e instanceof Call c) {
            return M(c, sigma, c.name);
        }
        throw new IllegalArgumentException("should never reach here");
    }

    Value applyBinary(Operator op, Value v1, Value v2) {
        StaticTypeCheck.check(v1.isUndef() || v2.isUndef(), "reference to undef value");

        return switch (op.val) {
            case Operator.AND -> new BoolValue(v1.boolValue() && v2.boolValue());
            case Operator.OR -> new BoolValue(v1.boolValue() || v2.boolValue());
            case Operator.INT_LT -> new BoolValue(v1.intValue() < v2.intValue());
            case Operator.INT_LE -> new BoolValue(v1.intValue() <= v2.intValue());
            case Operator.INT_EQ -> new BoolValue(v1.intValue() == v2.intValue());
            case Operator.INT_NE -> new BoolValue(v1.intValue() != v2.intValue());
            case Operator.INT_GT -> new BoolValue(v1.intValue() > v2.intValue());
            case Operator.INT_GE -> new BoolValue(v1.intValue() >= v2.intValue());
            case Operator.FLOAT_LT -> new BoolValue(v1.floatValue() < v2.floatValue());
            case Operator.FLOAT_LE -> new BoolValue(v1.floatValue() <= v2.floatValue());
            case Operator.FLOAT_EQ -> new BoolValue(v1.floatValue() == v2.floatValue());
            case Operator.FLOAT_NE -> new BoolValue(v1.floatValue() != v2.floatValue());
            case Operator.FLOAT_GT -> new BoolValue(v1.floatValue() > v2.floatValue());
            case Operator.FLOAT_GE -> new BoolValue(v1.floatValue() >= v2.floatValue());
            case Operator.CHAR_LT -> new BoolValue(v1.charValue() < v2.charValue());
            case Operator.CHAR_LE -> new BoolValue(v1.charValue() <= v2.charValue());
            case Operator.CHAR_EQ -> new BoolValue(v1.charValue() == v2.charValue());
            case Operator.CHAR_NE -> new BoolValue(v1.charValue() != v2.charValue());
            case Operator.CHAR_GT -> new BoolValue(v1.charValue() > v2.charValue());
            case Operator.CHAR_GE -> new BoolValue(v1.charValue() >= v2.charValue());
            case Operator.BOOL_LT -> new BoolValue(v1.boolValue() && !v2.boolValue());
            case Operator.BOOL_LE -> new BoolValue(v1.boolValue() || !v2.boolValue());
            case Operator.BOOL_EQ -> new BoolValue(v1.boolValue() == v2.boolValue());
            case Operator.BOOL_NE -> new BoolValue(v1.boolValue() != v2.boolValue());
            case Operator.BOOL_GT -> new BoolValue(!v1.boolValue() && v2.boolValue());
            case Operator.BOOL_GE -> new BoolValue(!v1.boolValue() || v2.boolValue());
            case Operator.INT_PLUS -> new IntValue(v1.intValue() + v2.intValue());
            case Operator.INT_MINUS -> new IntValue(v1.intValue() - v2.intValue());
            case Operator.INT_TIMES -> new IntValue(v1.intValue() * v2.intValue());
            case Operator.INT_DIV -> new IntValue(v1.intValue() / v2.intValue());
            case Operator.FLOAT_PLUS -> new FloatValue(v1.floatValue() + v2.floatValue());
            case Operator.FLOAT_MINUS -> new FloatValue(v1.floatValue() - v2.floatValue());
            case Operator.FLOAT_TIMES -> new FloatValue(v1.floatValue() * v2.floatValue());
            case Operator.FLOAT_DIV -> new FloatValue(v1.floatValue() / v2.floatValue());
            default -> throw new IllegalArgumentException("Unknown binary operator " + op.val);
        };
    }

    Value applyUnary(Operator op, Value v) {
        StaticTypeCheck.check(v.isUndef(), "reference to undef value");
        if (op.val.equals(Operator.NOT))
            return new BoolValue(!v.boolValue());
        if (op.val.equals(Operator.INT_NEG))
            return new IntValue(-v.intValue());
        if (op.val.equals(Operator.FLOAT_NEG))
            return new FloatValue(-v.floatValue());
        if (op.val.equals(Operator.I2F))
            return new FloatValue((float) (v.intValue()));
        if (op.val.equals(Operator.F2I))
            return new IntValue((int) (v.floatValue()));
        if (op.val.equals(Operator.C2I))
            return new IntValue((int) (v.charValue()));
        if (op.val.equals(Operator.I2C))
            return new CharValue((char) (v.intValue()));
        throw new IllegalArgumentException("Unknown unary operator " + op.val);
    }
}
