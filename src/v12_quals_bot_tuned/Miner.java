package v12_quals_bot_tuned;

import battlecode.common.*;

public class Miner extends Unit {

    MapLocation targetMineLoc;
    MapLocation refineLoc;
    RobotInfo[] enemyUnits;
    boolean builtOffensiveNetGun;
    boolean nearbyDrone;
    boolean runningBackToLattice;
    boolean nearbyFulfillment;
    int fulfillmentDist;
    int builtFulfillmentRound;
    MapLocation fulfillmentLoc;
    MapLocation designSchoolLoc = null;
    MapLocation droneLoc = null;
    public static int[] cornerXOffsets = {-2,-2,2,2};
    public static int[] cornerYOffsets = {-2,2,2,-2};
    boolean buildMiner = false;
    int birthRound;

    public Miner(RobotController r) throws GameActionException {
        super(r);
        runningBackToLattice = false;
        if(round == 2 && strat instanceof Rush) {
            rushing = true;
        }
        birthRound = round;
        refineLoc = null;
        nearbyDrone = false;
        nearbyFulfillment = false;
        builtFulfillmentRound = -100;
        targetLoc = center;
        fulfillmentDist = 100; //inf basically;
        if(strat instanceof EcoLattice) {
            comms.readMessages();
            Utils.log("I think there are " + unitCounts[RobotType.MINER.ordinal()] + " miners.");
            buildMiner = unitCounts[RobotType.MINER.ordinal()] > MagicConstants.NUM_NON_BUILD_MINERS || isWallComplete;
            if(buildMiner) {
                Utils.log("I'm a build miner!");
            }
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(comms.readRound < Math.min(birthRound, round - 1) && !buildMiner) {
            comms.readMessages(Math.min(birthRound, round - 1));
            if(comms.readRound - 1 == birthRound) {
                caughtUp = true;
                if(strat instanceof EcoLattice) {
                    Utils.log("I think there are " + unitCounts[RobotType.MINER.ordinal()] + " miners.");
                    buildMiner = unitCounts[RobotType.MINER.ordinal()] > MagicConstants.NUM_NON_BUILD_MINERS;
                    if(buildMiner) {
                        Utils.log("I'm a build miner!");
                    }
                }
            }
        }
        if(Clock.getBytecodesLeft() < 1000)
            return;
        if(standingOnLattice() || here.distanceSquaredTo(hqLoc) <= 8)
            runningBackToLattice = false;
        if(!rushing && rc.getCooldownTurns() < 1)
            dealWithEnemyDrones();
        if(rushing) {
            doRush();
        }
        else if (rc.getCooldownTurns() < 1){
            if(buildMiner) {
                if(!buildIfShould()) {
                    if(!isWallComplete || here.distanceSquaredTo(hqLoc) < 8) {
                        int idxToGoTo = (round / 7) % 4;
                        MapLocation goalLoc = new MapLocation(hqLoc.x + cornerXOffsets[idxToGoTo], hqLoc.y + cornerYOffsets[idxToGoTo]);
                        Direction bestDir = null;
                        int minDist = Integer.MAX_VALUE;
                        for(Direction dir: directions) {
                            if (rc.canMove(dir) && here.add(dir).distanceSquaredTo(hqLoc) < 8
                            && (!hqAttacked || here.add(dir).distanceSquaredTo(hqLoc) > 2)) {
                                int dist = here.add(dir).distanceSquaredTo(goalLoc);
                                if(dist < minDist) {
                                    minDist = dist;
                                    bestDir = dir;
                                }
                            }
                        }
                        if(bestDir != null)
                            tryMove(bestDir);
                    }
                    else if (standingOnLattice()){
                        if(enemyHQLoc != null)
                            goToOnLattice(enemyHQLoc, false, true);
                        else
                            goToOnLattice(hqLoc, false, true);
                    }
                    else {
                        goTo(hqLoc);
                    }
                }
            }
            else if((runningBackToLattice || floodingSoon(here)) && round > MagicConstants.RUN_BACK_TO_LATTICE_ROUND || reallyFloodingSoon(here)) {
                runningBackToLattice = true;
                rc.setIndicatorLine(here, hqLoc, 0, 0, 255);
                goTo(hqLoc);
            }
            else if(buildIfShould()) {
            }
            else if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                boolean deposited = false;
                for (Direction dir : directions) {
                    MapLocation refining = here.add(dir);
                    if(!rc.canSenseLocation(refining) ||
                        rc.senseRobotAtLocation(refining) == null ||
                        rc.senseRobotAtLocation(refining).team == enemy)
                        continue;
                    if (tryDeposit(dir)) {
                        Utils.log("I deposited soup! " + rc.getTeamSoup());
                        deposited = true;
                        refineLoc = null;
                        break;
                    }
                }
                if (!deposited) {
                    Utils.log("trying to go back to deposit soup");
                    if (refineLoc == null || refineLoc.equals(hqLoc) || turnCount % 20 == 0)
                        refineLoc = chooseRefineLoc();
                    if(refineLoc == null)
                        buildIfShould();
                    else {
                        goTo(refineLoc);
                        if(Utils.DEBUG)
                            rc.setIndicatorLine(here, refineLoc, 0, 255, 0);
                    }
                }
            }
            else {
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

    private void doRush() throws GameActionException{
        if (enemyHQLoc != null && (designSchoolLoc != null || here.distanceSquaredTo(enemyHQLoc) <= 8)) {
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
                int toleranceDist = 2;
                for(Direction dir: cardinalDirs) {
                    MapLocation loc = enemyHQLoc.add(dir);
                    if(rc.canSenseLocation(loc) && here.distanceSquaredTo(loc) <= here.distanceSquaredTo(enemyHQLoc) && !rc.isLocationOccupied(loc)){
                        toleranceDist = 1;
                        break;
                    }
                }
                designSchoolLoc = tryBuildWithin(RobotType.DESIGN_SCHOOL, enemyHQLoc, toleranceDist);
                if(designSchoolLoc == null && here.distanceSquaredTo(enemyHQLoc) > 1)
                    goTo(enemyHQLoc);
            }
            if(rc.getCooldownTurns() < 1 && designSchoolLoc != null) {
                Direction dir = designSchoolLoc.directionTo(enemyHQLoc);
                Direction dirR = dir.rotateRight();
                Direction dirL = dir.rotateLeft();
                MapLocation candLoc = enemyHQLoc.add(dir);
                MapLocation candLocL = enemyHQLoc.add(dirL);
                MapLocation candLocR = enemyHQLoc.add(dirR);
                MapLocation[] cands = {candLoc, candLocL, candLocR};
                targetLoc = cands[0];
                for(int i=0; i<cands.length; i++){
                    MapLocation cand = cands[i];
                    if(cand.equals(here) || !rc.canSenseLocation(cand) || canReach(cand, here, true)) {
                        targetLoc = cand;
                        break;
                    }
                }
                if(Utils.DEBUG)
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

    private MapLocation chooseRefineLoc() throws GameActionException {
        MapLocation bestLoc = null;
        int minDist = Integer.MAX_VALUE;
        for(int i=0; i<unitCounts[RobotType.REFINERY.ordinal()]; i++){
            if(invalidRefineries[i])
                continue;
            //System.out.println("checking " + refineries[i]);
            if(rc.canSenseLocation(refineries[i])) {
                RobotInfo ri = rc.senseRobotAtLocation(refineries[i]);
                if(ri == null || ri.team != us || ri.type != RobotType.REFINERY) {
                    invalidRefineries[i] = true;
                    continue;
                }
            }
            int dist = here.distanceSquaredTo(refineries[i]);
            if(dist < minDist){
                bestLoc = refineries[i];
                minDist = dist;
            }
        }
        if(bestLoc == null && !isWallComplete && (!rc.canSenseLocation(hqLoc) || canReachAdj(hqLoc, true, MagicConstants.MINER_ELEVATION_TOLERANCE)))
            bestLoc = hqLoc;
        return bestLoc;
    }

    private boolean buildRefineryIfShould(Direction buildDirection, boolean tryOthers) throws GameActionException {
        if(buildMiner ||
            buildDirection == null ||
            !rushing && strat instanceof Rush && round < 250)
            return false;
        MapLocation closestRefine = chooseRefineLoc();
        if(closestRefine != null && (rc.getTeamSoup() < MagicConstants.SOUP_REQUIRED_FOR_REFINERY || (hqAttacked && !MagicConstants.BUILD_REFINERY)|| here.distanceSquaredTo(closestRefine) < MagicConstants.REQUIRED_REFINERY_DIST))
            return false;
        updateTargetMineLoc();
        if(targetMineLoc != null && Utils.DEBUG)
            rc.setIndicatorLine(here, targetMineLoc, 255, 0 ,0);
        if(closestRefine != null && Utils.DEBUG)
            rc.setIndicatorLine(here, targetMineLoc, 0, 0,255);
        if (closestRefine == null || targetMineLoc != null && here.distanceSquaredTo(targetMineLoc) <= 25){
            Utils.log("trying to build refinery!");
            if(tryBuild(RobotType.REFINERY, buildDirection, tryOthers)) {
                return true;
            }
        }
        return false;
    }

    private boolean buildIfShould() throws GameActionException {
        Direction[] buildDirections = getBuildDirections();
        boolean tryOthers = !(strat instanceof EcoLattice);
        if(buildDirections[0] != null) {
            RobotType rt = strat.determineBuildingNeeded();
            if(rt != null && tryBuild(rt, buildDirections[0], tryOthers)) {
                return true;
            }
        }
        return buildRefineryIfShould(buildDirections[1], tryOthers);
    }

    private void updateTargetMineLoc() throws GameActionException {
        int elevationTolerance = rc.canSenseLocation(hqLoc) ? MagicConstants.MINER_ELEVATION_TOLERANCE: 3;
        for(Direction d: directions) {
            MapLocation loc = here.add(d);
            if(rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
                targetMineLoc = loc;
                return;
            }
        }
        if(targetMineLoc != null) {
            if(rc.canSenseLocation(targetMineLoc) && rc.senseSoup(targetMineLoc) > 0 && canReachAdj(targetMineLoc, false, elevationTolerance))
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
            if(canReachAdj(cand, false, elevationTolerance) && dist < minDist){
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

    public void explore() throws GameActionException {
        if (standingOnLattice()) {
            exploreOnLattice(false, true);
        }
        else {
            super.explore();
        }
    }

    public boolean goTo(MapLocation location) throws GameActionException {
        if(standingOnLattice()) {
            return goToOnLattice(location, false, true);
        }
        else {
            return super.goTo(location);
        }
    }


}
