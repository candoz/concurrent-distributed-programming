package gameoflife;

import gameoflife.workers.ControllerWorker;
import gameoflife.workers.Worker;
import gameoflife.workers.WorkerParameters;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import utils.Chrono;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;

import static gameoflife.config.GameConfig.*;
import static gameoflife.config.ViewConfig.*;

public class Controller implements Initializable, Visualizer {

	@FXML
	private ScrollPane scroll;
	@FXML
	private Label populationLabel;
	@FXML
	private Label populationPercentageLabel;
	@FXML
	private Label eraLabel;

	private Game game;
	private int rows;
	private int columns;
	private byte[] imageData;
	private Semaphore visualizerSem = new Semaphore(0);

	private ControllerWorker workers[] = new ControllerWorker[WORKER_THREADS];
	private Semaphore beginWorkSems[] = new Semaphore[WORKER_THREADS];
	private Semaphore endWorkSems[] = new Semaphore[WORKER_THREADS];

	private DecimalFormat decimalFormat = new DecimalFormat(DECIMAL_FORMAT_PATTERN);
	private PixelFormat pixelFormat = PixelFormat.createByteIndexedInstance(new int[]{DEAD_COLOR, ALIVE_COLOR});


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		game = new Game();
		rows = game.getWorld().getRows();
		columns = game.getWorld().getColumns();
		imageData = new byte[rows * columns];
		game.setVisualizer(this);
		game.setChrono(new Chrono());
		setUpWorkers();
		for (Worker worker: workers) {
			worker.start();
		}
		visualizerSem.release();
		visualize(game.getWorld(), game.getWorld().getPopulation(), 0);
		game.start();
	}

	@FXML
	private void startPressed(ActionEvent event) {
		game.resumeGame();
	}

	@FXML
	private void stopPressed(ActionEvent event) {
		game.pauseGame();
	}

	@Override
	public void visualize(World world, long population, long age) {
		final Canvas canvas = new Canvas();
		canvas.setHeight(world.getRows());
		canvas.setWidth(world.getColumns());
		GraphicsContext gc = canvas.getGraphicsContext2D();
		PixelWriter pixelWriter = gc.getPixelWriter();
		encodeImageData(world);
		pixelWriter.setPixels(0, 0, world.getColumns(), world.getRows(), pixelFormat, imageData, 0, world.getColumns());
		try {
			visualizerSem.acquire();  // synchronization between Game and GUI thread
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Platform.runLater(() -> {
			scroll.setContent(canvas);
			populationLabel.setText(""+population);
			populationPercentageLabel.setText("("+ decimalFormat.format((double)population*100/(rows*columns))+"%)");
			eraLabel.setText(""+age);
			visualizerSem.release();
		});
		if (SLOW_DOWN) {
			try { Thread.sleep(MILLIS_TO_WAIT); }
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}

	private void encodeImageData(World world) {
		for (int i = 0; i < workers.length; i++) {
			workers[i].setWorld(world);
			beginWorkSems[i].release();
		}
		for (int i = 0; i < workers.length; i++) {
			try {
				endWorkSems[i].acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void setUpWorkers() {
		WorkerParameters parameters[] = Worker.inferParameters(game.getWorld(), workers.length);
		for (int i = 0; i < workers.length; i++) {
			beginWorkSems[i] = new Semaphore(0);
			endWorkSems[i] = new Semaphore(0);
			workers[i] = new ControllerWorker(CONTROLLERWORKER_THREAD_NAME+i, game.getWorld(),
					parameters[i].getFromRow(), parameters[i].getFromColumn(), parameters[i].getNcells(),
					beginWorkSems[i], endWorkSems[i], imageData);
		}
	}

}
