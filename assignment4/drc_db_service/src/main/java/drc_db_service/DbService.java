package drc_db_service;

import com.rabbitmq.client.*;
import org.apache.commons.lang.SerializationUtils;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

import static dictionary.MsgDictionary.*;

public class DbService {
    private com.rabbitmq.client.Connection connection;
    private Channel chatroomsListChannel;
    private Channel chattersListChannel;
    private Channel createChatroomChannel;
    private Channel addChatterChannel;
    private Channel removeChatterChannel;
    private Channel ticketChannel;
    private Channel setCriticalSectionChannel;
    private Channel unsetCriticalSectionChannel;

    public void launch() throws Exception {
        setupConnection();
        setupChannels();
        setupHandlers();
    }

    private void setupConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(System.getenv("CLOUDAMQP_URL"));
        factory.setConnectionTimeout(30000);
        connection = factory.newConnection();
    }

    private void setupChannels() throws Exception {
        chatroomsListChannel = connection.createChannel();
        chattersListChannel = connection.createChannel();
        createChatroomChannel = connection.createChannel();
        addChatterChannel = connection.createChannel();
        removeChatterChannel = connection.createChannel();
        ticketChannel = connection.createChannel();
        setCriticalSectionChannel = connection.createChannel();
        unsetCriticalSectionChannel = connection.createChannel();
    }

    private void setupHandlers() throws Exception {
        setupChatroomsListHandler();
        setupChattersListHandler();
        setupCreateChatroomHandler();
        setupAddChatterHandler();
        setupRemoveChatterHandler();
        setupTicketHandler();
        setupSetCriticalSectionHandler();
        setupUnsetCriticalSectionHandler();
    }

    private void setupChatroomsListHandler() throws Exception {
        chatroomsListChannel.queueDeclare(REQUEST_CHATROOMS_QUEUE, false, false, false, null);
        chatroomsListChannel.basicConsume(REQUEST_CHATROOMS_QUEUE, true, new DefaultConsumer(chatroomsListChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                ArrayList<String> list = new ArrayList<>();
                java.sql.Connection sqlConnection = null;
                try {

                    sqlConnection = connectToDb();
                    Statement statement = sqlConnection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM chatrooms;");
                    while (resultSet.next()) {
                        list.add(resultSet.getString("name"));
                    }
                    resultSet.close();
                    statement.close();

                    byte[] chatroomsList = SerializationUtils.serialize(list);
                    chatroomsListChannel.basicPublish("", properties.getReplyTo(), null, chatroomsList);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupChattersListHandler() throws Exception {
        chattersListChannel.queueDeclare(REQUEST_CHATTERS_QUEUE, false, false, false, null);
        chattersListChannel.basicConsume(REQUEST_CHATTERS_QUEUE, true, new DefaultConsumer(chattersListChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                ArrayList<Object> chattersNames = new ArrayList<>();
                java.sql.Connection sqlConnection = null;
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                try {

                    sqlConnection = connectToDb();
                    PreparedStatement statement = sqlConnection.prepareStatement("SELECT chatter_name FROM chatters WHERE chatroom_name=?;");
                    statement.setString(1, chatroomName);
                    ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        chattersNames.add(resultSet.getString("chatter_name"));
                    }
                    resultSet.close();
                    statement.close();

                    properties.getHeaders().put(CHATTERS_LIST_HEADER, chattersNames);
                    chattersListChannel.basicPublish("", properties.getReplyTo(), properties, body);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupCreateChatroomHandler() throws Exception {
        createChatroomChannel.queueDeclare(REQUEST_CREATE_CHATROOM_QUEUE, false, false, false, null);
        createChatroomChannel.basicConsume(REQUEST_CREATE_CHATROOM_QUEUE, true, new DefaultConsumer(createChatroomChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                java.sql.Connection sqlConnection = null;
                properties.getHeaders().put(CHATROOM_NAME_HEADER, chatroomName);
                try {

                    sqlConnection = connectToDb();
                    PreparedStatement statement = sqlConnection.prepareStatement("INSERT INTO chatrooms VALUES (?);");
                    statement.setString(1, chatroomName);
                    statement.executeUpdate();
                    statement.close();

                    properties.getHeaders().put(STATE_HEADER, OK_STATE);
                    createChatroomChannel.basicPublish("", properties.getReplyTo(), properties, null);
                } catch (Exception e) {
                    try {
                        properties.getHeaders().put(STATE_HEADER, ERROR_STATE);
                        createChatroomChannel.basicPublish("", properties.getReplyTo(), properties, null);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupAddChatterHandler() throws Exception {
        addChatterChannel.queueDeclare(REQUEST_ADD_CHATTER_QUEUE, false, false, false, null);
        addChatterChannel.basicConsume(REQUEST_ADD_CHATTER_QUEUE, true, new DefaultConsumer(addChatterChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                java.sql.Connection sqlConnection = null;
                String chatterName = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                try {

                    sqlConnection = connectToDb();
                    PreparedStatement statement = sqlConnection.prepareStatement("INSERT INTO chatters VALUES (?, ?);");
                    statement.setString(1, chatterName);
                    statement.setString(2, chatroomName);
                    statement.executeUpdate();
                    statement.close();

                    PreparedStatement statementTicket = sqlConnection.prepareStatement("SELECT ticket FROM chatrooms WHERE name=?;");
                    statementTicket.setString(1, chatroomName);
                    ResultSet resultSet = statementTicket.executeQuery();
                    resultSet.next();
                    final long ticketNumber = Long.parseLong(resultSet.getString("ticket"));
                    resultSet.close();
                    statementTicket.close();

                    properties.getHeaders().put(STATE_HEADER, OK_STATE);
                    properties.getHeaders().put(TICKET_HEADER, ticketNumber);
                    addChatterChannel.basicPublish("", properties.getReplyTo(), properties, null);

                } catch (Exception e0) {
                    try {
                        properties.getHeaders().put(STATE_HEADER, ERROR_STATE);
                        addChatterChannel.basicPublish("", properties.getReplyTo(), properties, null);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupRemoveChatterHandler() throws Exception {
        removeChatterChannel.queueDeclare(REQUEST_REMOVE_CHATTER_QUEUE, false, false, false, null);
        removeChatterChannel.basicConsume(REQUEST_REMOVE_CHATTER_QUEUE, true, new DefaultConsumer(removeChatterChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                java.sql.Connection sqlConnection = null;
                String chatterName = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                try {

                    sqlConnection = connectToDb();
                    String query =
                            "DELETE FROM chatters " +
                            "WHERE chatter_name=? AND chatroom_name=?;";
                    PreparedStatement statement = sqlConnection.prepareStatement(query);
                    statement.setString(1, chatterName);
                    statement.setString(2, chatroomName);
                    statement.executeUpdate();
                    statement.close();

                    properties.getHeaders().put(STATE_HEADER, OK_STATE);
                    removeChatterChannel.basicPublish("", properties.getReplyTo(), properties, null);
                } catch (Exception e0) {
                    e0.printStackTrace();
                    try {
                        properties.getHeaders().put(STATE_HEADER, ERROR_STATE);
                        removeChatterChannel.basicPublish("", properties.getReplyTo(), properties, null);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupTicketHandler() throws Exception {
        ticketChannel.queueDeclare(REQUEST_TICKET_QUEUE, false, false, false, null);
        ticketChannel.basicConsume(REQUEST_TICKET_QUEUE, true, new DefaultConsumer(ticketChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                java.sql.Connection sqlConnection = null;
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                String chatterName = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                String messageType = properties.getHeaders().get(MESSAGE_TYPE_HEADER).toString();
                try {
                    sqlConnection = connectToDb();
                    String query;
                    PreparedStatement statement;
                    if(messageType.equals(LOGIN_MESSAGE) || messageType.equals(LOGOUT_MESSAGE)) {
                        query = "UPDATE chatrooms " +
                                "SET ticket=ticket+1 " +
                                "WHERE name=? " +
                                "RETURNING ticket;";
                        statement = sqlConnection.prepareStatement(query);
                        statement.setString(1, chatroomName);
                    } else {
                        query = "UPDATE chatrooms " +
                                "SET ticket=ticket+1 " +
                                "WHERE name=? AND (chatter_in_critical_section IS NULL OR chatter_in_critical_section=?) " +
                                "RETURNING ticket;";
                        statement = sqlConnection.prepareStatement(query);
                        statement.setString(1, chatroomName);
                        statement.setString(2, chatterName);
                    }
                    ResultSet resultSet = statement.executeQuery();
                    if(resultSet.next()) {  //query andata a buon fine, nessuno è in cs o ci sono io, rispondo al messaging_service
                        final long ticketNumber = Long.parseLong(resultSet.getString("ticket"));
                        properties.getHeaders().put(TICKET_HEADER, ticketNumber);
                        ticketChannel.basicPublish("", properties.getReplyTo(), properties, body);
                    }
                    resultSet.close();
                    statement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupSetCriticalSectionHandler() throws Exception {
        setCriticalSectionChannel.queueDeclare(REQUEST_SET_CRITICAL_SECTION_QUEUE, false, false, false, null);
        setCriticalSectionChannel.basicConsume(REQUEST_SET_CRITICAL_SECTION_QUEUE, true, new DefaultConsumer(setCriticalSectionChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                java.sql.Connection sqlConnection = null;
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                String chatterName = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                try {

                    sqlConnection = connectToDb();
                    String query = "UPDATE chatrooms " +
                                    "SET chatter_in_critical_section=? " +
                                    "WHERE name=? AND chatter_in_critical_section IS NULL;";
                    PreparedStatement statement = sqlConnection.prepareStatement(query);
                    statement.setString(1, chatterName);
                    statement.setString(2, chatroomName);
                    if(statement.executeUpdate() == 1) {
                        setCriticalSectionChannel.basicPublish("", properties.getReplyTo(), properties, body);
                    }
                    statement.close();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void setupUnsetCriticalSectionHandler() throws Exception {
        unsetCriticalSectionChannel.queueDeclare(REQUEST_UNSET_CRITICAL_SECTION_QUEUE, false, false, false, null);
        unsetCriticalSectionChannel.basicConsume(REQUEST_UNSET_CRITICAL_SECTION_QUEUE, true, new DefaultConsumer(unsetCriticalSectionChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
                java.sql.Connection sqlConnection = null;
                String chatroomName = properties.getHeaders().get(CHATROOM_NAME_HEADER).toString();
                String chatterName = properties.getHeaders().get(CHATTER_NAME_HEADER).toString();
                try {

                    sqlConnection = connectToDb();
                    String query = "UPDATE chatrooms " +
                            "SET chatter_in_critical_section=NULL " +
                            "WHERE name=? AND chatter_in_critical_section=?;";
                    PreparedStatement statement = sqlConnection.prepareStatement(query);
                    statement.setString(1, chatroomName);
                    statement.setString(2, chatterName);
                    if(statement.executeUpdate() == 1) {
                        unsetCriticalSectionChannel.basicPublish("", properties.getReplyTo(), properties, body);
                    }
                    statement.close();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(sqlConnection != null) {
                        try {
                            sqlConnection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private java.sql.Connection connectToDb() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

}
