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
        defending = hqAttacked;
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
        if(numWaterLocs <= MagicConstants.MAX_WATER_LOCS)
            broadcastWater();
    }

    private void broadcastWater() throws GameActionException {
        if(Clock.getBytecodesLeft() < 1000)
            return;
        for(Direction dir: directions) {
            if(Clock.getBytecodesLeft() < 1000)
                break;
            MapLocation loc = here;
            for(int i=1; i*i<=rc.getCurrentSensorRadiusSquared(); i++) {
                if(Clock.getBytecodesLeft() < 1000)
                    break;
                loc = loc.add(dir);
                if(!rc.canSenseLocation(loc))
                    break;
                if(rc.senseFlooding(loc)) {
                    boolean shouldAdd = true;
                    for(int j=0; j <numWaterLocs; j++) {
                        if(!invalidWater[j] && waterLocs[j].distanceSquaredTo(loc) <= MagicConstants.TOLERATED_WATER_DIST) {
                            shouldAdd = false;
                            break;
                        }
                    }
                    if(shouldAdd) {
                        comms.broadcastLoc(Comms.MessageType.WATER_LOC, loc);
                        return;
                    }
                }
            }
        }
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
        if(enemyHQLoc == null)
            if(updateOpponentHQs()) {
                pickTargetFromEnemyHQs(true);
            }
        if (!rc.isCurrentlyHoldingUnit()) {
            findAndPickUpEnemyUnit();
        } else {
            dropUnitInWater();
        }
    }

    private void dropUnitInWater() throws GameActionException {
        for(Direction dir: directions) {
            MapLocation loc = here.add(dir);
            if(rc.canSenseLocation(loc) && rc.senseFlooding(loc)) {
                rc.dropUnit(dir);
                return;
            }
        }
        MapLocation waterLoc = null;
        int minDist = Integer.MAX_VALUE;
        for(int i=0; i<numWaterLocs; i++) {
            if(invalidWater[i])
                continue;
            int dist = waterLocs[i].distanceSquaredTo(here);
            if(dist <= MagicConstants.GIVE_UP_WATER_DIST) {
                invalidWater[i] = true;
            }
            else if(dist < minDist){
                minDist = dist;
                waterLoc = waterLocs[i];
            }
        }
        if(waterLoc != null) {
            if(Utils.DEBUG)
                rc.setIndicatorLine(here, waterLoc, 0, 0, 255);
            goTo(waterLoc);
        }
        else {
            explore();
        }
    }

    private void findAndPickUpEnemyUnit() throws GameActionException {
        MapLocation closestEnemyLoc = null;
        int closestEnemyID = -1;
        int minDist = Integer.MAX_VALUE;
        for(RobotInfo e: enemies) {
            if(!Utils.isBuilding(e.type) && e.type != RobotType.DELIVERY_DRONE && here.distanceSquaredTo(e.location) < minDist) {
                minDist = here.distanceSquaredTo(e.location);
                closestEnemyLoc = e.location;
                closestEnemyID = e.getID();
            }
        }
        if(closestEnemyLoc == null) {
            if (defending && hqLoc != null) {
                goTo(hqLoc);
            }
            else if(harassing || crunching) {
                runToEnemyHQ();
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

    public void doCrunch() throws GameActionException {
        if(!pickedUpFriendlyLandscaper) {
            doHarass();
        }
        else {
            //TODO this
        }
    }

    public void runToEnemyHQ() throws GameActionException {
        if(enemyHQLoc == null && rushing){
            if(updateSymmetryAndOpponentHQs())
                targetLoc = pickTargetFromEnemyHQs(true);
        }
        if(enemyHQLoc != null) {
            Utils.log("know enemy hq loc");
            if(Utils.DEBUG)
                rc.setIndicatorLine(here, enemyHQLoc, 255, 0, 0);
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
