package v2_better_turtle;

import battlecode.common.*;

public class HQ extends Bot {
    public HQ(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.HQ_LOC, rc.getLocation());

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(numMiners < 10) {
            for (Direction dir : directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners++;
                }
        }
        comms.readMessages();
    }

}
