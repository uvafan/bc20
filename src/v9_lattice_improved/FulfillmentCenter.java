package v9_lattice_improved;

import battlecode.common.*;

public class FulfillmentCenter extends Building {

    public static MapLocation minerLoc = null;

    public FulfillmentCenter(RobotController r) throws GameActionException {
        super(r);
        RobotInfo[] friends = rc.senseNearbyRobots(2,us);
        for(RobotInfo f: friends) {
            if(f.type == RobotType.MINER) {
                minerLoc = f.location;
                break;
            }
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

    @Override
    public Direction getBuildDirection() {
        if(minerLoc != null)
            return here.directionTo(minerLoc);
        return randomDirection();
    }
}
