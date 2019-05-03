package gameoflife.workers;

import gameoflife.World;
import java.util.concurrent.Semaphore;

public class ControllerWorker extends Worker {

	private byte[] imageData;

	public ControllerWorker(String name, World current, int fromRow, int fromColumn, int ncells,
							Semaphore beginWorkSem, Semaphore endWorkSem, byte[] imageData){
		super(name, current, fromRow, fromColumn, ncells, beginWorkSem, endWorkSem);
		this.imageData = imageData;
	}

	@Override
	protected void work() {
		int row, column;
		for (int n = fromN; n < fromN + ncells; n++) {
			row = n / columns;
			column = n % columns;
			imageData[n] = world.isAlive(row, column) ? (byte) 1 : (byte) 0;
		}
	}

}
