package v2_better_turtle;

import battlecode.common.*;

public class Landscaper extends Bot {

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(hqLoc != null && hqLoc.distanceSquaredTo(here) < 4) {
            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                if (hqLoc != null)
                    tryDig(hqLoc.directionTo(here), true);
                else
                    tryDig(randomDirection(), true);
            }

            if (rc.getCooldownTurns() == 0 && hqLoc != null) {
                // find best place to build
                MapLocation bestPlaceToBuildWall = null;
                int lowestElevation = 9999999;
                for (Direction dir : directions) {
                    MapLocation tileToCheck = hqLoc.add(dir);
                    if (rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                            && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                        if (rc.senseElevation(tileToCheck) < lowestElevation) {
                            lowestElevation = rc.senseElevation(tileToCheck);
                            bestPlaceToBuildWall = tileToCheck;
                        }
                    }
                }
                if (round < 300)
                    bestPlaceToBuildWall = here;
                // build the wall
                if (bestPlaceToBuildWall != null) {
                    Direction dir = here.directionTo(bestPlaceToBuildWall);
                    if (rc.canDepositDirt(dir))
                        rc.depositDirt(dir);
                    Utils.log("building a wall at location " + bestPlaceToBuildWall);
                }
            }
        }
        // otherwise try to get to the hq
        else if(hqLoc != null){
            goTo(hqLoc);
        }
        comms.readMessages();

    }

    static boolean tryDig(Direction dir, boolean tryOthers) throws GameActionException {
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        if(tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while(dirL != dir) {
                if (rc.canDigDirt(dirL)) {
                    rc.digDirt(dirL);
                    return true;
                }
                if (rc.canDigDirt(dirR)) {
                    rc.digDirt(dirR);
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
    }

}
