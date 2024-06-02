// Abstract syntax for the language C++Lite,
// exactly as it appears in Appendix B.

import java.util.ArrayList;

class Indenter {
    public int level;

    public Indenter(int nextLevel) {
        level = nextLevel;
    }

    public void display(String message) {
        String tab = "";
        System.out.println();
        for (int i = 0; i < level; i++)
            tab += "  ";
        System.out.print(tab + message);
    }
}

class Program {
    // Program = Declarations globals; Functions functions
    Declarations globals; // 전역변수 선언
    Functions functions; // 함수 선언

    public Program() {
    }

    public Program(Declarations g, Functions f) {
        globals = g;
        functions = f;
    }

    public void display() {
        int level = 0;
        Indenter i = new Indenter(level);
        i.display("Program (abstract syntax): ");
        globals.display(level + 1);
        functions.display(level + 1);
    }
}

class Declarations extends ArrayList<Declaration> {
    // Declarations = Declaration*
    // (a list of declarations d1, d2, ..., dn)
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Declarations = {");
        String sep = "";
        for (Declaration d : this) {
            System.out.print(sep);
            d.display();
            sep = ", ";
        }
        System.out.print("}");
    }
}

class Declaration {
    // Declaration = Variable v; Type t
    Variable v;
    Type t;

    Declaration(Variable var, Type type) {
        v = var;
        t = type;
    } // declaration */

    Declaration(String var, Type type) {
        v = new Variable(var);
        t = type;
    }

    public void display() {
        System.out.print("<" + v.toString() + ", " + t.toString() + ">");
    }
}

class Functions extends ArrayList<Function> {
    // Functions = Function*

    public Function findFunction(String name) {
        for (Function f : this)
            if (f.id.equals(name))
                return f;
        throw new IllegalArgumentException("Function not found: " + name);
    }

    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Functions = {");
        for (Function f : this) {
            f.display(level + 1);
        }
        i.display("}");
    }
}

class Function {
    // Function = Type t; String id; Declarations params, locals; Block body
    Type t;
    String id;
    Declarations params;
    Declarations locals;
    Block body;

    Function(String i, Type type) {
        t = type;
        id = i;
    }

    public Function(Type t, String id, Declarations params, Declarations locals, Block body) {
        this.t = t;
        this.id = id;
        this.params = params;
        this.locals = locals;
        this.body = body;
    }

    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display(t.toString() + " " + id);
        i.display("  Parameters:");
        params.display(level + 2);
        i.display("  Local Variables:");
        locals.display(level + 2);
        i.display("  Body:");
        body.display(level + 2);
    }
}

class Type {
    // Type = int | bool | char | float | void
    final static Type INT = new Type("int");
    final static Type BOOL = new Type("bool");
    final static Type CHAR = new Type("char");
    final static Type FLOAT = new Type("float");
    final static Type VOID = new Type("void");
    final static Type UNDEFINED = new Type("undef");
    final static Type UNUSED = new Type("unused");

    private final String id;

    Type(String t) {
        id = t;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return id.equals(((Type) obj).id);
    }
}

class ProtoType extends Type {
    // 함수의 이름, 반환형, 파라미터만 정의해 몸체 없이 함수 식별 가능
    Declarations params;

    ProtoType(Type returnType, Declarations p) {
        super(returnType.toString());
        params = p;
    }

    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Prototype:");
        i.display("  Return Type:" + super.toString());
        i.display("  Parameters:");
        params.display(level + 2);
    }
}

abstract class Statement {
    // Statement = Skip | Block | Assignment | Conditional | Loop | Call | Return
    public void display(int level) {
    }
}

class Skip extends Statement {
}

class Block extends Statement {
    // Block = Statement*
    //         (a Vector of members)
    public ArrayList<Statement> members = new ArrayList<Statement>();

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Block:");
        for (Statement s : members)
            s.display(level + 1);
    }
}

class Assignment extends Statement {
    // Assignment = Variable target; Expression source
    Variable target;
    Expression source;

    Assignment(Variable t, Expression e) {
        target = t;
        source = e;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Assignment:");
        target.display(level + 1);
        source.display(level + 1);
    }
}

class Conditional extends Statement {
    // Conditional = Expression test; Statement thenbranch, elsebranch
    Expression test;
    Statement thenbranch, elsebranch;
    // elsebranch == null means "if... then"

    Conditional(Expression t, Statement tp) {
        test = t;
        thenbranch = tp;
        elsebranch = new Skip();
    }

    Conditional(Expression t, Statement tp, Statement ep) {
        test = t;
        thenbranch = tp;
        elsebranch = ep;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Conditional:");
        test.display(level + 1);
        thenbranch.display(level + 1);
        if (elsebranch != null) elsebranch.display(level + 1);
    }
}

class Loop extends Statement {
    // Loop = Expression test; Statement body
    Expression test;
    Statement body;

    Loop(Expression t, Statement b) {
        test = t;
        body = b;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Loop:");
        test.display(level + 1);
        body.display(level + 1);
    }
}

class Return extends Statement {
    Variable target;
    Expression result;

    Return(Variable t, Expression r) {
        target = t;
        result = r;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Return:");
        target.display(level + 1);
        result.display(level + 1);
    }
}

abstract class Expression extends Statement {
    // Expression = Variable | Value | Binary | Unary | Call
    @Override
    public void display(int level) {
    }
}

class Variable extends Expression {
    // Variable = String id
    private final String id;

    Variable(String s) {
        id = s;
    }

    Variable(Variable v) {
        this.id = v.id;
    }

    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return id.equals(((Variable) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Variable: " + id);
    }

}

abstract class Value extends Expression implements Cloneable {
    // Value = IntValue | BoolValue | CharValue | FloatValue | Undefined | Unused
    protected Type type;
    protected boolean undef = true;

    static Value mkValue(Type type) {
        if (type == Type.INT) return new IntValue();
        if (type == Type.BOOL) return new BoolValue();
        if (type == Type.CHAR) return new CharValue();
        if (type == Type.FLOAT) return new FloatValue();
        if (type == Type.UNDEFINED) return new UndefinedValue();
        if (type == Type.UNUSED) return new UnusedValue();
        throw new IllegalArgumentException("Illegal type in mkValue");
    }

    int intValue() {
        assert false : "should never reach here";
        return 0;
    }

    boolean boolValue() {
        assert false : "should never reach here";
        return false;
    }

    char charValue() {
        assert false : "should never reach here";
        return ' ';
    }

    float floatValue() {
        assert false : "should never reach here";
        return 0.0f;
    }

    boolean isUndef() {
        return !undef;
    }

    boolean isUnused() {
        return false;
    }

    Type type() {
        return type;
    }

    @Override
    public Value clone() {
        try {
            return (Value) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

class IntValue extends Value {
    private int value = 0;

    IntValue() {
        type = Type.INT;
    }

    IntValue(int v) {
        this();
        value = v;
        undef = false;
    }

    @Override
    int intValue() {
        assert !undef : "reference to undefined int value";
        return value;
    }

    @Override
    public String toString() {
        if (undef) return "undef";
        return "" + value;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("IntValue: " + value);
    }
}

class BoolValue extends Value {
    private boolean value = false;

    BoolValue() {
        type = Type.BOOL;
    }

    BoolValue(boolean v) {
        this();
        value = v;
        undef = false;
    }

    @Override
    boolean boolValue() {
        assert !undef : "reference to undefined bool value";
        return value;
    }

    @Override
    int intValue() {
        assert !undef : "reference to undefined bool value";
        return value ? 1 : 0;
    }

    @Override
    public String toString() {
        if (undef) return "undef";
        return "" + value;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("BoolValue: " + value);
    }
}

class CharValue extends Value {
    private char value = ' ';

    CharValue() {
        type = Type.CHAR;
    }

    CharValue(char v) {
        this();
        value = v;
        undef = false;
    }

    @Override
    char charValue() {
        assert !undef : "reference to undefined char value";
        return value;
    }

    @Override
    public String toString() {
        if (undef) return "undef";
        return "" + value;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("CharValue: " + value);
    }
}

class FloatValue extends Value {
    private float value = 0;

    FloatValue() {
        type = Type.FLOAT;
    }

    FloatValue(float v) {
        this();
        value = v;
        undef = false;
    }

    @Override
    float floatValue() {
        assert !undef : "reference to undefined float value";
        return value;
    }

    @Override
    public String toString() {
        if (undef) return "undef";
        return "" + value;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("FloatValue: " + value);
    }
}

class UndefinedValue extends Value {
    UndefinedValue() {
        type = Type.UNDEFINED;
        undef = true;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Undefined");
    }
}

class UnusedValue extends Value {
    UnusedValue() {
        type = Type.UNUSED;
        undef = true;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Unused");
    }
}


class Binary extends Expression {
    // Binary = Operator op; Expression term1, term2
    Operator op;
    Expression term1, term2;

    Binary(Operator o, Expression l, Expression r) {
        op = o;
        term1 = l;
        term2 = r;
    } // binary

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Binary:");
        i.display("  Operator: " + op.toString());
        term1.display(level + 1);
        term2.display(level + 1);
    }
}

class Unary extends Expression {
    // Unary = Operator op; Expression term
    Operator op;
    Expression term;

    Unary(Operator o, Expression e) {
        op = o;
        term = e;
    } // unary

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Unary:");
        i.display("  Operator: " + op.toString());
        term.display(level + 1);
    }
}

class Call extends Expression {
    String name;
    Expressions args;

    Call(String n, Expressions a) {
        name = n;
        args = a;
    }

    @Override
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Call Statement:");
        i.display("  " + name);
        args.display(level + 1);
    }
}

class Expressions extends ArrayList<Expression> {
    public void display(int level) {
        Indenter i = new Indenter(level);
        i.display("Expressions:");
        i.display("  Expressions = {");
        for (Expression e : this) {
            e.display(level + 2);
        }
        i.display("  }");
    }
}

class Operator {
    // Operator = BooleanOp | RelationalOp | ArithmeticOp | UnaryOp
    // BooleanOp = && | ||
    final static String AND = "&&";
    final static String OR = "||";
    // RelationalOp = < | <= | == | != | >= | >
    final static String LT = "<";
    final static String LE = "<=";
    final static String EQ = "==";
    final static String NE = "!=";
    final static String GT = ">";
    final static String GE = ">=";
    // ArithmeticOp = + | - | * | /
    final static String PLUS = "+";
    final static String MINUS = "-";
    final static String TIMES = "*";
    final static String DIV = "/";
    // UnaryOp = !    
    final static String NOT = "!";
    final static String NEG = "-";
    // CastOp = int | float | char
    final static String INT = "int";
    final static String FLOAT = "float";
    final static String CHAR = "char";
    // Typed Operators
    // RelationalOp = < | <= | == | != | >= | >
    final static String INT_LT = "INT<";
    final static String INT_LE = "INT<=";
    final static String INT_EQ = "INT==";
    final static String INT_NE = "INT!=";
    final static String INT_GT = "INT>";
    final static String INT_GE = "INT>=";
    // ArithmeticOp = + | - | * | /
    final static String INT_PLUS = "INT+";
    final static String INT_MINUS = "INT-";
    final static String INT_TIMES = "INT*";
    final static String INT_DIV = "INT/";
    // UnaryOp = !    
    final static String INT_NEG = "-";
    // RelationalOp = < | <= | == | != | >= | >
    final static String FLOAT_LT = "FLOAT<";
    final static String FLOAT_LE = "FLOAT<=";
    final static String FLOAT_EQ = "FLOAT==";
    final static String FLOAT_NE = "FLOAT!=";
    final static String FLOAT_GT = "FLOAT>";
    final static String FLOAT_GE = "FLOAT>=";
    // ArithmeticOp = + | - | * | /
    final static String FLOAT_PLUS = "FLOAT+";
    final static String FLOAT_MINUS = "FLOAT-";
    final static String FLOAT_TIMES = "FLOAT*";
    final static String FLOAT_DIV = "FLOAT/";
    // UnaryOp = !    
    final static String FLOAT_NEG = "-";
    // RelationalOp = < | <= | == | != | >= | >
    final static String CHAR_LT = "CHAR<";
    final static String CHAR_LE = "CHAR<=";
    final static String CHAR_EQ = "CHAR==";
    final static String CHAR_NE = "CHAR!=";
    final static String CHAR_GT = "CHAR>";
    final static String CHAR_GE = "CHAR>=";
    // RelationalOp = < | <= | == | != | >= | >
    final static String BOOL_LT = "BOOL<";
    final static String BOOL_LE = "BOOL<=";
    final static String BOOL_EQ = "BOOL==";
    final static String BOOL_NE = "BOOL!=";
    final static String BOOL_GT = "BOOL>";
    final static String BOOL_GE = "BOOL>=";
    // Type specific cast
    final static String I2F = "I2F";
    final static String F2I = "F2I";
    final static String C2I = "C2I";
    final static String I2C = "I2C";
    final static String[][] intMap = {
            {PLUS, INT_PLUS}, {MINUS, INT_MINUS},
            {TIMES, INT_TIMES}, {DIV, INT_DIV},
            {EQ, INT_EQ}, {NE, INT_NE}, {LT, INT_LT},
            {LE, INT_LE}, {GT, INT_GT}, {GE, INT_GE},
            {NEG, INT_NEG}, {FLOAT, I2F}, {CHAR, I2C}
    };
    final static String[][] floatMap = {
            {PLUS, FLOAT_PLUS}, {MINUS, FLOAT_MINUS},
            {TIMES, FLOAT_TIMES}, {DIV, FLOAT_DIV},
            {EQ, FLOAT_EQ}, {NE, FLOAT_NE}, {LT, FLOAT_LT},
            {LE, FLOAT_LE}, {GT, FLOAT_GT}, {GE, FLOAT_GE},
            {NEG, FLOAT_NEG}, {INT, F2I}
    };
    final static String[][] charMap = {
            {EQ, CHAR_EQ}, {NE, CHAR_NE}, {LT, CHAR_LT},
            {LE, CHAR_LE}, {GT, CHAR_GT}, {GE, CHAR_GE},
            {INT, C2I}
    };
    final static String[][] boolMap = {
            {EQ, BOOL_EQ}, {NE, BOOL_NE}, {LT, BOOL_LT},
            {LE, BOOL_LE}, {GT, BOOL_GT}, {GE, BOOL_GE},
            {AND, AND}, {OR, OR}
    };
    String val;

    Operator(String s) {
        val = s;
    }

    static private Operator map(String[][] tmap, String op) {
        for (String[] strings : tmap)
            if (strings[0].equals(op))
                return new Operator(strings[1]);
        assert false : "should never reach here";
        return null;
    }

    static public Operator intMap(String op) {
        return map(intMap, op);
    }

    static public Operator floatMap(String op) {
        return map(floatMap, op);
    }

    static public Operator charMap(String op) {
        return map(charMap, op);
    }

    static public Operator boolMap(String op) {
        return map(boolMap, op);
    }

    @Override
    public String toString() {
        return val;
    }

    @Override
    public boolean equals(Object obj) {
        return val.equals(obj);
    }

    boolean BooleanOp() {
        return val.equals(AND) || val.equals(OR);
    }

    boolean RelationalOp() {
        return val.equals(LT) || val.equals(LE) || val.equals(EQ)
                || val.equals(NE) || val.equals(GT) || val.equals(GE);
    }

    boolean ArithmeticOp() {
        return val.equals(PLUS) || val.equals(MINUS)
                || val.equals(TIMES) || val.equals(DIV);
    }

    boolean NotOp() {
        return val.equals(NOT);
    }

    boolean NegateOp() {
        return val.equals(NEG);
    }

    boolean intOp() {
        return val.equals(INT);
    }

    boolean floatOp() {
        return val.equals(FLOAT);
    }

    boolean charOp() {
        return val.equals(CHAR);
    }

}
