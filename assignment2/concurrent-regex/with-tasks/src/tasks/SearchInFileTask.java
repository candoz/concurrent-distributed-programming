package tasks;

import miscellaneous.Report;
import miscellaneous.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Search for regexp matches in file without splitting it.
 */
public class SearchInFileTask implements Callable<Report> {

    final protected Path filePath;
    final protected String regExp;

    public SearchInFileTask(Path filePath, String regExp) {
        this.filePath = filePath;
        this.regExp = regExp;
    }

    @Override
    public Report call() throws Exception {
        String fileContent = new String(Files.readAllBytes(filePath));
        return new Report(filePath, Utils.countMatches(fileContent, regExp));
    }

}
