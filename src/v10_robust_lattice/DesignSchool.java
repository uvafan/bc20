package v10_robust_lattice;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class DesignSchool extends Building {
	
    public DesignSchool(RobotController r) throws GameActionException {
        super(r);
        if(enemyHQLoc != null && strat instanceof Rush)
            rushing = true;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

    public Direction getBuildDirection() {
        if(rushing)
            return here.directionTo(enemyHQLoc);
        else if(hqLoc != null)
            return here.directionTo(hqLoc);
        return randomDirection();
    }

}
