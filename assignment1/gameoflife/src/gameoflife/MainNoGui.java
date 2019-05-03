package gameoflife;

import utils.Chrono;

import java.util.concurrent.Semaphore;

public class MainNoGui {

	public static void main(String[] args) {
		Game game = new Game();
		game.setChrono(new Chrono());
		game.start();  // the Game starts paused, so we need to "resume" it immediately
		game.resumeGame();
	}

}
