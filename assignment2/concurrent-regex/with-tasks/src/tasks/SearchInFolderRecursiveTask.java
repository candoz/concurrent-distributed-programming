package tasks;

import miscellaneous.Report;
import miscellaneous.Utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RecursiveTask;

import static miscellaneous.Config.MAX_CHUNK_LENGTH;

public class SearchInFolderRecursiveTask extends RecursiveTask<Long> {

    final private Path folderPath;
    final private String regExp;
    final private int depth;
    final private int maxDepth;
    final private CompletionService<Report> searchInFileCompletionService;
    final private Optional<ExecutorService> searchInFileSplitExecutor;

    public SearchInFolderRecursiveTask(Path folderPath,
                                       String regExp,
                                       int depth,
                                       int maxDepth,
                                       CompletionService<Report> searchInFileCompletionService,
                                       Optional<ExecutorService> searchInFileSplitExecutor) {
        this.folderPath = folderPath;
        this.regExp = regExp;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.searchInFileCompletionService = searchInFileCompletionService;
        this.searchInFileSplitExecutor = searchInFileSplitExecutor;
    }

    @Override
    protected Long compute() {
        Long nTextFilesFound = 0L;
        List<RecursiveTask<Long>> forks = new LinkedList<RecursiveTask<Long>>();
        if (depth <= maxDepth) {
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath);
                for (Path path : stream) {
                    if (Utils.isTextFile(path)) {
                        if (searchInFileSplitExecutor.isPresent()) {
                            searchInFileCompletionService.submit(new SearchInFileTaskSplit(path, regExp, searchInFileSplitExecutor.get(), MAX_CHUNK_LENGTH));
                        } else {
                            searchInFileCompletionService.submit(new SearchInFileTask(path, regExp));
                        }
                        nTextFilesFound++;
                    } else if (Files.isDirectory(path)) {
                        SearchInFolderRecursiveTask subFolderTask = new SearchInFolderRecursiveTask(
                                path,
                                regExp,
                                depth+1,
                                maxDepth,
                                searchInFileCompletionService,
                                searchInFileSplitExecutor);
                        forks.add(subFolderTask);
                        subFolderTask.fork();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        for (RecursiveTask<Long> task : forks) {
            nTextFilesFound = nTextFilesFound + task.join();
        }
        return nTextFilesFound;
    }
}
