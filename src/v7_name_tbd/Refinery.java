package v7_name_tbd;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Refinery extends Building {

    public Refinery(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
