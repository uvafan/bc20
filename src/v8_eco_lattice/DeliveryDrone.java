package v8_eco_lattice;

import battlecode.common.*;

public class DeliveryDrone extends Unit {

    public static MapLocation targetLoc;
    public static boolean defending = false;
    public static boolean harassing = false;
    public static boolean pickedUpFriendlyLandscaper = false;

    public DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
        if(strat instanceof Rush) {
            rushing = true;
        }
        else {
            harassing = true;
        }
        if(enemyHQLoc == null) {
            if (rushing && (hqLoc == null || hqLoc.distanceSquaredTo(center) > here.distanceSquaredTo(hqLoc)))
                targetLoc = center;
            else
                targetLoc = pickTargetFromEnemyHQs(true);
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rushing) {
            doRush();
        }
        else if (rc.getCooldownTurns() < 1) {
            if(!crunching && shouldCrunch())
                crunching = true;
            if(crunching) {
                doCrunch();
            }
            else {
                // harass
                doHarass();
            }
        }
        broadcastNetGuns();
        comms.readMessages();
        broadcastWater();
    }

    private void doRush() throws GameActionException {
        if(!rc.isCurrentlyHoldingUnit()) {
            int minerID = -1;
            RobotInfo[] friends = rc.senseNearbyRobots(2,us);
            for(RobotInfo f: friends) {
                if(f.type == RobotType.MINER) {
                    minerID = f.getID();
                    break;
                }
            }
            if(minerID != -1 && rc.canPickUpUnit(minerID)) {
                rc.pickUpUnit(minerID);
            }
            else if (minerID == -1){
                rushing = false;
            }
        }
        else {
            runToEnemyHQ();
        }
    }

    private void doHarass() throws GameActionException {
        if (!rc.isCurrentlyHoldingUnit()) {
            findAndPickUpEnemyUnit();
        } else {
            dropUnitInWater();
        }
    }

    private void dropUnitInWater() {
        for(Direction dir: directions) {
            
        }
    }

    private void findAndPickUpEnemyUnit() throws GameActionException {
        MapLocation closestEnemyLoc = null;
        int closestEnemyID = -1;
        int minDist = Integer.MAX_VALUE;
        for(RobotInfo e: enemies) {
            if(here.distanceSquaredTo(e.location) < minDist) {
                minDist = here.distanceSquaredTo(e.location);
                closestEnemyLoc = e.location;
                closestEnemyID = e.getID();
            }
        }
        if(closestEnemy == null) {
            if(harassing || crunching) {
                runToEnemyHQ();
            }
            else if (defending && hqLoc != null) {
                goTo(hqLoc);
            }
            else {
                explore();
            }
        }
        else {
            if(minDist <= 2 && rc.canPickUpUnit(closestEnemyID)) {
                rc.pickUpUnit(closestEnemyID);
            }
            else {
                goTo(closestEnemyLoc);
            }
        }
    }

    public boolean shouldCrunch() {
        return round >= MagicConstants.CRUNCH_ROUND;
    }

    public void doCrunch() {
        if(!pickedUpFriendlyLandscaper) {
            doHarass();
        }
        else {
            //TODO this
        }
    }

    public void runToEnemyHQ() throws GameActionException {
        if(enemyHQLoc == null){
            if(updateSymmetryAndOpponentHQs())
                targetLoc = pickTargetFromEnemyHQs(false);
        }
        if(enemyHQLoc != null) {
            Utils.log("know enemy hq loc");
            targetLoc = enemyHQLoc;
            if(rushing && here.distanceSquaredTo(targetLoc) <= 25){
                Utils.log("trying to drop");
                if(tryDrop(here.directionTo(targetLoc), true))
                    rushing = false;
            }
            if(rc.getCooldownTurns() < 1)
                goTo(targetLoc);
        }
        else {
            if(here.equals(targetLoc)) {
                targetLoc = pickTargetFromEnemyHQs(false);
            }
            goTo(targetLoc);
        }
    }

    public boolean shouldDropUnit(Direction dir) throws GameActionException {
        if (rushing) {
            MapLocation dropLoc = here.add(dir);
            if (rc.senseFlooding(dropLoc))
                return false;
            int elev = rc.senseElevation(dropLoc);
            for(Direction d: directions) {
                MapLocation loc = targetLoc.add(d);
                if(rc.canSenseLocation(loc) && Math.abs(rc.senseElevation(loc) - elev) <= 3) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean tryDrop(Direction dir, boolean tryOthers) throws GameActionException {
        if (rc.canDropUnit(dir) && shouldDropUnit(dir)) {
            rc.dropUnit(dir);
            return true;
        }
        if(tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while(dirL != dir) {
                if (rc.canDropUnit(dirL) && shouldDropUnit(dir)) {
                    rc.dropUnit(dirL);
                    return true;
                }
                if (rc.canDropUnit(dirR) && shouldDropUnit(dir)) {
                    rc.dropUnit(dirR);
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
    }
}
