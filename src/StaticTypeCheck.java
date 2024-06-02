// StaticTypeCheck.java

// Static type checking for Clite is defined by the functions
// V and the auxiliary functions typing and typeOf.  These
// functions use the classes in the Abstract Syntax of Clite.


import java.util.Objects;

public class StaticTypeCheck {

    public static void main(String[] args) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();
        System.out.println("\n\nBegin type checking...");
        System.out.print("Type map: ");
        TypeMap map = typing(prog.globals, prog.functions);
        map.display();
        V(prog);
    } //main

    static TypeMap typing(Declarations ds) {
        TypeMap map = new TypeMap();
        for (Declaration d : ds)
            map.put(d.v, d.t);
        return map;
    }

    static TypeMap typing(Declarations ds, Functions fs) {
        TypeMap map = new TypeMap();
        for (Declaration d : ds)
            map.put(d.v, d.t);
        for (Function f : fs) // 함수는 반환 타입과 매개변수들을 타입으로 검사
            map.put(new Variable(f.id), new ProtoType(f.t, f.params));
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test) return;
        System.err.println(msg);
        System.exit(1);
    }

    static void V(Declarations ds) {
        for (int i = 0; i < ds.size() - 1; i++) {
            for (int j = i + 1; j < ds.size(); j++) {
                Declaration di = ds.get(i);
                Declaration dj = ds.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
        }
    }

    static void V(Declarations ds, Functions fs) {
        for (int i = 0; i < ds.size() - 1; i++) {
            Declaration di = ds.get(i);
            for (int j = i + 1; j < ds.size(); j++) {
                Declaration dj = ds.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
            for (Function fj : fs)
                check(!di.v.toString().equals(fj.id), "duplicate declaration: " + fj.id);
        }
    }

    static void V(Declarations ds1, Declarations ds2) {
        for (Declaration di : ds1)
            for (Declaration dj : ds2)
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
    }


    static void V(Program p) {
        V(p.globals, p.functions);
        boolean foundMain = false;
        TypeMap tmg = typing(p.globals, p.functions); // 글로벌 타입맵
        System.out.print("Globals: ");
        p.globals.display(1);
        System.out.println();
        for (Function f : p.functions) {
            if (f.id.equals("main")) {
                if (foundMain)
                    check(false, "Duplicate main function");
                else
                    foundMain = true;
            }
            V(f.params, f.locals);
            TypeMap tmf = typing(f.params).onion(typing(f.locals));
            tmf = tmg.onion(tmf);
            System.out.print("Function " + f.id + " = ");
            tmf.display();
            V(f.body, tmf);
        }
    }

    static Type typeOf(Expression e, TypeMap tm) {
        if (e instanceof Value) return ((Value) e).type;
        if (e instanceof Variable v) {
            check(tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        if (e instanceof Binary b) {
            if (b.op.ArithmeticOp())
                if (typeOf(b.term1, tm).equals(Type.FLOAT))
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp() || b.op.BooleanOp())
                return (Type.BOOL);
        }
        if (e instanceof Unary u) {
            if (u.op.NotOp()) return (Type.BOOL);
            else if (u.op.NegateOp()) return typeOf(u.term, tm);
            else if (u.op.intOp()) return (Type.INT);
            else if (u.op.floatOp()) return (Type.FLOAT);
            else if (u.op.charOp()) return (Type.CHAR);
        }
        if (e instanceof Call c) {
            check(tm.containsKey(new Variable(c.name)), "undefined name: " + c.name);
            return tm.get(new Variable(c.name));
        }
        throw new IllegalArgumentException("should never reach here");
    }

    static Type typeOf(Function f, TypeMap tm) {
        Variable v = new Variable(f.id);
        check(tm.containsKey(v), "undefined variable: " + v);
        return tm.get(v); // 프로토타입 반환
    }

    static Type typeOf(Expression e, Functions fs, TypeMap tm) {
        if (e instanceof Value) {
            return ((Value) e).type;
        }

        if (e instanceof Variable v) {
            check(tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }

        if (e instanceof Call c) {
            Function func = fs.findFunction(c.name);
            tm.put(new Variable(func.id), func.t);
            return func.t;
        }

        if (e instanceof Binary b) {
            if (b.op.ArithmeticOp())
                if (typeOf(b.term1, fs, tm) == Type.FLOAT)
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp() || b.op.BooleanOp())
                return (Type.BOOL);
        }

        if (e instanceof Unary u) {
            if (u.op.NotOp())
                return (Type.BOOL);
            else if (u.op.NegateOp())
                return typeOf(u.term, fs, tm);
            else if (u.op.intOp())
                return (Type.INT);
            else if (u.op.floatOp())
                return (Type.FLOAT);
            else if (u.op.charOp())
                return (Type.CHAR);
        }

        throw new IllegalArgumentException("should never reach here");
    }


    static void V(Expression e, TypeMap tm) {
        if (e instanceof Value)
            return;
        if (e instanceof Variable v) {
            check(tm.containsKey(v), "undeclared variable: " + v);
            return;
        }
        if (e instanceof Binary b) {
            Type typ1 = typeOf(b.term1, tm);
            Type typ2 = typeOf(b.term2, tm);
            V(b.term1, tm);
            V(b.term2, tm);
            if (b.op.ArithmeticOp())
                check(typ1.equals(typ2) && (typ1.equals(Type.INT) || typ1.equals(Type.FLOAT)), "arithmetic type error for " + b.op);
            else if (b.op.RelationalOp())
                check(typ1.equals(typ2), "relational type error for " + b.op);
            else if (b.op.BooleanOp())
                check(typ1.equals(Type.BOOL) && typ2.equals(Type.BOOL), b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        if (e instanceof Unary u) {
            Type t = typeOf(u.term, tm);
            V(u.term, tm); // term이 타당한지 검사

            // op에 대해 검사
            if (u.op.NotOp()) // '!' 일 때 bool 식 필요
                check(t.equals(Type.BOOL), u.op + ": non-bool operand");
            else if (u.op.NegateOp()) // '-' 일 때 int 또는 float 식 필요
                check(t.equals(Type.INT) || t.equals(Type.FLOAT), "type error for " + u.op);
            else if (u.op.floatOp() || u.op.charOp()) // float 또는 char 캐스팅할 때 int 식 필요
                check(t.equals(Type.INT), "type error for " + u.op);
            else if (u.op.intOp()) // int 캐스팅할 때 float 또는 char 식 필요
                check(t.equals(Type.FLOAT) || t.equals(Type.CHAR), "type error for " + u.op);
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        if (e instanceof Call) {
            Variable v = new Variable(((Call) e).name);
            Expressions es = ((Call) e).args;
            check(tm.containsKey(v), "undeclared function: " + v);
            ProtoType p = (ProtoType) tm.get(v);
            checkProtoType(p, tm, typeOf(e, tm), es);
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    private static void checkProtoType(ProtoType p, TypeMap tm, Type t, Expressions es) {
        TypeMap tmp = typing(p.params); // 파라미터 위한 임시 타입맵
        check(es.size() == p.params.size(), "match numbers of arguments and pairs");
        check(p.toString().equals(t.toString()), "calls can only be to void functions");
        for (int i = 0; i < es.size(); i++) {
            // 인수와 파라미터 타입 매칭
            Expression e1 = es.get(i);
            Expression e2 = p.params.get(i).v;
            check(typeOf(e1, tm).equals(typeOf(e2, tmp)), "argument type does not match parameter");
        }
    }

    static void V(Statement s, TypeMap tm) {
        if (s == null)
            throw new IllegalArgumentException("AST error: null statement");
        if (s instanceof Skip) return;
        if (s instanceof Assignment a) {
            check(tm.containsKey(a.target), " undefined target in assignment: " + a.target);
            V(a.source, tm);
            Type ttype = tm.get(a.target);
            Type srctype = typeOf(a.source, tm);
            if (!Objects.equals(ttype.toString(), srctype.toString())) {
                if (ttype == Type.FLOAT)
                    check(Objects.equals(srctype.toString(), Type.INT.toString()), "mixed mode assignment to float " + a.target);
                else if (ttype == Type.INT)
                    check(Objects.equals(srctype.toString(), Type.CHAR.toString()), "mixed mode assignment to int " + a.target);
                else
                    check(false, "mixed mode assignment to " + a.target);
            }
            return;
        }
        // test가 타당한 계산식이고 bool 타입이며, thenbranch와  elsebranch도 타당해야 한다.
        if (s instanceof Conditional c) {
            Type t = typeOf(c.test, tm);
            check(t.equals(Type.BOOL), "conditional test is not boolean");
            V(c.thenbranch, tm);
            V(c.elsebranch, tm);
            return;
        }
        // test가 타당한 계산식이고 bool 타입이며, 몸체 body도 타당해야 한다.
        if (s instanceof Loop l) {
            Type t = typeOf(l.test, tm);
            check(t.equals(Type.BOOL), "loop test is not boolean");
            V(l.body, tm);
            return;
        }
        // Block 내의 모든 문장이 타당해야 한다.
        if (s instanceof Block b) {
            for (Statement stmt : b.members) V(stmt, tm);
            return;
        }
        if (s instanceof Call) {
            Variable v = new Variable(((Call) s).name);
            Expressions es = ((Call) s).args;
            check(tm.containsKey(v), "undefined function: " + v);
            ProtoType p = (ProtoType) tm.get(v);
            checkProtoType(p, tm, Type.VOID, es);
            return;
        }
        if (s instanceof Return) {
            Variable fid = ((Return) s).target;
            check(tm.containsKey(fid), "undefined function: " + fid);
            V(((Return) s).result, tm);
            check(tm.get(fid).toString().equals(typeOf(((Return) s).result, tm).toString()), "incorrect return type");
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

} // class StaticTypeCheck

