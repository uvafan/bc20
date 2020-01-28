package v13_post_quals.v12_quals_bot;

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
