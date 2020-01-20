package v4_best_turtle_only;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

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
