package v2;

import battlecode.common.*;

public class Refinery extends Bot {

    public Refinery(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.REFINERY_LOC, rc.getLocation());
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
