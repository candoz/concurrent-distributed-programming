package drc_messaging_service;

public class Main {
    public static void main(String[] argv) {
        try {
            new MessagingService().launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
