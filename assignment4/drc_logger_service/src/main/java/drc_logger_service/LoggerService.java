package drc_logger_service;

import com.rabbitmq.client.*;
import org.apache.commons.lang.SerializationUtils;

import static dictionary.MsgDictionary.*;

public class LoggerService {
    private Connection connectionToDbService;
    private Connection connectionToMyAmqpServer;
    private Connection connectionToMessagingService;
    private Channel loggerChannel;
    private Channel notifyChannel;
    private Channel loginDbChannel;
    private Channel logoutDbChannel;

    public void launch() throws Exception{
        setupConnection();
        setupChannels();
        setupHandlers();
    }

    private void setupConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setConnectionTimeout(30000);
        factory.setUri("amqp://pvawzpgd:oZJzaxqO_7uTUkyiH_rLZXm5TSPKMITs@wolverine.rmq.cloudamqp.com/pvawzpgd");
        connectionToDbService = factory.newConnection();
        factory.setUri("amqp://jmrbfcuy:16cLgtQ71ChRr1xxDbPJR1rhpXJ2H4_v@wolverine.rmq.cloudamqp.com/jmrbfcuy");
        connectionToMessagingService = factory.newConnection();
        factory.setUri(System.getenv("CLOUDAMQP_URL"));
        connectionToMyAmqpServer = factory.newConnection();
    }

    private void setupChannels() throws Exception {
        loggerChannel = connectionToMyAmqpServer.createChannel();
        notifyChannel = connectionToMessagingService.createChannel();
        loginDbChannel = connectionToDbService.createChannel();
        logoutDbChannel = connectionToDbService.createChannel();
    }

    private void setupHandlers() throws Exception {
        setupLoggerHandler(REQUEST_LOGIN_QUEUE, loginDbChannel, REQUEST_ADD_CHATTER_QUEUE);
        setupLoggerHandler(REQUEST_LOGOUT_QUEUE, logoutDbChannel, REQUEST_REMOVE_CHATTER_QUEUE);
        setupLoginHandler();
        setupLogoutHandler();
    }

    private void setupLoggerHandler(String requestQueue, Channel dbChannel, String requestToDb) throws Exception {
        loggerChannel.queueDeclare(requestQueue, false, false, false, null);
        loggerChannel.basicConsume(requestQueue, true, new DefaultConsumer(loggerChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                try {
                    properties.getHeaders().put(CHATTER_SENDER_QUEUE_HEADER, properties.getReplyTo());
                    dbChannel.basicPublish("", requestToDb, new AMQP.BasicProperties.Builder().headers(properties.getHeaders()).replyTo(RESPONSE_QUEUE).build(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupLoginHandler() throws Exception {
        loginDbChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(loginDbChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                try{
                    loggerChannel.basicPublish("", properties.getHeaders().get(CHATTER_SENDER_QUEUE_HEADER).toString(), properties, null);
                    if(properties.getHeaders().get(STATE_HEADER).toString().equals(OK_STATE)){
                        properties.getHeaders().put(MESSAGE_TYPE_HEADER, LOGIN_MESSAGE);
                        properties.getHeaders().remove(CHATTER_SENDER_QUEUE_HEADER);
                        properties.getHeaders().remove(STATE_HEADER);
                        byte[] message = SerializationUtils.serialize("");
                        notifyChannel.basicPublish("", REQUEST_MESSAGE_QUEUE, properties, message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupLogoutHandler() throws Exception {
        logoutDbChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(logoutDbChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                try{
                    properties.getHeaders().put(MESSAGE_TYPE_HEADER, LOGOUT_MESSAGE);
                    properties.getHeaders().remove(CHATTER_SENDER_QUEUE_HEADER);
                    byte[] message = SerializationUtils.serialize("");
                    notifyChannel.basicPublish("", REQUEST_MESSAGE_QUEUE, properties, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}