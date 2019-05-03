package gameoflife;

public class World {

	final public static double DEFAULT_RANDOM_ALIVE_FACTOR = 0.5;

	public enum InitType {
		RANDOM, GLIDER, COLUMN, CROSS;
	}

	private boolean[][] matrix;
	private int rows;
	private int columns;
	private boolean withBorders;

	public World(final int rows, final int columns, final boolean withBorders) {
		this.rows = rows;
		this.columns = columns;
		this.withBorders = withBorders;
		this.matrix = new boolean[rows][columns];
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	public boolean isWithBorders() {
		return withBorders;
	}

	public long getPopulation() {  // computed property
		long population = 0;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				if (isAlive(r, c)) {
					population++;
				}
			}
		}
		return population;
	}

	public void spawn(int row, int column) {
		this.matrix[row][column] = true;
	}

	public void kill(int row, int column) {
		this.matrix[row][column] = false;
	}

	public boolean isAlive(int row, int column) {
		return matrix[row][column];
	}

	public int neighboursAlive(final int row, final int column) {
		return (withBorders)? neighboursAliveWithBorders(row, column) : neighboursAliveWithoutBorders(row, column);
	}

	private int neighboursAliveWithBorders(final int row, final int column) {
		int foundAlive = 0;
		if(row == 0 || row == rows-1 || column == 0 || column == columns-1) {  // on borders
			int fromRow = Math.max(0, row-1);
			int fromColumn = Math.max(0, column-1);
			int toRow = Math.min(rows-1, row+1);
			int toColumn = Math.min(columns-1, column+1);
			for (int r = fromRow; r <= toRow; r++) {
				for (int c = fromColumn; c <= toColumn; c++) {
					if (isAlive(r, c)) {
						foundAlive++;
					}
				}
			}
			if (isAlive(row, column)) {
				foundAlive --;  // do not consider myself!
			}
		} else {  // not on borders
			foundAlive = neighboursAliveNotOnBorders(row, column);
		}
		return foundAlive;
	}

	private int neighboursAliveWithoutBorders(final int row, final int column) {
		int foundAlive = 0;
		if(row == 0 || row == rows-1 || column == 0 || column == columns-1) {  // on borders
			int x;  // column
			int y;  // row
			for (int r = -1; r <= 1; r++) {
				if (row + r < 0) {
					y = rows - 1;
				} else if (row + r >= rows) {
					y = 0;
				} else {
					y = row + r;
				}
				for (int c = -1; c <= 1; c++) {
					if (column + c < 0) {
						x = columns - 1;
					} else if (column + c >= columns) {
						x = 0;
					} else {
						x = column + c;
					}
					if (isAlive(y, x) && !(r == 0 && c == 0)) {
						foundAlive++;
					}
				}
			}
		} else {  // not on borders
			foundAlive = neighboursAliveNotOnBorders(row, column);
		}
		return foundAlive;
	}

	private int neighboursAliveNotOnBorders(final int row, final int column) {
		int foundAlive = 0;
		if (isAlive(row-1,column-1)) foundAlive++;
		if (isAlive(row-1, column)) foundAlive++;
		if (isAlive(row-1,column+1)) foundAlive++;
		if (isAlive(row,column-1)) foundAlive++;
		if (isAlive(row,column+1)) foundAlive++;
		if (isAlive(row+1,column-1)) foundAlive++;
		if (isAlive(row+1, column)) foundAlive++;
		if (isAlive(row+1,column+1)) foundAlive++;
		return foundAlive;
	}

	public void initRandomWithFactor(double aliveFactor) {
		java.util.Random rand = new java.util.Random();
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				if (rand.nextDouble() < aliveFactor) {
					spawn(r, c);
				} else {
					kill(r, c);
				}
			}
		}
	}

	public void init(InitType figure) {
		switch (figure) {
			case GLIDER:
				initGlider();
				break;
			case COLUMN:
				initColumn();
				break;
			case CROSS:
				initCross();
				break;
			default:
				initRandomWithFactor(DEFAULT_RANDOM_ALIVE_FACTOR);
				break;
		}
	}

	private void initGlider() {
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				if ((r == 0 && c == 1)
					|| (r == 1 && c == 2)
					|| (r == 2 && c == 0)
					|| (r == 2 && c == 1)
					|| (r == 2 && c == 2)) {
					spawn(r, c);
				} else {
					kill(r, c);
				}
			}
		}
	}
	private void initColumn() {
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				if ((r == 5 && c == 5)
					|| (r == 6 && c == 5)
					|| (r == 7 && c == 5)) {
					spawn(r, c);
				} else {
					kill(r, c);
				}
			}
		}
	}
	private void initCross() {
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				if ((r == 5 && c == 7)
					|| (r == 6 && c == 7)
					|| (r == 6 && c == 6)
					|| (r == 6 && c == 8)
					|| (r == 7 && c == 7)) {
					spawn(r, c);
				} else {
					kill(r, c);
				}
			}
		}
	}
}
