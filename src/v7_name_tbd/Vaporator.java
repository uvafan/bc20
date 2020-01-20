package v7_name_tbd;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Vaporator extends Building {

    public Vaporator(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
