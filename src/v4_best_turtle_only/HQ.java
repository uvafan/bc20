package v4_best_turtle_only;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class HQ extends Building {
    public HQ(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.HQ_LOC, rc.getLocation());

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        shootDrones();
        comms.readMessages();
    }

}
