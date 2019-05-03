package gameoflife.workers;

public class WorkerParameters {

	private int fromRow;
	private int fromColumn;
	private int ncells;

	public WorkerParameters(int fromRow, int fromColumn, int ncells) {
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.ncells = ncells;
	}

	public int getFromRow() {
		return fromRow;
	}

	public int getFromColumn() {
		return fromColumn;
	}

	public int getNcells() {
		return ncells;
	}

}
