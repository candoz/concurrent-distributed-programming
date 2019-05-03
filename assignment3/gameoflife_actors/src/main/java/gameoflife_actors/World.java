package gameoflife_actors;

import java.io.Serializable;

public class World implements Serializable {

    private final boolean[][] matrix;
    private final int rows;
    private final int columns;

    private World(boolean[][] matrix) {
        this.matrix = matrix;
        rows = matrix.length;
        columns =  (rows > 0) ? matrix[0].length : 0;
    }

    /* Factory Method 1 */
    public static World random(int rows, int columns, double aliveFactor) {
        boolean[][] matrix = new boolean[rows][columns];  // initialized as false
        java.util.Random rand = new java.util.Random();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (rand.nextDouble() < aliveFactor) {
                    matrix[r][c] = true;
                }
            }
        }
        return new World(matrix);
    }

    /* Factory Method 2 */
    /**
     *
     * @param subMatrices an array of sub-matrices. The first one is the one on top, the last one is the one on the bottom.
     * @return
     */
    public static World fromListOfSubMatrices(boolean[][][] subMatrices) {
        int totalRows = 0;
        for (boolean[][] subMatrix: subMatrices) {
            totalRows += subMatrix.length;
        }
        boolean[][] matrix = new boolean[totalRows][];
        int rowsCopied = 0;
        for (boolean[][] subMatrix: subMatrices) {
            System.arraycopy(subMatrix, 0, matrix, rowsCopied, subMatrix.length);
            rowsCopied += subMatrix.length;
        }
        return new World(matrix);
    }

    /* Factory Method 3 */
    public static World glider(int rows, int columns, int beginRow, int beginColumn) {
        boolean[][] matrix = new boolean[rows][columns];  // initialized as false
        matrix[beginRow][beginColumn] = false;
        matrix[beginRow][(beginColumn+1)%columns] = true;
        matrix[beginRow][(beginColumn+2)%columns] = false;
        matrix[(beginRow+1)%rows][beginColumn] = false;
        matrix[(beginRow+1)%rows][(beginColumn+1)%columns] = false;
        matrix[(beginRow+1)%rows][(beginColumn+2)%columns] = true;
        matrix[(beginRow+2)%rows][beginColumn] = true;
        matrix[(beginRow+2)%rows][(beginColumn+1)%columns] = true;
        matrix[(beginRow+2)%rows][(beginColumn+2)%columns] = true;
        return new World(matrix);
    }

    public boolean isAlive(int row, int column) {
        return matrix[row][column];
    }

    public int neighboursAlive(final int row, final int column) {
        int foundAlive = 0;
        final int rowPrev = row == 0 ? getRows() - 1 : row - 1;
        final int rowNext = (row + 1) % getRows();
        final int columnPrev = column == 0 ? getColumns() - 1 : column - 1;
        final int columnNext = (column + 1) % getColumns();
        if (isAlive(rowPrev, columnPrev)) foundAlive++;
        if (isAlive(rowPrev, column)) foundAlive++;
        if (isAlive(rowPrev, columnNext)) foundAlive++;
        if (isAlive(row, columnPrev)) foundAlive++;
        if (isAlive(row, columnNext)) foundAlive++;
        if (isAlive(rowNext, columnPrev)) foundAlive++;
        if (isAlive(rowNext, column)) foundAlive++;
        if (isAlive(rowNext, columnNext)) foundAlive++;
        return foundAlive;
    }

    public boolean willBeAlive(int row, int column) {  // game rules encapsulated here
        int neighbours = neighboursAlive(row, column);
        return (neighbours == 3
                || (neighbours == 2 && isAlive(row, column)));
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
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

}
