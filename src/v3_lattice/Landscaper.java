package v3_lattice;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(hqLoc != null && hqLoc.distanceSquaredTo(here) < 4) {
            if (rc.getDirtCarrying() == 0 ) {//< RobotType.LANDSCAPER.dirtLimit) {
                if (hqLoc != null)
                    tryDig(hqLoc.directionTo(here), true);
                else
                    tryDig(randomDirection(), true);
            }

            if (rc.getCooldownTurns() == 0 && hqLoc != null) {
                // find best place to build
                MapLocation bestPlaceToBuildWall = here;
                int lowestElevation = 9999999;
                for (Direction dir : directions) {
                    MapLocation tileToCheck = hqLoc.add(dir);
                    if (rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                            && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                        int elevation = rc.senseElevation(tileToCheck);
                        if (rc.senseElevation(tileToCheck) < lowestElevation && (round > 1000 ||
                                Utils.getRoundFlooded(elevation) - round < 10)) {
                            lowestElevation = rc.senseElevation(tileToCheck);
                            bestPlaceToBuildWall = tileToCheck;
                        }
                    }
                }
                // build the wall
                Direction dir = here.directionTo(bestPlaceToBuildWall);
                if (rc.canDepositDirt(dir))
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
