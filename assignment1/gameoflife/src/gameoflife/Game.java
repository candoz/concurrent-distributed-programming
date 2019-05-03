package gameoflife;

import gameoflife.workers.GameWorker;
import gameoflife.workers.Worker;
import gameoflife.workers.WorkerParameters;
import utils.Chrono;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import static gameoflife.World.InitType.RANDOM;
import static gameoflife.config.GameConfig.*;

public class Game extends Thread {

	private World world;
	private World supportWorld;
	private long population;
	private long era = 0;

	private boolean paused = true;
	private Semaphore gameRunningSem = new Semaphore(0, true);  // strong semaphore

	private GameWorker workers[] = new GameWorker[WORKER_THREADS];
	private Semaphore beginWorkSems[] = new Semaphore[WORKER_THREADS];
	private Semaphore endWorkSems[] = new Semaphore[WORKER_THREADS];

	private Optional<Chrono> chrono = Optional.empty();
	private Optional<Visualizer> visualizer = Optional.empty();

	public Game() {
		super(GAME_THREAD_NAME);
		world = new World(ROWS, COLUMNS, BORDERS);
		supportWorld = new World(ROWS, COLUMNS, BORDERS);
		if (INIT_TYPE == RANDOM) {
			world.initRandomWithFactor(RANDOM_ALIVE_FACTOR);
		} else {
			world.init(INIT_TYPE);
		}
		population = world.getPopulation();
	}

	@Override
	public void run() {
		setUpWorkers();
		for (Worker worker: workers) { worker.start(); }
		while (true) {
			try {
				chrono.ifPresent(c -> managePerformanceRecording());
				for (int i = 0; i < workers.length; i++) {  // computing next world in the meantime...
					beginWorkSems[i].release();
				}
				gameRunningSem.acquire();
				gameRunningSem.release();
				era++;
				population = 0;
				for (int i = 0; i < workers.length; i++) {
					endWorkSems[i].acquire();
					population += workers[i].getNextGeneration();
				}
				swapWorlds();

				visualizer.ifPresent(v -> v.visualize(world, population, era));

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void resumeGame() {  // add "synchronized" to make Game thread safe
		if (paused) {
			gameRunningSem.release();
			paused = false;
		}
	}

	public void pauseGame() {  // add "synchronized" to make Game thread safe
		if (!paused) {
			try {
				gameRunningSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			paused = true;
		}
	}

	public World getWorld() {
		return world;
	}

	public void setVisualizer(Visualizer visualizer) {
		this.visualizer = Optional.of(visualizer);
	}

	public void setChrono(Chrono chrono) {
		this.chrono = Optional.of(chrono);
	}

	private void setUpWorkers() {
		WorkerParameters parameters[] = Worker.inferParameters(world, workers.length);
		for (int i = 0; i < workers.length; i++) {
			beginWorkSems[i] = new Semaphore(0);
			endWorkSems[i] = new Semaphore(0);
			workers[i] = new GameWorker(GAMEWORKER_THREAD_NAME+i, world,
					parameters[i].getFromRow(), parameters[i].getFromColumn(), parameters[i].getNcells(),
					beginWorkSems[i], endWorkSems[i], supportWorld);
		}
	}

	public void swapWorlds() {
		World temp = world;
		world = supportWorld;
		supportWorld = temp;
	}

	private void managePerformanceRecording() {
		if (era == 0) {
			System.out.println(STARTING_PERFORMANCE_MESSAGE);
			chrono.get().start();
		} else if (era % PERFORMANCE_UPDATE_FREQUENCY == 0) {
			System.out.println(LEFT_SEPARATOR+ "era: " + (era-PERFORMANCE_UPDATE_FREQUENCY + 1) +" -> " + era + RIGHT_SEPARATOR +
					" Average time: " + (chrono.get().getTime() / PERFORMANCE_UPDATE_FREQUENCY) + "ms");
			chrono.get().start();
		}
	}

}
