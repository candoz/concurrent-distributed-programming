package gameoflife_actors;

import akka.actor.AbstractActor;
import gameoflife_actors.messages.GameWorkerConfigurationMsg;
import gameoflife_actors.messages.NextSubMatrixAskMsg;
import gameoflife_actors.messages.NextSubMatrixMsg;

import static gameoflife_actors.Config.WORKER_NAME;

public class GameWorkerActor extends AbstractActor {

    private String name;
    private int index;
    private int fromRow, nRows;

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(GameWorkerConfigurationMsg.class, msg -> {
            index = msg.getIndex();
            name = WORKER_NAME+index;
            fromRow = msg.getFromRow();
            nRows = msg.getnRows();
            log("Starting row = " + fromRow + " | Ending row = " + (fromRow + nRows - 1));
        }).match(NextSubMatrixAskMsg.class, msg -> {
            World world = msg.getWorld();
            long nextGeneration = 0;
            boolean[][] nextSubMatrix = new boolean[nRows][world.getColumns()];  // initialized to false
            for (int row = fromRow; row < fromRow + nRows; row++) {
                for (int column = 0; column < world.getColumns(); column++) {
                    if (world.willBeAlive(row, column)) {
                        nextSubMatrix[row-fromRow][column] = true;
                        nextGeneration++;
                    }
                }
            }
            getSender().tell(new NextSubMatrixMsg(index, nextSubMatrix, nextGeneration), getSelf());
        }).build();
    }

    protected void log(String msg) {
        System.out.println("[" + name + "]" + " " + msg);
    }

}
