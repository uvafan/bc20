package v8_eco_lattice;

import battlecode.common.*;

public class HQ extends Building {

    public HQ(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.HQ_LOC, rc.getLocation());
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        shootDrones();
        boolean seesNonDroneEnemy = false;
        for(RobotInfo e: enemies) {
            if(e.type != RobotType.DELIVERY_DRONE) {
                seesNonDroneEnemy = true;
                break;
            }
        }
        if(hqAttacked && !seesNonDroneEnemy) {
           hqAttacked = false;
           comms.broadcastLoc(Comms.MessageType.HQ_OK, here);
        }
        else if(!hqAttacked && seesNonDroneEnemy) {
            hqAttacked = true;
            comms.broadcastLoc(Comms.MessageType.HQ_ATTACKED, here);
        }
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
