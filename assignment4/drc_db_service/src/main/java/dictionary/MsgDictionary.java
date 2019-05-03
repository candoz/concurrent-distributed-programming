package dictionary;

public class MsgDictionary {
    public final static String REQUEST_CHATROOMS_QUEUE = "request-chatrooms-queue";
    public final static String REQUEST_CHATTERS_QUEUE = "request-chatters-queue";
    public final static String REQUEST_CREATE_CHATROOM_QUEUE = "request-create-chatroom-queue";
    public final static String REQUEST_TICKET_QUEUE = "request-ticket-queue";
    public final static String REQUEST_MESSAGE_QUEUE = "request-message-queue";
    public final static String REQUEST_ADD_CHATTER_QUEUE = "request-add-chatter-queue";
    public final static String REQUEST_REMOVE_CHATTER_QUEUE = "request-remove-chatter-queue";
    public final static String REQUEST_LOGIN_QUEUE = "request-login-queue";
    public final static String REQUEST_LOGOUT_QUEUE = "request-logout-queue";
    public final static String RESPONSE_QUEUE = "amq.rabbitmq.reply-to";
    public final static String REQUEST_SET_CRITICAL_SECTION_QUEUE = "request-set-critical-section-queue";
    public final static String REQUEST_UNSET_CRITICAL_SECTION_QUEUE = "request-unset-critical-section-queue";
    public final static String REQUEST_ENTER_CRITICAL_SECTION_QUEUE = "request-enter-critical-section-queue";
    public final static String REQUEST_EXIT_CRITICAL_SECTION_QUEUE = "request-exit-critical-section-queue";

    public final static String CHATROOM_NAME_HEADER = "chatroom-name-header";
    public final static String CHATTER_NAME_HEADER = "chatter-name-header";
    public final static String CHATTERS_LIST_HEADER = "chatters-list-header";
    public final static String CHATTER_SENDER_QUEUE_HEADER = "chatter-sender-queue-header";
    public final static String TICKET_HEADER = "ticket-header";
    public final static String STATE_HEADER = "state-header";
    public final static String MESSAGE_TYPE_HEADER = "message-type-header";

    public final static String OK_STATE = "OK";
    public final static String ERROR_STATE = "ERROR";
    public final static String LOGIN_MESSAGE = "login-message";
    public final static String LOGOUT_MESSAGE = "logout-message";
    public final static String CS_ENTER_MESSAGE = "cs-enter-message";
    public final static String CS_EXIT_MESSAGE = "cs-exit-message";
    public final static String CHATTER_MESSAGE = "chatter-message";

    public final static String DB_URL = "jdbc:postgresql://ec2-54-247-100-44.eu-west-1.compute.amazonaws.com:5432/d9eruaaljb2915";
    public final static String DB_USER = "zeyjbpfzitvzwo";
    public final static String DB_PASSWORD = "f221f834b1c092c7f7f5fe5113778d6cb2014da0502bf8f1c3460ce31b1ff86f";
}