import miscellaneous.Report;
import miscellaneous.Stats;
import tasks.SearchInFolderRecursiveTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.*;

import static miscellaneous.Config.POOL_SIZE;
import static miscellaneous.Config.SPLIT_FILES;


public class MainFullTasks {

    public static void main(String... args) {

        // Check args
        if (args.length != 3) {
            System.out.println("ARGS: node <ROOT_PATH> <REG_EXP> <MAX_DEPTH>\n");
            System.exit(1);
        }
        final Path rootPath = Paths.get(args[0]);
        final String regExp = args[1];
        int maxDepth = 0;
        try {
            maxDepth = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Third argument <MAX_DEPTH> should be an integer...\n");
            System.exit(1);
        }

        ForkJoinPool forkJoinPool = new ForkJoinPool(POOL_SIZE);

        ExecutorService searchInFileExecutor = Executors.newFixedThreadPool(POOL_SIZE);
        final Optional<ExecutorService> searchInTextExecutor;
        if (SPLIT_FILES) {
            searchInTextExecutor = Optional.of(Executors.newFixedThreadPool(POOL_SIZE));
        } else {
            searchInTextExecutor = Optional.empty();
        }

        final CompletionService<Report> searchInFileCompletionService = new ExecutorCompletionService<>(searchInFileExecutor);

        Long submittedTasks = forkJoinPool.invoke(
                new SearchInFolderRecursiveTask(
                        rootPath,
                        regExp,
                        1,
                        maxDepth,
                        searchInFileCompletionService,
                        searchInTextExecutor));

        searchInFileExecutor.shutdown();

        System.out.println("Found "+submittedTasks+" text files.");
        if (submittedTasks > 0) { System.out.println("Searching for matches in files..."); }

        Stats stats = new Stats();
        for (int handledResults = 0; handledResults < submittedTasks; handledResults++) {
            waitAndHandleNextResult(searchInFileCompletionService, stats);
        }

        searchInTextExecutor.ifPresent(ExecutorService::shutdown);

        System.out.println("\n\nGoodbye.");
    }

    private static void waitAndHandleNextResult(CompletionService completionService, Stats stats) {
        try {
            final Future<Report> futureReport = completionService.take();  // take is blocking until one of the tasks has finished
            final Report report = futureReport.get();

            stats.recordMatches(report.getMatches());

            if (report.getMatches() != 0 ) {
                System.out.println("\r"+report);
            }
            System.out.print("\r"+stats);

        } catch (InterruptedException e) {
            System.out.println("Error: Interrupted exception.");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println("Error: futureReport.get() threw an exception.");
            e.printStackTrace();
        }
    }

}
