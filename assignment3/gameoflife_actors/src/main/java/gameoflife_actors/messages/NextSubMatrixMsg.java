package gameoflife_actors.messages;

import java.io.Serializable;

public class NextSubMatrixMsg implements Serializable {

    private final int workerIndex;
    private final boolean[][] subMatrix;
    private final long population;

    public NextSubMatrixMsg(int workerIndex, boolean[][] subMatrix, long population) {
        this.workerIndex = workerIndex;
        this.subMatrix = subMatrix;
        this.population = population;
    }

    public int getWorkerIndex() {
        return workerIndex;
    }

    public boolean[][] getSubMatrix() {
        return subMatrix;
    }

    public long getPopulation() {
        return population;
    }

}
