package gameoflife_actors;

import gameoflife_actors.messages.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import static gameoflife_actors.Config.*;

public class Controller implements Initializable, Visualizer  {

    @FXML
    private ScrollPane scroll;
    @FXML
    private Label populationLabel;
    @FXML
    private Label populationPercentageLabel;
    @FXML
    private Label eraLabel;
    @FXML
    private Label computationTimeLabel;

    private PixelWriter pixelWriter;
    private PixelFormat pixelFormat = PixelFormat.createByteIndexedInstance(new int[]{DEAD_COLOR, ALIVE_COLOR, GRID_COLOR});
    private DecimalFormat decimalFormat = new DecimalFormat(DECIMAL_FORMAT_PATTERN);

    private int imageWidth = ((COLUMNS * CELLSIDE_DIMENSION) + COLUMNS + 1);
    private int imageHeight = ((ROWS * CELLSIDE_DIMENSION) + ROWS + 1);
    private byte[] imageData;

    private ActorRef visualizerActor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ActorSystem system = ActorSystem.create("GameOfLife");
        visualizerActor = system.actorOf(Props.create(VisualizerActor.class, this));

        imageData = new byte[imageHeight * imageWidth];
        WritableImage image = new WritableImage(imageWidth, imageHeight);
        ImageView iv = new ImageView(image);
        pixelWriter = image.getPixelWriter();
        scroll.setContent(iv);
    }

    @FXML
    private void startPressed(ActionEvent event) {
        visualizerActor.tell(new ResumeMsg(), ActorRef.noSender());
    }

    @FXML
    private void stopPressed(ActionEvent event) {
        visualizerActor.tell(new PauseMsg(), ActorRef.noSender());
    }

    @Override
    public void visualize(World world, long population, long era, long computationTime) {
        encodeImageData(world);
        Platform.runLater(() -> {
            pixelWriter.setPixels(0, 0, imageWidth, imageHeight, pixelFormat, imageData, 0, imageWidth);
            populationLabel.setText("" + population);
            populationPercentageLabel.setText("(" + decimalFormat.format((double) population * 100 / (Config.ROWS * Config.COLUMNS)) + "%)");
            eraLabel.setText("" + era);
            computationTimeLabel.setText(computationTime + "us");
            visualizerActor.tell(new RepaintFinishedMsg(), ActorRef.noSender());

        });
    }

    /**
     * Encodes the world state as an array of bytes.
     * The resulting image has a 1px grid and every inner cell is a square with side CELLSIDE_DIMENSION.
     * We did not use the JavaFX Canvas drawing capabilities for two main reasons:
     * 1) It was slower than our solution (and sadly much, much slower than the Swing alternative...).
     * 2) Canvas image size was limited to 8192x8192 pixels with our graphics cards.
     *    (see: https://stackoverflow.com/questions/17563827/maximum-dimensions-of-canvas-in-javafx)
     */
    private void encodeImageData(World world) {
        byte encodedByte;
        int index = 0;
        imageData[index++] = 2;
        for (int row = 0; row < ROWS; row++) {
            for (int i = 0; i < imageWidth; i++) {
                imageData[index++] = 2;
            }
            for (int i = 0; i < CELLSIDE_DIMENSION; i++) {
                for (int column = 0; column < COLUMNS; column++) {
                    encodedByte = world.isAlive(row, column) ? (byte) 1 : (byte) 0;
                    for (int j = 0; j < CELLSIDE_DIMENSION; j++) {
                        imageData[index++] = encodedByte;
                    }
                    imageData[index++] = 2;
                }
                imageData[index++] = 2;
            }
        }
        for (int i = 0; i < imageWidth - 1; i++) {
            imageData[index++] = 2;
        }
    }

}
