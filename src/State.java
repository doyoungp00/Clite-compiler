import java.util.ArrayList;

public class State implements Cloneable {
    Environment gamma;
    Memory mu;
    int a; // 메모리에서 스택의 다음 주소
    int slink; // 정적 링크
    int dlink; // 동적 링크

    State() {
        gamma = new Environment();
        mu = new Memory(16);
        a = slink = dlink = 0;
    }

    State(State s) {
        gamma = new Environment(s.gamma);
        mu = new Memory(s.mu);
        a = s.a;
        slink = s.slink;
        dlink = s.dlink;
    }

    State(Variable key, Value val) {
        this();
        gamma.add(new Pair(key, a));
        mu.set(a, val);
        a++;
    }


    // 메모리에 공간 할당 후 undef로 초기화
    State allocate(Declarations ds) {
        if (a + ds.size() < mu.size()) {
            for (Declaration d : ds) {
                gamma.add(new Pair(d.v, a));
                mu.set(a, Value.mkValue(Type.UNDEFINED));
                a++;
            }
        } else {
            System.out.println("Stack Overflow!");
            System.exit(0);
        }
        return this;
    }

    // 메모리 공간 반환 후 unused로 초기화
    State deallocate(Declarations ds) {
        for (int i = ds.size() - 1; i >= 0; i--) {
            gamma.remove(gamma.size() - 1);
            a--;
            mu.set(a, Value.mkValue(Type.UNUSED));
        }
        return this;
    }

    // former 상태에서 현재 상태로 추가
    State plus(State former) {
        for (int i = former.slink; i < former.gamma.size(); i++) {
            Pair p = former.gamma.get(i);
            gamma.add(p);
        }
        return this;
    }

    // gamma에서 마지막 n개 변수 삭제
    State minus(int n) {
        for (int i = 0; i < n; i++)
            gamma.remove(gamma.size() - 1);
        return this;
    }

    State onion(Variable key, Value val) {
        put(key, val);
        return this;
    }

    State onion(State t) {
        for (int i = 0; i < t.gamma.size(); i++) {
            Variable key = t.gamma.get(i).v;
            Value tvalue = t.get(key);
            if (gamma.contains(key)) {
                int address = getAddress(key);
                mu.set(address, tvalue);
            } else {
                int taddress = t.getAddress(key);
                gamma.add(new Pair(key, taddress));
                mu.set(taddress, tvalue);
            }
        }
        return this;
    }

    void put(Variable key, Value val) {
        if (!contains(key)) {
            gamma.add(new Pair(key, a));
            mu.set(a, val);
            a++;
        } else {
            int address = getAddress(key);
            mu.set(address, val);
        }
    }

    boolean contains(Variable key) {
        for (Pair p : gamma)
            if (p.v.equals(key))
                return true;
        return false;
    }


    Value get(Variable key) {
        int address = getAddress(key);
        return mu.get(address);
    }

    int getAddress(Variable key) {
        for (Pair p : gamma)
            if (p.v.equals(key))
                return p.addr;

        throw new IllegalArgumentException("Undefined variable: " + key);
    }

    public void display() {
        System.out.print("{");
        String sep = "";
        for (Pair p : gamma) {
            System.out.print(sep + p.v + "=" + mu.get(p.addr));
            sep = ", ";
        }
        System.out.println("}");
    }

    @Override
    public State clone() {
        try {
            State cloned = (State) super.clone();
            cloned.gamma = new Environment(this.gamma);
            cloned.mu = new Memory(this.mu);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}

class Pair {
    Variable v;
    int addr;

    public Pair(Variable v, int a) {
        this.v = v;
        this.addr = a;
    }

    public Pair(Pair p) {
        this.v = new Variable(p.v);
        this.addr = p.addr;
    }
}


class Environment extends ArrayList<Pair> {
    public Environment() {
        super();
    }

    public Environment(Environment env) {
        super();
        for (Pair p : env)
            this.add(new Pair(p));
    }
}

class Memory extends ArrayList<Value> {
    int size;

    public Memory(int size) {
        super();
        this.size = size;
        for (int i = 0; i < size; i++) {
            this.add(new UnusedValue());
        }
    }

    public Memory(Memory m) {
        super();
        this.size = m.size;
        for (int i = 0; i < m.size(); i++)
            this.add(m.get(i).clone());
    }
}

