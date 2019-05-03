package gameoflife.workers;

import gameoflife.World;

import java.util.concurrent.Semaphore;

public class GameWorker extends Worker {

	private World supportWorld;
	private long nextGeneration;

	public GameWorker(String name, World current, int fromRow, int fromColumn, int ncells,
					  Semaphore beginWorkSem, Semaphore endWorkSem, World supportWorld){
		super(name, current, fromRow, fromColumn, ncells, beginWorkSem, endWorkSem);
		this.supportWorld = supportWorld;
	}

	public long getNextGeneration() { return nextGeneration; }

	@Override
	protected void work() {
		nextGeneration = 0;
		int row, column;
		for (int n = fromN; n < fromN + ncells; n++) {
			row = n / columns;
			column = n % columns;
			applyNewCellState(row, column);
		}
		swapWorlds();
	}

	private void applyNewCellState(int r, int c) {
		if (willBeAlive(r, c)) {
			supportWorld.spawn(r, c);
			nextGeneration++;
		} else {
			supportWorld.kill(r, c);
		}
	}

	private boolean willBeAlive(int r, int c) {  // game rules
		int currentNeighbours = world.neighboursAlive(r, c);
		return (currentNeighbours == 3
				|| (currentNeighbours == 2 && world.isAlive(r, c)));
	}

	public void swapWorlds() {
		World temp = supportWorld;
		supportWorld = world;
		world = temp;
	}

}
