package gameoflife_actors.messages;

import gameoflife_actors.World;

import java.io.Serializable;

public class NextSubMatrixAskMsg implements Serializable {

    private final World world;

    public NextSubMatrixAskMsg(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

}
