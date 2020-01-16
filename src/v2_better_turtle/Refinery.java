package v2_better_turtle;

import battlecode.common.*;

public class Refinery extends Building {

    public Refinery(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.REFINERY_LOC, rc.getLocation());
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
