package gameoflife.workers;

import gameoflife.World;
import java.util.concurrent.Semaphore;
import static gameoflife.config.GameConfig.*;

public abstract class Worker extends Thread {

	/** Returns the parameters (fromRows, fromColumns, ncells) for every worker, given the World and the number of workers.
	 */
	public static WorkerParameters[] inferParameters(World world, int nworkers) {
		WorkerParameters parameters[] = new WorkerParameters[nworkers];
		int totalCells = world.getRows() * world.getColumns();
		int chunkSize = totalCells / nworkers;
		int remainder = totalCells - (chunkSize * nworkers);
		int count = 0;
		for (int i = 0; i < nworkers; i++) {
			int ncells = chunkSize;
			if (i < remainder) {
				ncells++;  // add a cell to ncells if there's still some remainder to assign
			}
			int fromRow = count / world.getColumns();
			int fromColumn = count % world.getColumns();
			parameters[i] = new WorkerParameters(fromRow, fromColumn, ncells);
			count += ncells;
		}
		return parameters;
	}

	protected World world;
	protected int fromRow, fromColumn, ncells;
	protected Semaphore beginWorkSem, endWorkSem;
	protected int columns, fromN;

	public Worker(String name, World world, int fromRow, int fromColumn, int ncells,
				  Semaphore beginWorkSem, Semaphore endWorkSem) {
		super(name);
		this.world = world;
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.ncells = ncells;
		this.beginWorkSem = beginWorkSem;
		this.endWorkSem = endWorkSem;
		columns = world.getColumns();
		fromN = fromRow*columns + fromColumn;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	private void log(String msg){
		System.out.println(LEFT_SEPARATOR+super.getName()+RIGHT_SEPARATOR+" "+msg);
	}

	public void run() {
		log("Starting row = "+fromRow+" | Starting column = "+fromColumn+" | Total cells = "+ncells);
		while (true) {
			try {
				beginWorkSem.acquire();
				work();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				endWorkSem.release();
			}
		}
	}

	protected abstract void work();

}
