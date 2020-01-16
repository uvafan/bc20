package v2;

import battlecode.common.*;

public class Landscaper extends Bot {

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(rc.getDirtCarrying() == 0){
            tryDig();
        }

        MapLocation bestPlaceToBuildWall = null;
        // find best place to build
        if(hqLoc != null) {
            int lowestElevation = 9999999;
            for (Direction dir : directions) {
                MapLocation tileToCheck = hqLoc.add(dir);
                if(rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                        && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                    if (rc.senseElevation(tileToCheck) < lowestElevation) {
                        lowestElevation = rc.senseElevation(tileToCheck);
                        bestPlaceToBuildWall = tileToCheck;
                    }
                }
            }
            if(round < 300)
            	bestPlaceToBuildWall = here;
        }

        if (hqLoc != null && hqLoc.distanceSquaredTo(here) <= 2){
            // build the wall
            if (bestPlaceToBuildWall != null) {
            	Direction dir = here.directionTo(bestPlaceToBuildWall);
                if(rc.canDepositDirt(dir))
                	rc.depositDirt(dir);
                Utils.log("building a wall at location " + bestPlaceToBuildWall);
            }
        }

        // otherwise try to get to the hq
        else if(hqLoc != null){
            goTo(hqLoc);
        }
        comms.readMessages();

    }

    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

}
