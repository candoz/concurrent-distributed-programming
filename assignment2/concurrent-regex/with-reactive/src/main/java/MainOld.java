import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;


public class MainOld {

    public static void main(String[] args) {

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

        List<Path> foundFiles = new LinkedList<>();
        Stats stats = new Stats();

        try {
             foundFiles = Files.walk(rootPath, maxDepth)
                               .filter(Utils::isTextFile)
                               .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* SINGLE STREAM WITH SEQUENTIAL OPERATIONS */
//        Observable.fromArray(foundFiles.stream().toArray())
//                .cast(Path.class)
//                .subscribeOn(Schedulers.computation())
//                .map(
//                    path -> new SearchInFileCallable(path, regex).call()
//                )
//                .blockingSubscribe(
//                        result -> {
//                                    System.out.println("manager: " + Thread.currentThread());
//
//                            int matches = result.getMatches();
//                            stats.recordMatches(matches);
//                            if (matches != 0 ) {
//                                System.out.println("\r["+matches+"] "+result.getFilePath());
//                            }
//                            System.out.print("\r"+stats.toString());
//                        },
//                        error -> System.out.println(error.getMessage()),
//                        () -> System.out.println("\nCompleted")
//                );

        /* SINGLE STREAM WITH PARALLEL RESEARCH IN MORE FILES  */
        Observable<Report> reportsSource =
                Observable.fromIterable(foundFiles)
                          .flatMap(val -> Observable.just(val)
                                                    .subscribeOn(Schedulers.computation())
                                                    .map(c -> new SearchInFileCallable(c, regExp).call()));

        /* THE MAIN FLOW MANAGES RESULTS */
        reportsSource.blockingSubscribe(
                report -> {
                    int matches = report.getMatches();
                    stats.recordMatches(matches);
                    if (matches != 0) {
                        System.out.println("\r" + report);
                    }
                    System.out.print("\r" + stats);
                },
                error -> System.out.println(error.getMessage()),
                () -> System.out.println("\n\nCompleted")
        );
    }
}
