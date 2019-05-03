import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

	final private static double MIN_HEIGHT = 400;
	final private static double MIN_WIDTH =  600;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		Scene scene = null;
		Parent parent = null;
		try {
			parent = FXMLLoader.load(getClass().getResource("gui.fxml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		primaryStage.setTitle("Regexp Finder Tool");
		primaryStage.setScene(new Scene(parent));
		primaryStage.setResizable(false); //non ridimensionabile
		primaryStage.setMinWidth(MIN_WIDTH);
		primaryStage.setMinHeight(MIN_HEIGHT);
		primaryStage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		primaryStage.show();
	}
}
