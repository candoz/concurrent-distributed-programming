package gameoflife;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static gameoflife.config.ViewConfig.MINIMUM_WINDOW_HEIGHT;
import static gameoflife.config.ViewConfig.MINIMUM_WINDOW_WIDTH;


public class Main extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		Scene scene = new Scene(FXMLLoader.load(getClass().getResource("gui.fxml")));
		primaryStage.setTitle("Game of Life");
		primaryStage.setScene(scene);
		primaryStage.setMinHeight(MINIMUM_WINDOW_HEIGHT);
		primaryStage.setMinWidth(MINIMUM_WINDOW_WIDTH);
		primaryStage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}
