package miscellaneous;

public class Config {

    final public static boolean SPLIT_FILES = true;
    final public static int MAX_CHUNK_LENGTH = 10;  // relevant if SPLIT_FILES == true
    final public static int POOL_SIZE = Runtime.getRuntime().availableProcessors();

}
