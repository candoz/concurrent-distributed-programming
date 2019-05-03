package gameoflife_actors;

public class Config {

    final public static int N_WORKERS = 8;  //Runtime.getRuntime().availableProcessors();
    final public static String WORKER_NAME = "GameWorkerActor";

    final public static double RANDOM_ALIVE_FACTOR = 0.15;

    final public static int ROWS = 256;
    final public static int COLUMNS = 256;
    final public static int CELLSIDE_DIMENSION = 3;

    final public static int FRAMERATE_CAP = 60; // Hz

    final public static int ALIVE_COLOR = 0xFFFFFFFF;
    final public static int DEAD_COLOR = 0xFF000000;
    final public static int GRID_COLOR = 0xFF303030;

    final public static int MINIMUM_WINDOW_WIDTH = 850;
    final public static int MINIMUM_WINDOW_HEIGHT = 225;
    final public static String DECIMAL_FORMAT_PATTERN = "0.00";

}
