// StaticTypeCheck.java

// Static type checking for Clite is defined by the functions
// V and the auxiliary functions typing and typeOf.  These
// functions use the classes in the Abstract Syntax of Clite.


public class StaticTypeCheck {

    public static TypeMap typing(Declarations ds) {
        TypeMap map = new TypeMap();
        for (Declaration d : ds)
            map.put(d.v, d.t);
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test) return;
        System.err.println(msg);
        System.exit(1);
    }

    public static void V(Declarations d) {
        for (int i = 0; i < d.size() - 1; i++)
            for (int j = i + 1; j < d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
    }

    public static void V(Program p) {
        V(p.decpart);
        V(p.body, typing(p.decpart));
    }

    public static Type typeOf(Expression e, TypeMap tm) {
        if (e instanceof Value) return ((Value) e).type;
        if (e instanceof Variable) {
            Variable v = (Variable) e;
            check(tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            if (b.op.ArithmeticOp())
                if (typeOf(b.term1, tm) == Type.FLOAT)
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp() || b.op.BooleanOp())
                return (Type.BOOL);
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            if (u.op.NotOp()) return (Type.BOOL);
            else if (u.op.NegateOp()) return typeOf(u.term, tm);
            else if (u.op.intOp()) return (Type.INT);
            else if (u.op.floatOp()) return (Type.FLOAT);
            else if (u.op.charOp()) return (Type.CHAR);
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V(Expression e, TypeMap tm) {
        if (e instanceof Value)
            return;
        if (e instanceof Variable) {
            Variable v = (Variable) e;
            check(tm.containsKey(v), "undeclared variable: " + v);
            return;
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm);
            Type typ2 = typeOf(b.term2, tm);
            V(b.term1, tm);
            V(b.term2, tm);
            if (b.op.ArithmeticOp())
                check(typ1 == typ2 && (typ1 == Type.INT || typ1 == Type.FLOAT), "type error for " + b.op);
            else if (b.op.RelationalOp())
                check(typ1 == typ2, "type error for " + b.op);
            else if (b.op.BooleanOp())
                check(typ1 == Type.BOOL && typ2 == Type.BOOL, b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Type t = typeOf(u.term, tm);
            V(u.term, tm); // term이 타당한지 검사

            // op에 대해 검사
            if (u.op.NotOp()) // '!' 일 때 bool 식 필요
                check(t == Type.BOOL, u.op + ": non-bool operand");
            else if (u.op.NegateOp()) // '-' 일 때 int 또는 float 식 필요
                check(t == Type.INT || t == Type.FLOAT, "type error for " + u.op);
            else if (u.op.floatOp() || u.op.charOp()) // float 또는 char 캐스팅할 때 int 식 필요
                check(t == Type.INT, "type error for " + u.op);
            else if (u.op.intOp()) // int 캐스팅할 때 float 또는 char 식 필요
                check(t == Type.FLOAT || t == Type.CHAR, "type error for " + u.op);
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V(Statement s, TypeMap tm) {
        if (s == null)
            throw new IllegalArgumentException("AST error: null statement");
        if (s instanceof Skip) return;
        if (s instanceof Assignment) {
            Assignment a = (Assignment) s;
            check(tm.containsKey(a.target), " undefined target in assignment: " + a.target);
            V(a.source, tm);
            Type ttype = (Type) tm.get(a.target);
            Type srctype = typeOf(a.source, tm);
            if (ttype != srctype) {
                if (ttype == Type.FLOAT)
                    check(srctype == Type.INT, "mixed mode assignment to " + a.target);
                else if (ttype == Type.INT)
                    check(srctype == Type.CHAR, "mixed mode assignment to " + a.target);
                else
                    check(false, "mixed mode assignment to " + a.target);
            }
            return;
        }
        // test가 타당한 계산식이고 bool 타입이며, thenbranch와  elsebranch도 타당해야 한다.
        if (s instanceof Conditional) {
            Conditional c = (Conditional) s;
            Type t = typeOf(c.test, tm);
            check(t == Type.BOOL, "conditional test is not boolean");
            V(c.thenbranch, tm);
            V(c.elsebranch, tm);
            return;
        }
        // test가 타당한 계산식이고 bool 타입이며, 몸체 body도 타당해야 한다.
        if (s instanceof Loop) {
            Loop l = (Loop) s;
            Type t = typeOf(l.test, tm);
            check(t == Type.BOOL, "loop test is not boolean");
            V(l.body, tm);
            return;
        }
        // Block 내의 모든 문장이 타당해야 한다.
        if (s instanceof Block) {
            Block b = (Block) s;
            for (Statement stmt : b.members) V(stmt, tm);
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();
        System.out.println("\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = typing(prog.decpart);
        map.display();
        V(prog);
    } //main

} // class StaticTypeCheck

