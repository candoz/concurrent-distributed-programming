package tasks;

import miscellaneous.Report;
import miscellaneous.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Search for regexp matches in file splitting it if necessary (and using an executor service in that case).
 */
public class SearchInFileTaskSplit extends SearchInFileTask implements Callable<Report> {

    final protected ExecutorService executor;
    final protected int maxChunkLength;

    public SearchInFileTaskSplit(Path filePath, String regExp, ExecutorService executor, int maxChunkLength) {
        super(filePath, regExp);
        this.executor = executor;
        this.maxChunkLength = maxChunkLength;
    }

    @Override
    public Report call() throws Exception {
        Report report;
        String fileContent = new String(Files.readAllBytes(filePath));
        if (fileContent.length() < maxChunkLength) {
            report = super.call();
        } else {
            report = new Report(filePath, splitAndCount(fileContent));
        }
        return report;
    }

    private int splitAndCount(String text) {
        String[] textChunks = Utils.splitText(text, maxChunkLength);
        List<Future<Integer>> results = new LinkedList<>();
        for (String chunk: textChunks) {
            results.add(executor.submit(new SearchInTextTask(chunk, regExp)));
        }

        int total = 0;
        for (Future<Integer> result: results) {
            try {
                total += result.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return total;
    }

}
