package v14_final_bot;

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
        Direction bestDir = null;
        int bestScore = Integer.MIN_VALUE;
        for(Direction d: directions) {
            boolean safe = true;
            MapLocation loc = here.add(d);
            for(RobotInfo e: enemies) {
                if(e.type == RobotType.NET_GUN || e.type == RobotType.HQ) {
                    if(here.isWithinDistanceSquared(e.location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
                        safe = false;
                        break;
                    }
                }
            }
            if(!safe)
                continue;
            int score = hqLoc.distanceSquaredTo(loc);
            if(hqAttacked)
                score = -score;
            if(score > bestScore) {
                bestDir = d;
                bestScore = score;
            }
        }
        return bestDir;
    }

}
