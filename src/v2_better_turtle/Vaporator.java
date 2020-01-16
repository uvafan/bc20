package v2_better_turtle;

import battlecode.common.*;

public class Vaporator extends Bot {

    public Vaporator(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
