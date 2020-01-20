package v7_seeding;

import battlecode.common.*;

public class Landscaper extends Unit {

    public static MapLocation ourDesignSchool = null;

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        if((strat instanceof Rush) && enemyHQLoc != null) {
            rushing = true;
            RobotInfo[] friends = rc.senseNearbyRobots(2, us);
            for(RobotInfo f: friends) {
                if(f.type == RobotType.DESIGN_SCHOOL) {
                    ourDesignSchool = f.location;
                    break;
                }
            }
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(rushing) {
            if(here.distanceSquaredTo(enemyHQLoc) > 2) {
               if(rc.getCooldownTurns() < 1)
                   goTo(enemyHQLoc);
            }
            else {
                if (rc.getDirtCarrying() == 0) {
                    tryDig(here.directionTo(ourDesignSchool), false);
                    tryDig(enemyHQLoc.directionTo(here), true);
                } else if (rc.getCooldownTurns() < 1) {
                    if (rc.canDepositDirt(here.directionTo(enemyHQLoc))) {
                        rc.depositDirt(here.directionTo(enemyHQLoc));
                    }
                }
            }
        }
        else {
            MapLocation closestEnemyBuilding = null;
            int minDist = Integer.MAX_VALUE;
            for(RobotInfo e: enemies) {
                if(Utils.isBuilding(e.type) && here.distanceSquaredTo(e.location) < minDist) {
                    closestEnemyBuilding = e.location;
                    minDist = here.distanceSquaredTo(e.location);
                }
            }
            if(closestEnemyBuilding != null) {
                if(minDist <= 2) {
                    if(rc.getDirtCarrying() == 0) {
                        if(hqLoc != null) {
                            if (rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0 && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                                tryDig(here.directionTo(hqLoc), false);
                            }
                            if (rc.getCooldownTurns() < 1 && rc.getDirtCarrying() == 0) {//< RobotType.LANDSCAPER.dirtLimit) {
                                tryDig(hqLoc.directionTo(here), true);
                            }
                        }
                        else {
                            tryDig(randomDirection(), true);
                        }
                    }
                    else if(rc.canDepositDirt(here.directionTo(closestEnemyBuilding))) {
                        rc.depositDirt(here.directionTo(closestEnemyBuilding));
                    }
                }
                else if (hqLoc == null || here.distanceSquaredTo(hqLoc) > 2 || rc.senseRobotAtLocation(hqLoc).dirtCarrying == 0) {
                    if(spotIsFreeAround(closestEnemyBuilding))
                        goTo(closestEnemyBuilding);
                }
            }
            if (rc.getCooldownTurns() < 1 && hqLoc != null && hqLoc.distanceSquaredTo(here) < 4) {
                if (rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0 && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                    tryDig(here.directionTo(hqLoc), false);
                }
                if (rc.getCooldownTurns() < 1 && (rc.getDirtCarrying() == 0 || rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && round < 250)) {
                    tryDig(hqLoc.directionTo(here), true);
                }
                if (rc.getCooldownTurns() < 1 && hqLoc != null) {
                    // find best place to build
                    MapLocation bestPlaceToBuildWall = here;
                    int lowestElevation = 9999999;
                    for (Direction dir : directions) {
                        MapLocation tileToCheck = hqLoc.add(dir);
                        if (here.distanceSquaredTo(tileToCheck) < 4
                                && rc.canDepositDirt(here.directionTo(tileToCheck))) {
                            int elevation = rc.senseElevation(tileToCheck);
                            if (rc.senseElevation(tileToCheck) < lowestElevation && (round > 1000 ||
                                    Utils.getRoundFlooded(elevation - 1) < round)) {
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
