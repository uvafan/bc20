package v2;

import battlecode.common.*;

public class Miner extends Bot {

    MapLocation targetMineLoc;
    MapLocation refineLoc;

    public Miner(RobotController r) throws GameActionException {
        super(r);
        refineLoc = null;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            boolean deposited = false;
            for (Direction dir : directions)
                if (tryDeposit(dir)) {
                    Utils.log("I deposited soup! " + rc.getTeamSoup());
                    deposited  = true;
                    refineLoc = null;
                    break;
                }
            if(!deposited) {
                if(refineLoc == null)
                    refineLoc = chooseRefineLoc();
                goTo(refineLoc);
                if (Utils.DEBUG && refineLoc != null) {
                    rc.setIndicatorLine(here, refineLoc, 0, 255, 0);
                }
            }
        }
        else if(buildIfShould()){
        }
        else {
            updateTargetMineLoc();
            if (targetMineLoc != null) {
                if(rc.getLocation().isWithinDistanceSquared(targetMineLoc,2)) {
                    Direction dir = rc.getLocation().directionTo(targetMineLoc);
                    tryMine(dir);
                    Utils.log("I mined soup! " + rc.getSoupCarrying());
                }
                else {
                    goTo(targetMineLoc);
                    if (Utils.DEBUG && targetMineLoc != null) {
                        rc.setIndicatorLine(here, targetMineLoc, 255, 0, 0);
                    }
                }
            }
            else {
                explore();
                Utils.log("exploring");
            }
        }
        comms.readMessages();
    }

    private MapLocation chooseRefineLoc() {
        MapLocation bestLoc = hqLoc;
        int minDist = here.distanceSquaredTo(bestLoc);
        for(int i=0; i<numRefineries; i++){
            int dist = here.distanceSquaredTo(refineries[i]);
            if(dist < minDist){
                bestLoc = refineries[i];
                minDist = dist;
            }
        }
        return bestLoc;
    }

    private boolean buildIfShould() throws GameActionException {
        if(numDesignSchools == 0)
            if(tryBuild(RobotType.DESIGN_SCHOOL, randomDirection())) {
                Utils.log("created a design school");
                return true;
            }
        if (!nearbyRobot(RobotType.REFINERY)){
            if(tryBuild(RobotType.REFINERY, randomDirection())) {
                Utils.log("created a refinery");
                return true;
            }
        }
        return false;
    }

    private void updateTargetMineLoc() throws GameActionException {
        if(targetMineLoc != null) {
            if(rc.canSenseLocation(targetMineLoc) && !rc.isLocationOccupied(targetMineLoc) && rc.senseSoup(targetMineLoc) > 0)
                return;
        }
        targetMineLoc = null;
        MapLocation[] candidates = getLocationsWithinSensorRad();
        for(MapLocation cand: candidates){
            if(cand == null)
                break;
            if(rc.canSenseLocation(cand) && !rc.isLocationOccupied(cand) && rc.senseSoup(cand) > 0){
                targetMineLoc = cand;
                return;
            }
        }
    }

    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }


    static boolean tryDeposit(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

}
