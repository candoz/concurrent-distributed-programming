package gameoflife_actors.messages;

import java.io.Serializable;

public class GameWorkerConfigurationMsg implements Serializable {

    private final int index;
    private final int fromRow, nRows;

    public GameWorkerConfigurationMsg(int index, int fromRow, int nRows) {
        this.index = index;
        this.fromRow = fromRow;
        this.nRows = nRows;
    }

    public int getIndex() {
        return index;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getnRows() {
        return nRows;
    }
}
