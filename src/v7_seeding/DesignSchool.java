package v7_seeding;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class DesignSchool extends Building {
	
    public DesignSchool(RobotController r) throws GameActionException {
        super(r);
        if(enemyHQLoc != null)
            rushing = true;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

    public Direction getBuildDirection() {
        if(enemyHQLoc != null)
            return here.directionTo(enemyHQLoc);
        else if(hqLoc != null)
            return here.directionTo(hqLoc);
        return randomDirection();
    }

}
