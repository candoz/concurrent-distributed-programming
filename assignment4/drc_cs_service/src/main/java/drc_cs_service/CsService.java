package drc_cs_service;

import com.rabbitmq.client.*;
import org.apache.commons.lang.SerializationUtils;

import java.util.HashMap;
import java.util.Map;

import static dictionary.MsgDictionary.*;

public class CsService {

    public static Long TIMEOUT_MILLIS = 20000L;

    private Connection connectionToDbService;
    private Connection connectionToMessagingService;
    private Connection connectionToMyAmqpServer;

    private Channel criticalSectionChannel;
    private Channel notifyChannel;
    private Channel enterCsDbChannel;
    private Channel exitCsDbChannel;

//    private Channel timeoutChannel;

    public void launch() throws Exception {
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
        enterCsDbChannel = connectionToDbService.createChannel();
        exitCsDbChannel = connectionToDbService.createChannel();
        notifyChannel = connectionToMessagingService.createChannel();
        criticalSectionChannel = connectionToMyAmqpServer.createChannel();

        // Setup timeout:
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", REQUEST_EXIT_CRITICAL_SECTION_QUEUE);
        args.put("x-message-ttl", TIMEOUT_MILLIS);
        criticalSectionChannel.queueDelete(REQUEST_TIMEOUT_CRITICAL_SECTION_QUEUE);
        criticalSectionChannel.queueDeclare(REQUEST_TIMEOUT_CRITICAL_SECTION_QUEUE, false, false, false, args);
    }

    private void setupHandlers() throws Exception {
        setupCriticalSectionRequestHandler(REQUEST_ENTER_CRITICAL_SECTION_QUEUE, enterCsDbChannel, REQUEST_SET_CRITICAL_SECTION_QUEUE);
        setupCriticalSectionRequestHandler(REQUEST_EXIT_CRITICAL_SECTION_QUEUE, exitCsDbChannel, REQUEST_UNSET_CRITICAL_SECTION_QUEUE);
        setupCriticalSectionResponseHandler(enterCsDbChannel, CS_ENTER_MESSAGE);
        setupCriticalSectionResponseHandler(exitCsDbChannel, CS_EXIT_MESSAGE);
    }

    private void setupCriticalSectionRequestHandler(String requestQueue, Channel dbChannel, String requestToDb) throws Exception {
        criticalSectionChannel.queueDeclare(requestQueue, false, false, false, null);
        criticalSectionChannel.basicConsume(requestQueue, true, new DefaultConsumer(criticalSectionChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                try {
                    dbChannel.basicPublish("", requestToDb, new AMQP.BasicProperties.Builder().headers(properties.getHeaders()).replyTo(RESPONSE_QUEUE).build(), null);
                    if (requestQueue.equals(REQUEST_ENTER_CRITICAL_SECTION_QUEUE)) {
                        criticalSectionChannel.basicPublish("", REQUEST_TIMEOUT_CRITICAL_SECTION_QUEUE, properties, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupCriticalSectionResponseHandler(Channel dbChannel, String messageType) throws Exception {
        dbChannel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(dbChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                try{
                    properties.getHeaders().put(MESSAGE_TYPE_HEADER, messageType);
                    byte[] message = SerializationUtils.serialize("");
                    notifyChannel.basicPublish("", REQUEST_MESSAGE_QUEUE, properties, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
