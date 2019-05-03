package distributed_chat.register;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class MainRegister {

    public static void main(String[] args) {
        Config config = ConfigFactory.parseFile(new File("src/main/java/distributed_chat/register/register.conf"));
        ActorSystem system = ActorSystem.create("MySystem",config);
        system.actorOf(Props.create(Register.class), "register");
    }

}
