package bear.session;

import bear.core.Var;

/**
* @author Andrey Chaschev chaschev@gmail.com
*/
public class Address {
    @Var("sessionHostname")
    String name;

    public Address() {
    }

    public Address(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}