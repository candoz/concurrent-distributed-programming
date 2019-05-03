package drc_cs_service;

public class Main {
    public static void main(String[] argv) {
        try {
            new CsService().launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
