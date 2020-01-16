package v2_better_turtle;

import battlecode.common.*;

public class Building extends Bot {
    public Building(RobotController r) throws GameActionException {
        super(r);
        here = rc.getLocation();
    }

    @Override
    public void takeTurn() throws GameActionException {
        super.takeTurn();
    }

}
