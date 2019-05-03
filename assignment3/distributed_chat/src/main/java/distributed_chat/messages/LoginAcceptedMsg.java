package distributed_chat.messages;

import java.io.Serializable;

public class LoginAcceptedMsg implements Serializable {
    private final long pulse;

    public LoginAcceptedMsg(long pulse) { this.pulse = pulse; }

    public long getPulse() {
        return pulse;
    }
}