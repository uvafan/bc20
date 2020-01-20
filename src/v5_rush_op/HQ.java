package v5_rush_op;

import battlecode.common.*;

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

    public Direction getBuildDirection() {
        if(strat instanceof Rush && round == 1)
            return here.directionTo(center);
        MapLocation[] locs = rc.senseNearbySoup();
        if(locs.length > 0)
            return here.directionTo(locs[0]);
        return randomDirection();
    }

}
