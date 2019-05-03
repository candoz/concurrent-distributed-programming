import io.reactivex.Observable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.rxjavafx.observers.JavaFxObserver;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Controller {

    @FXML
    private TextField rootTextField;
    @FXML
    private TextField regExpTextField;
    @FXML
    private TextField maxDepthTextField;
    @FXML
    private Button searchButton;
    @FXML
    private CheckBox concurrentlyCheck;
    @FXML
    private TextArea textArea;
    @FXML
    private Label nFilesLabel;
    @FXML
    private Label withMatchesPercentLabel;
    @FXML
    private Label avgMatchesLabel;

    final private DecimalFormat df = new DecimalFormat("#.#");
    private Path rootPath;
    private String regExp;
    private int maxDepth;

    @FXML
    private void searchButtonPressed(ActionEvent event) {
        rootPath = Paths.get(rootTextField.getText());
        regExp = regExpTextField.getText();
        maxDepth = Integer.parseInt(maxDepthTextField.getText());
        textArea.clear();

        List<Path> foundFiles = new LinkedList<>();
        try {
            foundFiles = Files.walk(rootPath, maxDepth)
                    .filter(Utils::isTextFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        ConnectableObservable<Report> reportsSource;

        if (concurrentlyCheck.isSelected()) {
            reportsSource = Observable.fromIterable(foundFiles)
                    .flatMap(val -> Observable.just(val)
                            .observeOn(Schedulers.io())
                            .map(c -> new SearchInFileCallable(c, regExp).call()))
                    .observeOn(Schedulers.computation())
                    .publish();
        } else {
            reportsSource = Observable.fromIterable(foundFiles)
                    .observeOn(Schedulers.newThread())  // Do not block the GUI!
                    .map(c -> new SearchInFileCallable(c, regExp).call())
                    .publish();
        }

        Observable<Integer> nFiles =
                reportsSource.map(val -> 1)
                        .scan((acc, val) -> acc + val);

        Observable<Integer> nFilesWithMatches =
                reportsSource.map(Report::getMatches)
                        .map(matches -> matches == 0 ? 0 : 1)
                        .scan((acc, val) -> acc + val);

        Observable<Integer> totalMatches =
                reportsSource.map(Report::getMatches)
                        .scan((acc, val) -> acc + val);

        Observable<String> incrementalPathList =
                reportsSource.filter(report -> report.getMatches() > 0)
                        .map(val -> val.toString() + "\n")
                        .scan((acc, entry) -> acc + entry);

        Observable<String> nFilesStr =
                nFiles.map(Object::toString);

        Observable<String> withMatchesPercent =
                nFilesWithMatches.zipWith(nFiles, (m, c) -> m.doubleValue() * 100 / c)
                        .map(val -> df.format(val) + "%");

        Observable<String> avgMatches =
                totalMatches.zipWith(nFilesWithMatches, (m, c) -> m.doubleValue() / c)
                        .map(df::format)
                        .map(Object::toString);

        nFilesLabel.textProperty()
                .bind(JavaFxObserver
                        .toBinding(nFilesStr
                                .observeOn(JavaFxScheduler.platform())));

        withMatchesPercentLabel.textProperty()
                .bind(JavaFxObserver
                        .toBinding(withMatchesPercent
                                .observeOn(JavaFxScheduler.platform())));

        avgMatchesLabel.textProperty()
                .bind(JavaFxObserver
                        .toBinding(avgMatches
                                .observeOn(JavaFxScheduler.platform())));
        
        textArea.textProperty()
                .bind(JavaFxObserver
                        .toBinding(incrementalPathList
                                .observeOn(JavaFxScheduler.platform())));

        reportsSource.connect();
    }
}
