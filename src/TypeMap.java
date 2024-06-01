import java.util.HashMap;

public class TypeMap extends HashMap<Variable, Type> {

    public TypeMap onion(TypeMap tm) {
        TypeMap res = new TypeMap();
        res.putAll(this);
        res.putAll(tm);
        return res;
    }

    public void display() {
        System.out.println(this.entrySet());
    }
}
