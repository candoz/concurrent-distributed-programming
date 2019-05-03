package miscellaneous;

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

    public static String[] splitText(String text, int maxChunkLength) {

        int totalLength = text.length();
        int safeMaxChunkLenght = Math.min(maxChunkLength, totalLength);

        int numChunks = totalLength / safeMaxChunkLenght;
        int remainder = totalLength - numChunks * safeMaxChunkLenght;
        if (remainder != 0) {
            numChunks++;
            remainder = totalLength - numChunks * safeMaxChunkLenght;
        }
        String[] chunks = new String[numChunks];
        int chunkSize = totalLength / numChunks;

        int count = 0;
        for (int i = 0; i < numChunks; i++) {
            int currentChunkSize = chunkSize;
            if (i < remainder) {
                currentChunkSize++;
            }
//            System.out.println("{"+currentChunkSize+"}"+chunks[i]);
            chunks[i] = text.substring(count, count + currentChunkSize);
            count += currentChunkSize;
        }

        return chunks;
    }

}
