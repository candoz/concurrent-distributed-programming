package distributed_chat.messages;

import java.io.Serializable;

public class LoginRequestFromChatterMsg implements Serializable {

    private final String name;

    public LoginRequestFromChatterMsg(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
