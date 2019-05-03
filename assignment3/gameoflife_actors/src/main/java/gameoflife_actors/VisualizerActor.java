package gameoflife_actors;

import gameoflife_actors.messages.*;
import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.time.Duration;

import static gameoflife_actors.Config.FRAMERATE_CAP;

public class VisualizerActor extends AbstractActorWithStash {

    private boolean paused = false;
    private boolean firstWorld = true;
    private long lastFrameTime = System.currentTimeMillis();

    private World currentWorld;
    private long currentPopulation;
    private long currentEra;
    private long currentComputationTime;

    private ActorSystem system;
    private ActorRef gameActor;
    private Visualizer visualizer;

    public VisualizerActor(Visualizer visualizer){
        this.visualizer = visualizer;
    }

    @Override
    public void preStart() {
        system = getContext().getSystem();
        gameActor = system.actorOf(Props.create(GameActor.class));
        gameActor.tell(new FirstWorldAskMsg(), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(PauseMsg.class, msg -> {
            paused = true;
        }).match(ResumeMsg.class, msg -> {
            paused = false;
            unstashAll();
        }).match(NextWorldMsg.class, msg -> {
            if (!paused) {
                if (firstWorld) {
                    firstWorld = false;
                    paused = true;
                }
                this.currentWorld = msg.getWorld();
                this.currentPopulation = msg.getPopulation();
                this.currentEra = msg.getEra();
                this.currentComputationTime = msg.getComputationTime();
                long now = System.currentTimeMillis();
                long toWait = 0;
                if (now - lastFrameTime < 1000 / FRAMERATE_CAP) {
                    toWait = 1000 / FRAMERATE_CAP - (now - lastFrameTime);
                }
                system.scheduler().scheduleOnce(Duration.ofMillis(toWait), getSelf(), new FramerateCheckMsg(), system.dispatcher(), null);
            } else {
                stash();
            }
        }).match(FramerateCheckMsg.class, msg -> {
            visualizer.visualize(currentWorld, currentPopulation, currentEra, currentComputationTime);
        }).match(RepaintFinishedMsg.class, msg -> {
            lastFrameTime = System.currentTimeMillis();
            gameActor.tell(new NextWorldAskMsg(),getSelf());
        }).build();
    }

}
