package gameoflife_actors.messages;

import gameoflife_actors.World;

import java.io.Serializable;

public class NextWorldMsg implements Serializable {

    private final World world;
    private final long population;
    private final long era;
    private final long computationTime;

    public NextWorldMsg(World world, long population, long era, long computationTime) {
        this.world = world;
        this.population = population;
        this.era = era;
        this.computationTime = computationTime;
    }

    public World getWorld() {
        return world;
    }

    public long getPopulation() {
        return population;
    }

    public long getEra() {
        return era;
    }

    public long getComputationTime() {
        return computationTime;
    }

}
