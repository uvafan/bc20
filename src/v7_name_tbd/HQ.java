package v7_name_tbd;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
