package v6_rush_improved;

import battlecode.common.*;

public class Miner extends Unit {

    MapLocation targetMineLoc;
    MapLocation refineLoc;
    RobotInfo[] enemyUnits;
    boolean builtOffensiveNetGun;
    boolean nearbyDrone;
    boolean nearbyFulfillment;
    int fulfillmentDist;
    int builtFulfillmentRound;
    MapLocation fulfillmentLoc;
    MapLocation designSchoolLoc = null;
    MapLocation droneLoc = null;

    public Miner(RobotController r) throws GameActionException {
        super(r);
        if(round == 2 && strat instanceof Rush) {
            rushing = true;
        }
        refineLoc = null;
        nearbyDrone = false;
        nearbyFulfillment = false;
        builtFulfillmentRound = -100;
        targetLoc = center;
        fulfillmentDist = 100;//inf basically;
    }
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(rushing) {
            if (enemyHQLoc != null && here.distanceSquaredTo(enemyHQLoc) <= 8) {
                if (!builtOffensiveNetGun) {
                    enemyUnits = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemy);
                    fulfillmentDist = 100;
                    nearbyFulfillment = false;
                    nearbyDrone = false;
                    for (RobotInfo i : enemyUnits) {
                        if (i.type == RobotType.DELIVERY_DRONE) {
                            nearbyDrone = true;
                            droneLoc = i.location;
                        }
                        if (i.type == RobotType.FULFILLMENT_CENTER) {
                            fulfillmentDist = Math.min(fulfillmentDist, here.distanceSquaredTo(i.location));
                            nearbyFulfillment = true;
                            fulfillmentLoc = i.location;
                        }
                    }
                    if (nearbyDrone || nearbyFulfillment) {
                        builtOffensiveNetGun = tryBuild(RobotType.NET_GUN, here.directionTo(fulfillmentLoc), true);
                    }
                    if (nearbyDrone && rc.getCooldownTurns() < 1 && here.distanceSquaredTo(droneLoc) <= 2 && designSchoolLoc == null) {
                        // TODO improve running away
                        moveInDir(droneLoc.directionTo(here));
                    }
                }
                if(rc.getCooldownTurns() < 1 && designSchoolLoc == null) {
                    designSchoolLoc = tryBuildWithin(RobotType.DESIGN_SCHOOL, enemyHQLoc, 2);
                    if(designSchoolLoc == null)
                        goTo(enemyHQLoc);
                }
                if(rc.getCooldownTurns() < 1 && designSchoolLoc != null) {
                    // TODO improve this
                    targetLoc = enemyHQLoc.add(designSchoolLoc.directionTo(enemyHQLoc));
                    rc.setIndicatorLine(here, targetLoc, 255, 0, 0);
                    goTo(targetLoc);
                }
            }
            else {
                if(enemyHQLoc == null)
                    if(updateSymmetryAndOpponentHQs())
                        targetLoc = pickTargetFromEnemyHQs(false);
                if(enemyHQLoc != null) {
                    targetLoc = enemyHQLoc;
                    rushToTarget();
                }
                else {
                    rushToTarget();
                }
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, targetLoc, 255, 0, 0);
            }
        }
        else {
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                boolean deposited = false;
                for (Direction dir : directions)
                    if (tryDeposit(dir)) {
                        Utils.log("I deposited soup! " + rc.getTeamSoup());
                        deposited = true;
                        refineLoc = null;
                        break;
                    }
                if (!deposited) {
                    if (refineLoc == null)
                        refineLoc = chooseRefineLoc();
                    goTo(refineLoc);
                    if (Utils.DEBUG && refineLoc != null) {
                        rc.setIndicatorLine(here, refineLoc, 0, 255, 0);
                    }
                }
            } else if (buildIfShould()) {
            } else {
                updateTargetMineLoc();
                if (targetMineLoc != null) {
                    if (here.distanceSquaredTo(targetMineLoc) <= 2) {
                        tryMine(here.directionTo(targetMineLoc));
                        Utils.log("I mined soup! " + rc.getSoupCarrying());
                    } else {
                        goTo(targetMineLoc);
                        if (Utils.DEBUG && targetMineLoc != null) {
                            rc.setIndicatorLine(here, targetMineLoc, 255, 0, 0);
                        }
                    }
                } else {
                    explore();
                    Utils.log("exploring");
                }
            }
        }
        comms.readMessages();
    }

    private void rushToTarget() throws GameActionException {
        if(round - builtFulfillmentRound < 25 || rc.getCooldownTurns() >= 1)
            return;
        if(targetLoc.equals(center) || rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost + RobotType.DELIVERY_DRONE.cost) {
            if(targetLoc.equals(center)) {
                if(!Nav.tryMoveDirect(targetLoc)) {
                    targetLoc = pickTargetFromEnemyHQs(false);
                }
                else {
                    return;
                }
            }
            goTo(targetLoc);
        }
        else {
            if(!Nav.tryMoveDirect(targetLoc) && builtFulfillmentRound == -100) {
                if(tryBuild(RobotType.FULFILLMENT_CENTER, here.directionTo(targetLoc), true))
                    builtFulfillmentRound = round;
            }
        }
    }

    private MapLocation tryBuildWithin(RobotType type, MapLocation loc, int dist) throws GameActionException {
        Direction dirL = here.directionTo(loc);
        Direction dirR = dirL.rotateRight();
        for(int i=0;i<4;i++) {
            MapLocation locL = here.add(dirL);
            MapLocation locR = here.add(dirR);
            if(rc.canBuildRobot(type, dirL) && locL.distanceSquaredTo(loc) <= dist) {
                rc.buildRobot(type, dirL);
                return locL;
            }
            if(rc.canBuildRobot(type, dirR) && locR.distanceSquaredTo(loc) <= dist) {
                rc.buildRobot(type, dirR);
                return locR;
            }
            dirL = dirL.rotateLeft();
            dirR = dirR.rotateRight();
        }
        return null;
    }

    private MapLocation chooseRefineLoc() {
        MapLocation bestLoc = hqLoc;
        int minDist = here.distanceSquaredTo(bestLoc);
        for(int i=0; i<unitCounts[RobotType.REFINERY.ordinal()]; i++){
            int dist = here.distanceSquaredTo(refineries[i]);
            if(dist < minDist){
                bestLoc = refineries[i];
                minDist = dist;
            }
        }
        return bestLoc;
    }

    private boolean buildIfShould() throws GameActionException {
        RobotType rt = strat.determineBuildingNeeded();
        if(rt != null && tryBuild(rt, hqLoc.directionTo(here), true)) {
            return true;
        }
        if(!rushing && strat instanceof Rush && round < 250)
            return false;
        if (!nearbyRobot(RobotType.REFINERY) && rc.senseNearbySoup().length > 2 ){
            if(tryBuild(RobotType.REFINERY, hqLoc.directionTo(here), true)) {
                Utils.log("created a refinery");
                return true;
            }
        }
        return false;
    }

    private void updateTargetMineLoc() throws GameActionException {
        if(targetMineLoc != null) {
            if(rc.canSenseLocation(targetMineLoc) && rc.senseSoup(targetMineLoc) > 0)
                return;
        }
        targetMineLoc = null;
        MapLocation[] candidates = rc.senseNearbySoup();
        int minDist = Integer.MAX_VALUE;
        int xsum = 0;
        int ysum = 0;
        for(MapLocation cand: candidates){
            xsum += cand.x;
            ysum += cand.y;
            int dist = here.distanceSquaredTo(cand);
            if(!rc.senseFlooding(cand) && dist < minDist){
                targetMineLoc = cand;
                minDist = dist;
            }
        }
        if(targetMineLoc == null) {
            minDist = Integer.MAX_VALUE;
            for(int i=0; i<numSoupClusters; i++){
                if(invalidCluster[i])
                    continue;
                int dist = soupClusters[i].distanceSquaredTo(here);
                if(dist <= MagicConstants.GIVE_UP_CLUSTER_DIST)
                    invalidCluster[i] = true;
                else if(dist < minDist) {
                    targetMineLoc = soupClusters[i];
                    minDist = dist;
                }
            }
        }
        else {
            int xmean = xsum/candidates.length;
            int ymean = ysum/candidates.length;
            MapLocation clusterLoc = new MapLocation(xmean, ymean);
            boolean shouldAdd = true;
            for(int i=0; i<numSoupClusters; i++){
                if(soupClusters[i].isWithinDistanceSquared(clusterLoc, MagicConstants.MAX_CLUSTER_DIST)){
                    shouldAdd = false;
                    break;
                }
            }
            if(shouldAdd)
                comms.broadcastLoc(Comms.MessageType.SOUP_CLUSTER_LOC, clusterLoc);
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
