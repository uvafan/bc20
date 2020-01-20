package v7_name_tbd;

import battlecode.common.*;

public class DeliveryDrone extends Unit {

    public static MapLocation targetLoc;

    public DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
        if(strat instanceof Rush && round < 100) {
            rushing = true;
        }
        if(hqLoc == null || hqLoc.distanceSquaredTo(center) > here.distanceSquaredTo(hqLoc))
            targetLoc = center;
        else
            targetLoc = pickTargetFromEnemyHQs(true);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rushing) {
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
                if(enemyHQLoc == null){
                    if(updateSymmetryAndOpponentHQs())
                        targetLoc = pickTargetFromEnemyHQs(false);
                }
                if(enemyHQLoc != null) {
                    Utils.log("know enemy hq loc");
                    targetLoc = enemyHQLoc;
                    if(here.distanceSquaredTo(targetLoc) <= 25){
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
        }
        else {
            if (!rc.isCurrentlyHoldingUnit()) {
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
                if (robots.length > 0) {
                    // Pick up
                    // a first robot within range
                    rc.pickUpUnit(robots[0].getID());
                    System.out.println("I picked up " + robots[0].getID() + "!");
                }
            } else {
                // No close robots, so search for robots within sight radius
                explore();
            }
        }
        comms.readMessages();
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
