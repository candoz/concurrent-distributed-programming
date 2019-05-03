package gameoflife_actors;

import gameoflife_actors.messages.*;
import akka.actor.*;

import static gameoflife_actors.Config.*;

public class GameActor extends AbstractActorWithStash {

    private boolean[][][] subMatrices = new boolean[N_WORKERS][][];
    private World world;
    private long population;
    private long era;

    private long beginComputationTime;
    private long computationTime;

    private ActorRef workers[] = new ActorRef[N_WORKERS];
    private int workersCounter;

    @Override
    public void preStart() {
        world = World.random(ROWS, COLUMNS, RANDOM_ALIVE_FACTOR);
        //world = World.glider(ROWS, COLUMNS, 0, 0);
        population = world.getPopulation();
        era = 0;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(FirstWorldAskMsg.class, msg -> {

            // Send the first world to the sender ...
            getSender().tell(new NextWorldMsg(world, population, era, 0L), getSelf());

            // ... and prepare the next world in the meantime.
            workersCounter = 0;
            population = 0;
            setUpWorkers();
            for (ActorRef worker : workers) {
                worker.tell(new NextSubMatrixAskMsg(world), getSelf());
            }
        }).match(NextWorldAskMsg.class, msg -> {
            if (workersCounter == N_WORKERS) {
                era++;
                world = World.fromListOfSubMatrices(subMatrices);
                getSender().tell(new NextWorldMsg(world, population, era, computationTime), getSelf());

                population = 0;
                workersCounter = 0;
                beginComputationTime = System.nanoTime();
                for (ActorRef worker : workers) {
                    worker.tell(new NextSubMatrixAskMsg(world), getSelf());
                }
            } else {
                stash();
            }
        }).match(NextSubMatrixMsg.class, msg -> {
            workersCounter++;
            population += msg.getPopulation();
            subMatrices[msg.getWorkerIndex()] = msg.getSubMatrix();
            if (workersCounter == N_WORKERS) {
                computationTime = (System.nanoTime() - beginComputationTime)/1000;
                unstashAll();
            }
        }).build();
    }

    private void setUpWorkers() {
        int quotient = world.getRows() / N_WORKERS;
        int remainder = world.getRows() % N_WORKERS;
        int fromRow = 0;
        int nRows;
        ActorSystem system = getContext().getSystem();
        for (int i = 0; i < N_WORKERS; i++) {
            workers[i] = system.actorOf(Props.create(GameWorkerActor.class));
            if (remainder > 0) {
                nRows = quotient + 1;
                remainder--;
            } else {
                nRows = quotient;
            }
            workers[i].tell(new GameWorkerConfigurationMsg(i, fromRow, nRows), getSelf());
            fromRow += nRows;
        }
    }

}
