import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static boolean isTextFile(Path file) {
        boolean isText = false;
        try {
            isText = Files.isRegularFile(file) && (Files.probeContentType(file)).startsWith("text");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return isText;
        }
    }

    public static int countMatches(String text, String regExp) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(text);
        int matches = 0;
        while (matcher.find()) {
            matches++;
        }
        return matches;
    }
}
