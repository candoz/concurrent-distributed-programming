package gameoflife.config;

import gameoflife.World.InitType;
import static gameoflife.World.InitType.*;

public class GameConfig {

	final public static int WORKER_THREADS = 8; //Runtime.getRuntime().availableProcessors();

	final public static InitType INIT_TYPE = GLIDER;  // RANDOM, GLIDER, COLUMN, CROSS;
	final public static double RANDOM_ALIVE_FACTOR = 0.15;

	final public static int ROWS = 5000;
	final public static int COLUMNS = 5000;
	final public static boolean BORDERS = false;

	final public static int PERFORMANCE_UPDATE_FREQUENCY = 50;
	final public static String STARTING_PERFORMANCE_MESSAGE = "("+ WORKER_THREADS +" thread/s) " +
			"Starting performance evaluation: updating every "+PERFORMANCE_UPDATE_FREQUENCY+" ages ...";
	final public static String GAME_THREAD_NAME = "Game";
	final public static String GAMEWORKER_THREAD_NAME = "GameWorker";
	final public static String CONTROLLERWORKER_THREAD_NAME = "ControllerWorker";

	final public static String LEFT_SEPARATOR = "[";
	final public static String RIGHT_SEPARATOR = "]";

}
