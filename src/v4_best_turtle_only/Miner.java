package v4_best_turtle_only;

import battlecode.common.*;

public class Miner extends Unit {

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
            if(targetMineLoc == null || !(targetMineLoc.equals(here) && rc.senseSoup(here) > 0) && turnCount % 10 == 0)
                updateTargetMineLoc();
            if (targetMineLoc != null) {
                if(here.equals(targetMineLoc)) {
                    tryMine(Direction.CENTER);
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
        if(rt != null && tryBuild(rt, randomDirection())) {
            return true;
        }
        if (!nearbyRobot(RobotType.REFINERY) && rc.senseNearbySoup().length > 2){
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
        MapLocation[] candidates = rc.senseNearbySoup();
        int minDist = Integer.MAX_VALUE;
        int xsum = 0;
        int ysum = 0;
        for(MapLocation cand: candidates){
            xsum += cand.x;
            ysum += cand.y;
            int dist = here.distanceSquaredTo(cand);
            if(!rc.isLocationOccupied(cand) && !rc.senseFlooding(cand) && dist < minDist){
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
