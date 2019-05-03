package drc_chatter;

import com.rabbitmq.client.Connection;

public class Connections {
    private Connection connectionToDbService;
    private Connection connectionToLoggerService;
    private Connection connectionToMessagingService;
    private Connection connectionToCsService;

    public Connections(Connection connectionToDbService, Connection connectionToLoggerService, Connection connectionToMessagingService, Connection connectionToCsService) {
        this.connectionToDbService = connectionToDbService;
        this.connectionToLoggerService = connectionToLoggerService;
        this.connectionToMessagingService = connectionToMessagingService;
        this.connectionToCsService = connectionToCsService;
    }

    public Connection getConnectionToDbService() {
        return connectionToDbService;
    }

    public Connection getConnectionToLoggerService() {
        return connectionToLoggerService;
    }

    public Connection getConnectionToMessagingService() {
        return connectionToMessagingService;
    }

    public Connection getConnectionToCsService() {
        return connectionToCsService;
    }
}
