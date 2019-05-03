package drc_db_service;

public class Main {
    public static void main(String[] argv) {
        try {
            new DbService().launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
