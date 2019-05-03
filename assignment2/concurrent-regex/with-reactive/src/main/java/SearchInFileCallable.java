import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class SearchInFileCallable implements Callable<Report> {

    final private Path filePath;
    final private String regExp;

    public SearchInFileCallable(Path filePath, String regExp) {
        this.filePath = filePath;
        this.regExp = regExp;
    }

    @Override
    public Report call() throws Exception {

//        System.out.println("Working on file in thread: " + Thread.currentThread().getId());
//        Thread.sleep(1000);

        String fileContent = new String(Files.readAllBytes(filePath));

        int matches = Utils.countMatches(fileContent, regExp);
        return new Report(filePath, matches);
    }
}
