public class TypeTransformer {

    public static void main(String[] args) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();
        System.out.println("\n\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals, prog.functions);
        map.display();
        StaticTypeCheck.V(prog);
        Program out = T(prog);
        System.out.println("\n\nOutput AST");
        out.display();
    } //main

    static Program T(Program p) {
        TypeMap globalMap = StaticTypeCheck.typing(p.globals);
        Functions fs = p.functions;

        for (Function func : fs) {
            TypeMap functionMap = new TypeMap();
            for (Function f : fs)
                functionMap.put(new Variable(f.id), f.t);
            functionMap.putAll(globalMap);
            functionMap.putAll(StaticTypeCheck.typing(func.locals));
            if (func.params != null)
                if (!func.params.isEmpty())
                    functionMap.putAll(StaticTypeCheck.typing(func.params));

            Block body = (Block) T(func.body, fs, functionMap);
            fs.set(fs.indexOf(func), new Function(func.t, func.id, func.params, func.locals, body));
        }

        return new Program(p.globals, fs);
    }

    static Expression T(Expression e, Functions f, TypeMap tm) {
        if (e instanceof Value)
            return e;
        if (e instanceof Variable)
            return e;
        if (e instanceof Call c) {
            Expressions es = new Expressions();
            for (Expression ce : c.args)
                es.add(T(ce, f, tm));
            return new Call(c.name, es);
        }
        if (e instanceof Binary b) {
            Type typ1 = StaticTypeCheck.typeOf(b.term1, f, tm);
            Type typ2 = StaticTypeCheck.typeOf(b.term2, f, tm);
            Expression t1 = T(b.term1, f, tm);
            Expression t2 = T(b.term2, f, tm);
            if (typ1.equals(Type.FLOAT) || typ2.equals(Type.FLOAT)) {
                if (typ1.equals(Type.INT))
                    return new Binary(Operator.floatMap(b.op.val), new Unary(new Operator(Operator.I2F), t1), t2);
                else if (typ2.equals(Type.INT))
                    return new Binary(Operator.floatMap(b.op.val), t1, new Unary(new Operator(Operator.I2F), t2));
                else
                    return new Binary(Operator.floatMap(b.op.val), t1, t2);
            } else if (typ1.equals(Type.INT) || typ2.equals(Type.INT)) {
                if (typ1.equals(Type.CHAR))
                    return new Binary(Operator.intMap(b.op.val), new Unary(new Operator(Operator.C2I), t1), t2);
                else if (typ2.equals(Type.CHAR))
                    return new Binary(Operator.intMap(b.op.val), t1, new Unary(new Operator(Operator.C2I), t2));
                else
                    return new Binary(Operator.intMap(b.op.val), t1, t2);
            } else if (typ1.equals(Type.CHAR) || typ2.equals(Type.CHAR))
                return new Binary(Operator.charMap(b.op.val), t1, t2);
            else if (typ1.equals(Type.BOOL) || typ2.equals(Type.BOOL))
                return new Binary(Operator.boolMap(b.op.val), t1, t2);
            throw new IllegalArgumentException("should never reach here, " + typ1 + " vs " + typ2);
        }
        if (e instanceof Unary u) {
            Type typ1 = StaticTypeCheck.typeOf(u.term, f, tm);
            Expression term = T(u.term, f, tm);
            Operator op = u.op;
            if (u.op.toString().equals(Operator.NOT)) {
                ;
            } else if (Operator.NEG.equals(u.op.toString())) {
                if (typ1.equals(Type.INT))
                    op = Operator.intMap(op.val);
                else if (typ1.equals(Type.FLOAT))
                    op = Operator.floatMap(op.val);
            } else if (Operator.FLOAT.equals(u.op.toString()))
                op = Operator.intMap(op.val);
            else if (Operator.CHAR.equals(u.op.toString()))
                op = Operator.intMap(op.val);
            else if (Operator.INT.equals(u.op.toString())) {
                if (typ1.equals(Type.FLOAT))
                    op = Operator.floatMap(op.val);
                else if (typ1.equals(Type.CHAR))
                    op = Operator.charMap(op.val);
            } else {
                throw new IllegalArgumentException("should never reach here");
            }
            return new Unary(op, term);
        }
        throw new IllegalArgumentException("should never reach here");
    }


    static Expression T(Expression e, TypeMap tm) {
        if (e instanceof Value)
            return e;
        if (e instanceof Variable)
            return e;
        if (e instanceof Binary b) {
            Type typ1 = StaticTypeCheck.typeOf(b.term1, tm);
            Type typ2 = StaticTypeCheck.typeOf(b.term2, tm);
            Expression t1 = T(b.term1, tm);
            Expression t2 = T(b.term2, tm);
            if (typ1.equals(Type.INT))
                return new Binary(Operator.intMap(b.op.val), t1, t2);
            if (typ1.equals(Type.FLOAT))
                return new Binary(Operator.floatMap(b.op.val), t1, t2);
            if (typ1.equals(Type.CHAR))
                return new Binary(Operator.charMap(b.op.val), t1, t2);
            if (typ1.equals(Type.BOOL))
                return new Binary(Operator.boolMap(b.op.val), t1, t2);
            throw new IllegalArgumentException("should never reach here");
        }
        if (e instanceof Unary u) {
            Type t = StaticTypeCheck.typeOf(u.term, tm);
            Expression exp = T(u.term, tm);
            if (t.equals(Type.BOOL))
                return new Unary(Operator.boolMap(u.op.val), exp);
            if (t.equals(Type.FLOAT)) {
                if (u.op.val.equals(Operator.MINUS))
                    return new Unary(new Operator(Operator.FLOAT_NEG), exp);
                return new Unary(Operator.floatMap(u.op.val), exp);
            }
            if (t.equals(Type.INT)) {
                if (u.op.val.equals(Operator.MINUS))
                    return new Unary(new Operator(Operator.INT_NEG), exp);
                return new Unary(Operator.intMap(u.op.val), exp);
            }
            if (t.equals(Type.CHAR))
                return new Unary(Operator.charMap(u.op.val), exp);
        }
        throw new IllegalArgumentException("should never reach here");
    }

    static Statement T(Statement s, Functions f, TypeMap tm) {
        if (s instanceof Skip)
            return s;
        if (s instanceof Assignment a) {
            Variable target = a.target;
            Expression src = T(a.source, f, tm);
            Type ttype = tm.get(a.target);
            Type srctype = StaticTypeCheck.typeOf(a.source, f, tm);
            if (ttype.equals(Type.FLOAT)) {
                if (srctype.equals(Type.INT)) {
                    src = new Unary(new Operator(Operator.I2F), src);
                    srctype = Type.FLOAT;
                }
            } else if (ttype.equals(Type.INT)) {
                if (srctype.equals(Type.CHAR)) {
                    src = new Unary(new Operator(Operator.C2I), src);
                    srctype = Type.INT;
                }
            }
            StaticTypeCheck.check(ttype == srctype, " cannot assign to " + target);
            return new Assignment(target, src);
        }
        if (s instanceof Conditional c) {
            Expression test = T(c.test, f, tm);
            Statement tbr = T(c.thenbranch, f, tm);
            Statement ebr = T(c.elsebranch, f, tm);
            return new Conditional(test, tbr, ebr);
        }
        if (s instanceof Loop l) {
            Expression test = T(l.test, f, tm);
            Statement body = T(l.body, f, tm);
            return new Loop(test, body);
        }
        if (s instanceof Block b) {
            Block out = new Block();
            for (Statement stmt : b.members)
                out.members.add(T(stmt, f, tm));
            return out;
        }
        if (s instanceof Call c) {
            Expressions es = new Expressions();
            for (Expression e : c.args)
                es.add(T(e, f, tm));
            return new Call(c.name, es);
        }
        if (s instanceof Return r) {
            Expression result = T(r.result, f, tm);
            return new Return(r.target, result);
        }
        throw new IllegalArgumentException("should never reach here");
    }

    static Statement T(Statement s, TypeMap tm) {
        if (s instanceof Skip) return s;
        if (s instanceof Assignment a) {
            Variable target = a.target;
            Expression src = T(a.source, tm);
            Type ttype = tm.get(a.target);
            Type srctype = StaticTypeCheck.typeOf(a.source, tm);
            if (ttype.equals(Type.FLOAT)) {
                if (srctype.equals(Type.INT)) {
                    src = new Unary(new Operator(Operator.I2F), src);
                    srctype = Type.FLOAT;
                }
            } else if (ttype.equals(Type.INT)) {
                if (srctype.equals(Type.CHAR)) {
                    src = new Unary(new Operator(Operator.C2I), src);
                    srctype = Type.INT;
                }
            }
            StaticTypeCheck.check(ttype.equals(srctype), " cannot assign to " + target);
            return new Assignment(target, src);
        }
        if (s instanceof Conditional c) {
            Expression test = T(c.test, tm);
            Statement tbr = T(c.thenbranch, tm);
            Statement ebr = T(c.elsebranch, tm);
            return new Conditional(test, tbr, ebr);
        }
        if (s instanceof Loop l) {
            Expression test = T(l.test, tm);
            Statement body = T(l.body, tm);
            return new Loop(test, body);
        }
        if (s instanceof Block b) {
            Block out = new Block();
            for (Statement stmt : b.members)
                out.members.add(T(stmt, tm));
            return out;
        }
        throw new IllegalArgumentException("should never reach here");
    }
} // class TypeTransformer


