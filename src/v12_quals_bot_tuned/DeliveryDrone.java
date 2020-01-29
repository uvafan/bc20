package v12_quals_bot_tuned;

import battlecode.common.*;

public class DeliveryDrone extends Unit {

    public static MapLocation targetLoc;
    public static boolean defending = false;
    public static boolean harassing = false;
    public static boolean landscaperDropper = false;
    public static boolean droppedOff = false;
    public static boolean pickedUpFriend = false;
    // public static boolean pickedUpEnemy = false;

    public DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
        if(strat instanceof Rush) {
            rushing = true;
        }
        else {
            harassing = true;
            if (round >= MagicConstants.PICK_UP_LANDSCAPER_ROUND)
                landscaperDropper = true;
        }
        if(enemyHQLoc == null) {
            if (rushing && (hqLoc == null || hqLoc.distanceSquaredTo(center) > here.distanceSquaredTo(hqLoc)))
                targetLoc = center;
            else if(harassing)
                targetLoc = center;
            else
                targetLoc = pickTargetFromEnemyHQs(true);
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        defending = hqAttacked || (defending && round >= MagicConstants.CRUNCH_ROUND);
        if (rushing) {
            doRush();
        }
        else if (rc.getCooldownTurns() < 1) {
            if(!crunching && shouldCrunch())
                crunching = true;
            if(crunching) {
                doCrunch();
            }
            else if (!defending) {
                helpOutFriends();
            }
            if(rc.getCooldownTurns() < 1 && (harassing || defending)) {
                doHarass();
            }
        }
        broadcastNetGuns();
        comms.readMessages();
        if(numWaterLocs <= MagicConstants.MAX_WATER_LOCS && comms.isCaughtUp())
            broadcastWater();
    }

    public MapLocation getDropOffLoc() throws GameActionException {
        int minDist = Integer.MAX_VALUE;
        MapLocation ret = null;
        for(int i = 0; i < MagicConstants.WALL_X_OFFSETS.length; i++) {
            if(Clock.getBytecodesLeft() < 1000)
                break;
            int dx = MagicConstants.WALL_X_OFFSETS[i];
            int dy = MagicConstants.WALL_Y_OFFSETS[i];
            MapLocation check = new MapLocation(hqLoc.x + dx, hqLoc.y + dy);
            if(rc.canSenseLocation(check) && !rc.isLocationOccupied(check) && safeFromDrones(check)
                && rc.senseElevation(check) >= MagicConstants.LATTICE_HEIGHT && rc.senseElevation(check) <= MagicConstants.LATTICE_TOLERANCE) {
                int dist = here.distanceSquaredTo(check);
                if(dist < minDist) {
                    minDist = dist;
                    ret = check;
                }
            }
        }
        return ret;
    }

    private void helpOutFriends() throws GameActionException {
        MapLocation wallLoc = getDropOffLoc();
        if(pickedUpFriend) {
            Utils.log("holding friend!");
            if(wallLoc != null) {
                int dist = here.distanceSquaredTo(wallLoc);
                rc.setIndicatorLine(here, wallLoc, 0,  0, 255);
                if(dist <= 2) {
                    if(tryDrop(here.directionTo(wallLoc), false))
                        pickedUpFriend = false;
                }
                else {
                    goTo(wallLoc);
                }
            }
            else {
                if(tryDrop(here.directionTo(hqLoc), true))
                    pickedUpFriend = false;
            }
        }
        else if (wallLoc != null && !rc.isCurrentlyHoldingUnit()){
            RobotInfo closestFriend = null;
            int minDist = Integer.MAX_VALUE;
            for (RobotInfo ri : friends) {
                if ((ri.type == RobotType.MINER && (isWallComplete || round > MagicConstants.HELP_MINER_UP_ROUND)) || ri.type == RobotType.LANDSCAPER) {
                    if (ri.location.distanceSquaredTo(hqLoc) < 9 && here.distanceSquaredTo(ri.location) < minDist) {
                        minDist = here.distanceSquaredTo(ri.location);
                        closestFriend = ri;
                    }
                }
            }
            if(closestFriend != null) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, closestFriend.location, 0, 255, 0);
                if(minDist <= 2) {
                    if(rc.canPickUpUnit(closestFriend.ID)) {
                        rc.pickUpUnit(closestFriend.ID);
                        pickedUpFriend = true;
                    }
                }
                else {
                    goTo(closestFriend.location);
                }
            }
        }
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
        if(enemyHQLoc == null || enemyHqLocPossibilities.length > 1)
            if(updateSymmetryAndOpponentHQs()) {
                targetLoc = pickTargetFromEnemyHQs(true);
            }
        if(landscaperDropper && !droppedOff) {
            if(!rc.isCurrentlyHoldingUnit()) {
                RobotInfo closestLandscaper = null;
                int minDist = Integer.MAX_VALUE;
                for(RobotInfo f: friends) {
                    if(f.type != RobotType.LANDSCAPER)
                        continue;
                    int dist = here.distanceSquaredTo(f.location);
                    if(dist < minDist) {
                        closestLandscaper = f;
                        minDist = dist;
                    }
                }
                if(minDist <= 2) {
                    if(rc.canPickUpUnit(closestLandscaper.ID))
                        rc.pickUpUnit(closestLandscaper.ID);
                } else if (closestLandscaper != null) {
                    goTo(closestLandscaper.location);
                }
                else {
                    // TODO: go to nearest design school maybe
                    explore();
                }
            }
            else {
                runToEnemyHQ();
            }
        }
        else {
            if (!rc.isCurrentlyHoldingUnit()) {
                findAndPickUpEnemyUnit();
            }
            else if (crunching && rc.senseFlooding(here)) {
                rc.disintegrate();
            }
            else {
                dropUnitInWater();
            }
        }
    }

    private void dropUnitInWater() throws GameActionException {
        for(Direction dir: directions) {
            MapLocation loc = here.add(dir);
            if(rc.canSenseLocation(loc) && rc.senseFlooding(loc)) {
                if(tryDrop(dir, false))
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
            Utils.log("dont see an enemy to pick up");
            if (defending && hqLoc != null) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, hqLoc, 0, 0, 255);
                goTo(hqLoc);
            }
            else if (crunching && here.distanceSquaredTo(enemyHQLoc) <= 16) {
                Utils.log("gonna go defend");
                defending = true;
                crunching = false;
                goTo(hqLoc);
            }
            else if(harassing || crunching) {
                Utils.log("harassing or crunching");
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
        return round >= MagicConstants.CRUNCH_ROUND && !defending;
    }

    public void doCrunch() throws GameActionException {
        if(!landscaperDropper || !rc.isCurrentlyHoldingUnit()) {
            doHarass();
        }
        else {
            if(enemyHQLoc != null && here.distanceSquaredTo(enemyHQLoc) <= 8) {
                for(Direction dir: directions) {
                    MapLocation loc = here.add(dir);
                    if(loc.distanceSquaredTo(enemyHQLoc) <= 2 && rc.canDropUnit(dir)) {
                        droppedOff = true;
                        rc.dropUnit(dir);
                    }
                }
            }
            else if(enemyHQLoc != null) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, enemyHQLoc, 255, 0, 0);
                goTo(enemyHQLoc);
            }
        }
    }

    public void runToEnemyHQ() throws GameActionException {
        if((enemyHQLoc == null || enemyHqLocPossibilities.length > 1) && rushing){
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
            if(Utils.DEBUG)
                rc.setIndicatorLine(here, targetLoc, 255, 0, 0);
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
        else if(crunching || harassing) {
            return true;
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
