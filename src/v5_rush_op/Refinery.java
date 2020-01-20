package v5_rush_op;

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
