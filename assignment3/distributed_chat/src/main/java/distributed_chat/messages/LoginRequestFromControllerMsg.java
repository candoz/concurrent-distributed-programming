package distributed_chat.messages;

import java.io.Serializable;

public class LoginRequestFromControllerMsg implements Serializable {

    private final String name;

    public LoginRequestFromControllerMsg(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
