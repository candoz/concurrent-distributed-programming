package drc_messaging_service;

import com.rabbitmq.client.*;

import java.util.ArrayList;

import static dictionary.MsgDictionary.*;

public class MessagingService {
    private Connection connectionToDbService;
    private Connection connectionToMyAmqpServer;
    private Channel messageChannel;
    private Channel ticketChannel;
    private Channel chattersListChannel;

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
        factory.setUri(System.getenv("CLOUDAMQP_URL"));
        connectionToMyAmqpServer = factory.newConnection();
    }

    private void setupChannels() throws Exception {
        messageChannel = connectionToMyAmqpServer.createChannel();
        ticketChannel = connectionToDbService.createChannel();
        chattersListChannel = connectionToDbService.createChannel();
    }

    private void setupHandlers() throws Exception {
        setupMessageHandler();
        setupTicketHandler();
        setupChattersListHandler();
    }

    private void setupMessageHandler() throws Exception {
        messageChannel.queueDeclare(REQUEST_MESSAGE_QUEUE, false, false, false, null);
        messageChannel.basicConsume(REQUEST_MESSAGE_QUEUE, true, new DefaultConsumer(messageChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                try {
                    if(!properties.getHeaders().containsKey(MESSAGE_TYPE_HEADER)) {
                        properties.getHeaders().put(MESSAGE_TYPE_HEADER, CHATTER_MESSAGE);
                    }
                    ticketChannel.basicPublish("", REQUEST_TICKET_QUEUE, new AMQP.BasicProperties.Builder().headers(properties.getHeaders()).replyTo(RESPONSE_QUEUE).build(), body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupTicketHandler() throws Exception {
        ticketChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(ticketChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                try{
                    chattersListChannel.basicPublish("", REQUEST_CHATTERS_QUEUE, new AMQP.BasicProperties.Builder().headers(properties.getHeaders()).replyTo(RESPONSE_QUEUE).build(), body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupChattersListHandler() throws Exception {
        chattersListChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(chattersListChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                try{
                    String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                    ArrayList<Object> chattersNames = (ArrayList<Object>)properties.getHeaders().get(CHATTERS_LIST_HEADER);
                    Channel chatroomChannel = connectionToMyAmqpServer.createChannel();
                    chatroomChannel.exchangeDeclare(chatroomName,"fanout");
                    for(Object chatterName : chattersNames) {
                        String queueName = chatterName+chatroomName;
                        chatroomChannel.queueDeclare(queueName, false, false, false, null);
                        chatroomChannel.queueBind(queueName, chatroomName, "");
                    }
                    properties.getHeaders().remove(CHATTERS_LIST_HEADER);
                    chatroomChannel.basicPublish(chatroomName, "", properties, body);
                    chatroomChannel.exchangeDelete(chatroomName);
                    chatroomChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}