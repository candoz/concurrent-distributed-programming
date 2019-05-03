package drc_logger_service;

public class Main {
    public static void main(String[] argv) {
        try {
            new LoggerService().launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
