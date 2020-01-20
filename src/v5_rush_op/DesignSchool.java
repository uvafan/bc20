package v5_rush_op;

import battlecode.common.*;

public class DesignSchool extends Building {
	
	public static boolean attacking = false;

    public DesignSchool(RobotController r) throws GameActionException {
        super(r);
        if(enemyHQLoc != null)
            attacking = true;
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
